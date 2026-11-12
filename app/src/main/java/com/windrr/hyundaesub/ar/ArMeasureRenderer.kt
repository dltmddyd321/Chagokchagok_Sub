package com.windrr.hyundaesub.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.measure.FloorRect
import com.windrr.hyundaesub.measure.MeasureGeometry
import com.windrr.hyundaesub.measure.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

/** 측정 단계 */
enum class MeasureStep {
    /** 바닥 모서리 A */
    POINT_A,

    /** 바닥 모서리 B (A→B = 가로) */
    POINT_B,

    /** 바닥 모서리 C (B→C = 세로) */
    POINT_C,

    /** 높이 */
    HEIGHT,

    /** 가로·세로·높이 모두 확정 */
    DONE,
}

/** 화면에 그릴 모서리. measured = 치수를 재는 세 모서리(가로·세로·높이) */
data class ScreenEdge(val from: Offset, val to: Offset, val measured: Boolean)

/** 모서리 치수 라벨. outward = 상자 바깥쪽 방향(단위 벡터) — 라벨을 선에서 살짝 띄울 때 쓴다 */
data class DimLabel(val at: Offset, val outward: Offset, val text: String)

/** 화면에 보여줄 측정 상태 (GL 스레드에서 매 프레임 생성) */
data class MeasureUiState(
    val tracking: Boolean = false,
    val floorFound: Boolean = false,
    val step: MeasureStep = MeasureStep.POINT_A,
    /** 화면 가운데 조준점이 지금 찍을 수 있는 곳(바닥 / 높이)을 가리키는지 */
    val aimValid: Boolean = false,
    /** 조준점이 가리키는 지점의 화면 좌표 (미리보기 점) */
    val aimPoint: Offset? = null,
    /** 확정된 점 (바닥 꼭짓점, 높이 끝) */
    val points: List<Offset> = emptyList(),
    /** 직육면체 모서리 (확정 + 실시간 미리보기) */
    val edges: List<ScreenEdge> = emptyList(),
    val labels: List<DimLabel> = emptyList(),
    /** 현재 치수 (확정값, 없으면 실시간 미리보기 값) */
    val size: BoxSize = BoxSize(null, null, null),
) {
    val canCapture: Boolean get() = tracking && (step == MeasureStep.DONE || aimValid)
}

/**
 * 점 찍기 + 실시간 직육면체 측정.
 * A·B·C = 물건 바깥쪽 바닥 모서리 (A→B 가로, B→C 세로, 직각으로 보정),
 * 높이 = A 위 수직선과 시선이 가장 가까운 지점.
 * 매 프레임 화면 가운데(조준점)로 다음 점을 미리 계산해 상자와 치수를 실시간으로 보여준다.
 * 화면을 탭하면 그 위치에, 셔터([captureAtAim])를 누르면 조준점 위치에 점을 찍는다.
 */
class ArMeasureRenderer(
    private val session: Session,
    private val displayRotation: () -> Int,
    private val onUpdate: (MeasureUiState) -> Unit,
) : GLSurfaceView.Renderer {

    private val background = CameraBackgroundRenderer()
    private val taps = ConcurrentLinkedQueue<Offset>()
    private val anchors = mutableListOf<Anchor>()
    private var heightM: Float? = null
    private var viewWidth = 1
    private var viewHeight = 1

    @Volatile
    private var resetRequested = false

    @Volatile
    private var undoRequested = false

    /** 화면 탭 위치에 점 찍기 */
    fun onTap(x: Float, y: Float) {
        taps.add(Offset(x, y))
    }

    /** 셔터: 화면 가운데 조준점 위치에 점 찍기 */
    fun captureAtAim() {
        taps.add(Offset.Unspecified)
    }

    /** 마지막 단계 되돌리기 (높이 → C → B → A 순) */
    fun undo() {
        undoRequested = true
    }

    fun reset() {
        resetRequested = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        background.createOnGlThread()
        session.setCameraTextureName(background.textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewWidth = width
        viewHeight = height
        session.setDisplayGeometry(displayRotation(), width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (resetRequested) {
            anchors.forEach { it.detach() }
            anchors.clear()
            heightM = null
            taps.clear()
            resetRequested = false
        }
        if (undoRequested) {
            when {
                heightM != null -> heightM = null
                anchors.isNotEmpty() -> anchors.removeAt(anchors.lastIndex).detach()
            }
            undoRequested = false
        }

        val frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            return
        }
        background.draw(frame)

        val camera = frame.camera
        val tracking = camera.trackingState == TrackingState.TRACKING
        val floorFound = session.getAllTrackables(Plane::class.java).any {
            it.trackingState == TrackingState.TRACKING &&
                it.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                it.subsumedBy == null
        }

        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.05f, 100f) }
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val viewProj = FloatArray(16).also { Matrix.multiplyMM(it, 0, projection, 0, view, 0) }
        val invViewProj = FloatArray(16).also { Matrix.invertM(it, 0, viewProj, 0) }
        val center = Offset(viewWidth / 2f, viewHeight / 2f)

        while (true) {
            val tap = taps.poll() ?: break
            if (tracking) handleTap(frame, if (tap == Offset.Unspecified) center else tap, invViewProj)
        }

        // 조준점 미리보기: 바닥 단계는 바닥 위 지점, 높이 단계는 높이 값
        val step = currentStep()
        val aimFloor = if (tracking && step <= MeasureStep.POINT_C) hitFloor(frame, center)?.toVec3() else null
        val aimHeight = if (tracking && step == MeasureStep.HEIGHT) heightAt(center, invViewProj) else null

        onUpdate(buildState(tracking, floorFound, step, aimFloor, aimHeight, viewProj))
    }

    private fun currentStep(): MeasureStep = when {
        anchors.size == 0 -> MeasureStep.POINT_A
        anchors.size == 1 -> MeasureStep.POINT_B
        anchors.size == 2 -> MeasureStep.POINT_C
        heightM == null -> MeasureStep.HEIGHT
        else -> MeasureStep.DONE
    }

    private fun handleTap(frame: Frame, tap: Offset, invViewProj: FloatArray) {
        when (currentStep()) {
            MeasureStep.POINT_A, MeasureStep.POINT_B, MeasureStep.POINT_C -> {
                val hit = frame.hitTest(tap.x, tap.y).firstOrNull { it.isOnFloor() } ?: return
                anchors += hit.createAnchor()
            }
            MeasureStep.HEIGHT -> heightM = heightAt(tap, invViewProj)
            MeasureStep.DONE -> Unit
        }
    }

    /** 위를 향한 수평면(바닥)에 닿은 지점 */
    private fun hitFloor(frame: Frame, at: Offset): Pose? =
        frame.hitTest(at.x, at.y).firstOrNull { it.isOnFloor() }?.hitPose

    private fun com.google.ar.core.HitResult.isOnFloor(): Boolean {
        val plane = trackable as? Plane ?: return false
        return plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING && plane.isPoseInPolygon(hitPose)
    }

    /** 화면 위치의 시선이 A 위 수직선과 만나는 높이 (m) */
    private fun heightAt(at: Offset, invViewProj: FloatArray): Float? {
        val base = anchors.firstOrNull()?.pose?.toVec3() ?: return null
        val ray = MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj)
        return MeasureGeometry.heightAlongVertical(base, ray)?.takeIf { it >= MIN_HEIGHT_M }
    }

    private fun buildState(
        tracking: Boolean,
        floorFound: Boolean,
        step: MeasureStep,
        aimFloor: Vec3?,
        aimHeight: Float?,
        viewProj: FloatArray,
    ): MeasureUiState {
        val project = { p: Vec3 ->
            MeasureGeometry.projectToScreen(p, viewProj, viewWidth, viewHeight)?.let { (x, y) -> Offset(x, y) }
        }
        val placed = anchors.map { it.pose.toVec3() }
        val aimValid = when (step) {
            MeasureStep.POINT_A, MeasureStep.POINT_B, MeasureStep.POINT_C -> aimFloor != null
            MeasureStep.HEIGHT -> aimHeight != null
            MeasureStep.DONE -> false
        }

        val edges = mutableListOf<ScreenEdge>()
        val labels = mutableListOf<DimLabel>()
        val points = mutableListOf<Offset>()
        var size = BoxSize(null, null, null)

        fun edge(a: Vec3, b: Vec3, measured: Boolean): Pair<Offset, Offset>? {
            val from = project(a) ?: return null
            val to = project(b) ?: return null
            edges += ScreenEdge(from, to, measured)
            return from to to
        }

        fun label(line: Pair<Offset, Offset>?, valueM: Float, boxCenter: Offset?) {
            line ?: return
            val mid = (line.first + line.second) / 2f
            labels += DimLabel(mid, outwardOf(line, mid, boxCenter), "${(valueM * 100f).roundToInt()} cm")
        }

        when {
            // A만: A → 조준점 가로 미리보기
            placed.size == 1 -> {
                project(placed[0])?.let { points += it }
                if (aimFloor != null) {
                    val width = MeasureGeometry.horizontalDistance(placed[0], aimFloor)
                    label(edge(placed[0], aimFloor, measured = true), width, null)
                    size = BoxSize(width * 100f, null, null)
                }
            }
            // A·B: 조준점으로 세로까지 바닥 직사각형 미리보기
            placed.size == 2 -> {
                placed.mapNotNullTo(points) { project(it) }
                if (aimFloor != null) {
                    val rect = MeasureGeometry.rectifyFloor(placed[0], placed[1], aimFloor)
                    addBox(rect, null, ::edge, ::label, project, size = { size = it })
                } else {
                    val width = MeasureGeometry.horizontalDistance(placed[0], placed[1])
                    label(edge(placed[0], placed[1], measured = true), width, null)
                    size = BoxSize(width * 100f, null, null)
                }
            }
            // 바닥 확정: 높이(확정 또는 조준점 미리보기)로 직육면체
            placed.size >= 3 -> {
                val rect = MeasureGeometry.rectifyFloor(placed[0], placed[1], placed[2])
                rect.corners.take(3).mapNotNullTo(points) { project(it) }
                val height = heightM ?: aimHeight
                val topPoint = addBox(rect, height, ::edge, ::label, project, size = { size = it })
                if (heightM != null) topPoint?.let { points += it }
            }
        }

        return MeasureUiState(
            tracking = tracking,
            floorFound = floorFound,
            step = step,
            aimValid = aimValid,
            aimPoint = aimFloor?.let(project),
            points = points,
            edges = edges,
            labels = labels,
            size = size,
        )
    }

    /**
     * 바닥 직사각형(+높이)으로 상자 모서리와 라벨을 만든다.
     * 가로 = A→B, 세로 = B→C, 높이 = 화면에서 가장 오른쪽 기둥 (시안처럼 오른쪽에 높이 표시).
     * 높이 라벨을 붙인 기둥의 윗점 화면 좌표를 돌려준다.
     */
    private fun addBox(
        rect: FloorRect,
        height: Float?,
        edge: (Vec3, Vec3, Boolean) -> Pair<Offset, Offset>?,
        label: (Pair<Offset, Offset>?, Float, Offset?) -> Unit,
        project: (Vec3) -> Offset?,
        size: (BoxSize) -> Unit,
    ): Offset? {
        val base = rect.corners
        val top = height?.let { rect.top(it) }
        val center = (base + top.orEmpty()).mapNotNull(project).takeIf { it.isNotEmpty() }?.let { pts ->
            pts.reduce { acc, o -> acc + o } / pts.size.toFloat()
        }

        // 측정하지 않는 모서리 먼저 (측정 모서리가 위에 그려지도록)
        edge(rect.c, rect.d, false)
        edge(rect.d, rect.a, false)
        var heightColumn = -1
        if (top != null) {
            heightColumn = base.indices.maxByOrNull { project(base[it])?.x ?: Float.NEGATIVE_INFINITY } ?: 0
            for (i in 0 until 4) edge(top[i], top[(i + 1) % 4], false)
            base.indices.filter { it != heightColumn }.forEach { edge(base[it], top[it], false) }
        }

        label(edge(rect.a, rect.b, true), rect.width, center)
        label(edge(rect.b, rect.c, true), rect.depth, center)
        var topPoint: Offset? = null
        if (top != null && height != null) {
            label(edge(base[heightColumn], top[heightColumn], true), height, center)
            topPoint = project(top[heightColumn])
        }
        size(BoxSize(rect.width * 100f, rect.depth * 100f, height?.times(100f)))
        return topPoint
    }

    /** 라벨을 선에서 띄울 방향: 선에 수직이면서 상자 중심에서 멀어지는 쪽 */
    private fun outwardOf(line: Pair<Offset, Offset>, mid: Offset, boxCenter: Offset?): Offset {
        val dir = line.second - line.first
        val len = dir.getDistance()
        if (len < 1f) return Offset(0f, -1f)
        var normal = Offset(-dir.y / len, dir.x / len)
        val away = boxCenter?.let { mid - it }
        if (away != null && away.x * normal.x + away.y * normal.y < 0f) normal = -normal
        if (away == null && normal.y > 0f) normal = -normal // 기준 없으면 위쪽
        return normal
    }

    private fun Pose.toVec3() = Vec3(tx(), ty(), tz())

    private companion object {
        /** 이보다 낮은 높이는 측정 실패로 본다 (m) */
        const val MIN_HEIGHT_M = 0.01f
    }
}
