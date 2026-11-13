package com.windrr.hyundaesub.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.measure.FloorCircle
import com.windrr.hyundaesub.measure.FloorRect
import com.windrr.hyundaesub.measure.MeasureGeometry
import com.windrr.hyundaesub.measure.MeasureShape
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
    val step: MeasureStep = MeasureStep.POINT_A,
    val tapMissed: Boolean = false,
    /** 원통: 세 점이 한 줄에 가까워 원을 만들 수 없음 */
    val badCircle: Boolean = false,
    /** 확정된 점 (바닥 꼭짓점, 높이 끝) */
    val points: List<Offset> = emptyList(),
    /** 직육면체 모서리 (확정 + 실시간 미리보기) */
    val edges: List<ScreenEdge> = emptyList(),
    val labels: List<DimLabel> = emptyList(),
    /** 현재 치수 (확정값, 없으면 실시간 미리보기 값) */
    val size: BoxSize = BoxSize(null, null, null),
)

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
    /** BOX = 바닥 꼭짓점 3개, CYLINDER = 바닥 둘레 3점 */
    private val shape: MeasureShape = MeasureShape.BOX,
    private val onUpdate: (MeasureUiState) -> Unit,
) : GLSurfaceView.Renderer {

    private val background = CameraBackgroundRenderer()
    private val taps = ConcurrentLinkedQueue<Offset>()
    private val anchors = mutableListOf<Anchor>()
    private var heightAnchor: Anchor? = null
    private var viewWidth = 1
    private var viewHeight = 1
    private var lastUiUpdateMs = 0L
    private var tapMissed = false
    private var badCircle = false

    @Volatile
    private var resetRequested = false

    @Volatile
    private var undoRequested = false

    /** 화면 탭 위치에 점 찍기 */
    fun onTap(x: Float, y: Float) {
        taps.add(Offset(x, y))
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
            heightAnchor?.detach()
            heightAnchor = null
            taps.clear()
            tapMissed = false
            badCircle = false
            resetRequested = false
        }
        if (undoRequested) {
            when {
                heightAnchor != null -> {
                    heightAnchor?.detach()
                    heightAnchor = null
                }
                anchors.isNotEmpty() -> anchors.removeAt(anchors.lastIndex).detach()
            }
            tapMissed = false
            badCircle = false
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
        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.05f, 100f) }
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val viewProj = FloatArray(16).also { Matrix.multiplyMM(it, 0, projection, 0, view, 0) }
        val invViewProj = FloatArray(16).also { Matrix.invertM(it, 0, viewProj, 0) }
        var stepChanged = false
        while (true) {
            val tap = taps.poll() ?: break
            if (tracking) {
                handleTap(frame, tap, invViewProj)
                stepChanged = true
            }
        }

        val now = SystemClock.elapsedRealtime()
        if (!stepChanged && now - lastUiUpdateMs < UI_UPDATE_INTERVAL_MS) return
        lastUiUpdateMs = now

        onUpdate(buildState(tracking, currentStep(), viewProj))
    }

    private fun currentStep(): MeasureStep = when {
        anchors.size == 0 -> MeasureStep.POINT_A
        anchors.size == 1 -> MeasureStep.POINT_B
        anchors.size == 2 -> MeasureStep.POINT_C
        heightAnchor == null -> MeasureStep.HEIGHT
        else -> MeasureStep.DONE
    }

    private fun handleTap(frame: Frame, tap: Offset, invViewProj: FloatArray) {
        when (currentStep()) {
            MeasureStep.POINT_A, MeasureStep.POINT_B, MeasureStep.POINT_C -> {
                val point = surfacePointOnTapRay(frame, tap, invViewProj)
                tapMissed = point == null
                badCircle = false
                if (point != null && shape == MeasureShape.CYLINDER && anchors.size == 2) {
                    // 세 번째 둘레 점: 원이 안 만들어지거나 비현실적으로 크면 받지 않는다
                    val (a, b) = anchors.map { it.pose.toVec3() }
                    val circle = MeasureGeometry.circleThrough(a, b, point)
                    if (circle == null || circle.radius > MAX_RADIUS_M) {
                        badCircle = true
                        return
                    }
                }
                point?.let { anchors += session.createAnchor(Pose.makeTranslation(it.x, it.y, it.z)) }
            }
            MeasureStep.HEIGHT -> {
                val top = heightTopAt(frame, tap, invViewProj)
                tapMissed = top == null
                heightAnchor = top?.let { session.createAnchor(Pose.makeTranslation(it.x, it.y, it.z)) }
            }
            MeasureStep.DONE -> Unit
        }
    }

    /** 터치한 화면 위치의 시선 위에서, 가장 가까운 표면(평면·깊이·특징점)까지의 거리에 있는 지점 */
    private fun surfacePointOnTapRay(frame: Frame, at: Offset, invViewProj: FloatArray): Vec3? {
        val hit = frame.hitTest(at.x, at.y).firstOrNull { it.isOnSurface() } ?: return null
        val eye = frame.camera.pose.toVec3()
        val direction = MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj).direction
        return eye + direction * hit.distance
    }

    private fun HitResult.isOnSurface(): Boolean = when (val target = trackable) {
        is Plane -> target.isPoseInPolygon(hitPose)
        is DepthPoint -> true
        is Point -> true
        else -> false
    }

    /**
     * 높이 끝점. 터치한 곳이 바닥 직사각형 위쪽 표면(물건 윗면)이면 그 지점을 쓰고,
     * 아니면 바닥 네 꼭짓점의 수직선 중 시선과 가장 가까운 수직선 위 지점을 쓴다.
     */
    private fun heightTopAt(frame: Frame, at: Offset, invViewProj: FloatArray): Vec3? {
        if (shape == MeasureShape.CYLINDER) return cylinderTopAt(frame, at, invViewProj)
        val rect = floorRect() ?: return null
        val top = surfacePointOnTapRay(frame, at, invViewProj)
            ?.takeIf { rect.containsHorizontally(it, TOP_SURFACE_MARGIN_M) }
            ?: MeasureGeometry.topOnNearestVertical(
                rect.corners,
                MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj),
            )
        return top?.takeIf { it.y - rect.a.y >= MIN_HEIGHT_M }
    }

    /**
     * 원통 높이 끝점. 터치한 곳이 원 위쪽 표면(윗면)이면 그 지점,
     * 아니면 둘레 수직선들 중 시선과 가장 가까운 수직선 위 지점(옆면 윗 가장자리).
     */
    private fun cylinderTopAt(frame: Frame, at: Offset, invViewProj: FloatArray): Vec3? {
        val circle = floorCircle() ?: return null
        val top = surfacePointOnTapRay(frame, at, invViewProj)
            ?.takeIf { circle.containsHorizontally(it, TOP_SURFACE_MARGIN_M) }
            ?: MeasureGeometry.topOnNearestVertical(
                circle.rim(RIM_SAMPLES),
                MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj),
            )
        return top?.takeIf { it.y - circle.center.y >= MIN_HEIGHT_M }
    }

    private fun floorCircle(): FloorCircle? {
        if (anchors.size < 3) return null
        val (a, b, c) = anchors.take(3).map { it.pose.toVec3() }
        return MeasureGeometry.circleThrough(a, b, c)
    }

    private fun floorRect(): FloorRect? {
        if (anchors.size < 3) return null
        val (a, b, c) = anchors.take(3).map { it.pose.toVec3() }
        return MeasureGeometry.rectifyFloor(a, b, c)
    }

    private fun buildState(
        tracking: Boolean,
        step: MeasureStep,
        viewProj: FloatArray,
    ): MeasureUiState {
        val project = { p: Vec3 ->
            MeasureGeometry.projectToScreen(p, viewProj, viewWidth, viewHeight)?.let { (x, y) -> Offset(x, y) }
        }
        val placed = anchors.map { it.pose.toVec3() }

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
            shape == MeasureShape.CYLINDER && placed.size >= 3 -> {
                val circle = MeasureGeometry.circleThrough(placed[0], placed[1], placed[2])
                if (circle != null) {
                    val top = heightAnchor?.pose?.toVec3()
                    val height = top?.let { it.y - circle.center.y }
                    addCylinder(circle, placed[0], height, ::edge, ::label, project, size = { size = it })
                }
                placed.take(3).mapNotNullTo(points) { project(it) }
                heightAnchor?.pose?.toVec3()?.let(project)?.let { points += it }
            }
            shape == MeasureShape.CYLINDER && placed.size == 2 -> {
                // 둘레 두 점: 지름이 아니라 현(chord)이라 치수는 아직 표시하지 않는다
                placed.mapNotNullTo(points) { project(it) }
                edge(placed[0], placed[1], measured = false)
            }
            placed.size == 1 -> {
                project(placed[0])?.let { points += it }
            }
            placed.size == 2 -> {
                placed.mapNotNullTo(points) { project(it) }
                val width = MeasureGeometry.horizontalDistance(placed[0], placed[1])
                label(edge(placed[0], placed[1], measured = true), width, null)
                size = BoxSize(width * 100f, null, null)
            }
            placed.size >= 3 -> {
                val rect = MeasureGeometry.rectifyFloor(placed[0], placed[1], placed[2])
                val top = heightAnchor?.pose?.toVec3()
                val height = top?.let { it.y - rect.a.y }
                addBox(rect, height, ::edge, ::label, project, size = { size = it })
                placed.take(3).mapNotNullTo(points) { project(it) }
                top?.let(project)?.let { points += it }
            }
        }

        return MeasureUiState(
            tracking = tracking,
            step = step,
            tapMissed = tapMissed,
            badCircle = badCircle,
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

    /**
     * 원통: 바닥 원·윗면 원(둘레를 선분으로 근사) + 화면 좌우 끝 옆선 + 지름선.
     * 지름 라벨은 첫 번째 둘레 점 A를 지나는 지름, 높이 라벨은 화면에서 가장 오른쪽 옆선.
     * 실을 때는 지름 × 지름 × 높이 상자로 본다.
     */
    private fun addCylinder(
        circle: FloorCircle,
        firstRimPoint: Vec3,
        height: Float?,
        edge: (Vec3, Vec3, Boolean) -> Pair<Offset, Offset>?,
        label: (Pair<Offset, Offset>?, Float, Offset?) -> Unit,
        project: (Vec3) -> Offset?,
        size: (BoxSize) -> Unit,
    ) {
        val start = kotlin.math.atan2(firstRimPoint.z - circle.center.z, firstRimPoint.x - circle.center.x)
        val base = circle.rim(RIM_SEGMENTS, start)
        val top = height?.let { circle.rim(RIM_SEGMENTS, start, it) }
        val center = (base + top.orEmpty()).mapNotNull(project).takeIf { it.isNotEmpty() }?.let { pts ->
            pts.reduce { acc, o -> acc + o } / pts.size.toFloat()
        }

        for (i in base.indices) edge(base[i], base[(i + 1) % base.size], false)
        if (top != null) {
            for (i in top.indices) edge(top[i], top[(i + 1) % top.size], false)
        }

        // 지름: A에서 중심을 지나 반대편까지
        val opposite = base[RIM_SEGMENTS / 2]
        label(edge(base[0], opposite, true), circle.diameter, center)

        if (top != null && height != null) {
            // 원통 윤곽처럼 보이도록 화면 좌우 끝에 옆선
            val projected = base.indices.mapNotNull { i -> project(base[i])?.let { i to it.x } }
            val left = projected.minByOrNull { it.second }?.first
            val right = projected.maxByOrNull { it.second }?.first
            left?.let { edge(base[it], top[it], false) }
            right?.let { label(edge(base[it], top[it], true), height, center) }
        }
        size(BoxSize(circle.diameter * 100f, circle.diameter * 100f, height?.times(100f)))
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

        const val UI_UPDATE_INTERVAL_MS = 33L

        const val TOP_SURFACE_MARGIN_M = 0.05f

        /** 원통 둘레를 그릴 때 선분 수 (짝수 — 지름 반대편 점이 정확히 잡히도록) */
        const val RIM_SEGMENTS = 36

        /** 원통 높이 수직선 후보 수 */
        const val RIM_SAMPLES = 24

        /** 원통 반지름 상한 (m). 이보다 크면 잘못 찍은 것으로 본다 */
        const val MAX_RADIUS_M = 1.0f
    }
}
