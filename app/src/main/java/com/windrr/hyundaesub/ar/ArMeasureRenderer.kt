package com.windrr.hyundaesub.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
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

/** 측정 단계 (바닥 점 3개 → 높이 → 완료) */
enum class MeasureStep {
    POINT_A,
    POINT_B,
    POINT_C,
    HEIGHT,
    DONE,
}

/** 화면에 그릴 모서리. measured = 치수를 재는 모서리(가로·세로·높이 / 지름·높이) */
data class ScreenEdge(val from: Offset, val to: Offset, val measured: Boolean)

/** 모서리 치수 라벨. outward = 상자 바깥쪽 방향(단위 벡터) — 라벨을 선에서 살짝 띄울 때 쓴다 */
data class DimLabel(val at: Offset, val outward: Offset, val text: String)

/** 화면에 보여줄 측정 상태 (GL 스레드에서 만들어 최대 30fps로 전달) */
data class MeasureUiState(
    /** 추적 상태. 잠깐 끊기는 것은 무시하고 일정 시간 이상 끊겼을 때만 false */
    val tracking: Boolean = false,
    /** 바닥을 한 번이라도 인식했는지 (한 번 찾으면 계속 true) */
    val floorFound: Boolean = false,
    val step: MeasureStep = MeasureStep.POINT_A,
    val tapMissed: Boolean = false,
    /** 원통: 세 점이 한 줄에 가까워 원을 만들 수 없음 */
    val badCircle: Boolean = false,
    /** 찍은 점 (끌어서 옮길 수 있는 손잡이): 바닥 점들 + 높이 끝 */
    val points: List<Offset> = emptyList(),
    /** 지금 끌고 있는 점의 index (points 기준), 없으면 null */
    val draggingIndex: Int? = null,
    /** 직육면체·원통 모서리 */
    val edges: List<ScreenEdge> = emptyList(),
    val labels: List<DimLabel> = emptyList(),
    val size: BoxSize = BoxSize(null, null, null),
)

/**
 * 꼭짓점 직접 터치 측정 (네모 / 원통).
 *
 * - 바닥 점은 표면 인식(특징점·깊이) 대신 **인식한 바닥 평면과 터치 시선의 교점**으로 잡는다.
 *   물건 바닥 꼭짓점은 항상 바닥 위에 있어서, 물건 옆면이나 노이즈 많은 특징점에 찍히지 않고
 *   터치한 곳 바로 그 지점이 정확히 잡힌다. 바닥 높이는 첫 점에서 정하고 이후 점에 같이 쓴다.
 * - 네모: 바닥 꼭짓점 세 개를 **아무 순서로** 찍으면 직각에 가장 가까운 점을 모서리로 보고 상자를 만든다.
 * - 원통: 바닥 둘레 아무 곳 세 점 → 원.
 * - 높이: 물건 윗면(인식된 평면)을 찍으면 그 높이, 아니면 바닥 꼭짓점(둘레) 수직선 중 시선과 가장 가까운 곳.
 * - 찍은 점은 언제든 **끌어서 옮길 수 있다** (바닥 점은 바닥 위에서, 높이 점은 높이만).
 */
class ArMeasureRenderer(
    private val session: Session,
    private val displayRotation: () -> Int,
    /** BOX = 바닥 꼭짓점 3개, CYLINDER = 바닥 둘레 3점 */
    private val shape: MeasureShape = MeasureShape.BOX,
    /** 점을 잡았다고 볼 화면 거리 (px) */
    private val grabRadiusPx: Float = 120f,
    private val onUpdate: (MeasureUiState) -> Unit,
) : GLSurfaceView.Renderer {

    private sealed interface TouchEvent {
        data class Tap(val at: Offset) : TouchEvent
        data class DragStart(val at: Offset) : TouchEvent
        data class DragMove(val at: Offset) : TouchEvent
        data object DragEnd : TouchEvent
    }

    private val background = CameraBackgroundRenderer()
    private val events = ConcurrentLinkedQueue<TouchEvent>()
    private val anchors = mutableListOf<Anchor>()
    private var heightAnchor: Anchor? = null
    /** 첫 바닥 점에서 정한 바닥 높이 (월드 y) */
    private var floorY: Float? = null
    private var viewWidth = 1
    private var viewHeight = 1
    private var lastUiUpdateMs = 0L
    private var tapMissed = false
    private var badCircle = false
    private var floorFoundLatched = false
    private var lostSinceMs: Long? = null
    /** 직전 프레임에 그린 손잡이 화면 좌표 (끌기 시작할 점 찾기용) */
    private var lastHandles: List<Offset?> = emptyList()
    /** 끌고 있는 손잡이: 0..2 바닥 점, 3 높이 점 */
    private var dragIndex: Int? = null

    @Volatile
    private var resetRequested = false

    @Volatile
    private var undoRequested = false

    fun onTap(x: Float, y: Float) {
        events.add(TouchEvent.Tap(Offset(x, y)))
    }

    fun onDragStart(x: Float, y: Float) {
        events.add(TouchEvent.DragStart(Offset(x, y)))
    }

    fun onDrag(x: Float, y: Float) {
        events.add(TouchEvent.DragMove(Offset(x, y)))
    }

    fun onDragEnd() {
        events.add(TouchEvent.DragEnd)
    }

    /** 마지막 단계 되돌리기 (높이 → 바닥 점 역순) */
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
            floorY = null
            events.clear()
            dragIndex = null
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
            if (anchors.isEmpty()) floorY = null
            dragIndex = null
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
        val now = SystemClock.elapsedRealtime()
        val trackingNow = camera.trackingState == TrackingState.TRACKING
        // 추적이 잠깐 끊겼다 붙는 것은 안내 문구를 바꾸지 않는다
        lostSinceMs = if (trackingNow) null else lostSinceMs ?: now
        val trackingStable = lostSinceMs?.let { now - it < TRACKING_LOST_GRACE_MS } ?: true
        if (!floorFoundLatched) floorFoundLatched = trackedFloorY() != null

        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.05f, 100f) }
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val viewProj = FloatArray(16).also { Matrix.multiplyMM(it, 0, projection, 0, view, 0) }
        val invViewProj = FloatArray(16).also { Matrix.invertM(it, 0, viewProj, 0) }

        var changed = false
        while (true) {
            val event = events.poll() ?: break
            if (!trackingNow && event !is TouchEvent.DragEnd) continue
            when (event) {
                is TouchEvent.Tap -> handleTap(frame, event.at, invViewProj)
                is TouchEvent.DragStart -> dragIndex = nearestHandle(event.at)
                is TouchEvent.DragMove -> dragIndex?.let { moveHandle(frame, it, event.at, invViewProj) }
                TouchEvent.DragEnd -> dragIndex = null
            }
            changed = true
        }

        if (!changed && now - lastUiUpdateMs < UI_UPDATE_INTERVAL_MS) return
        lastUiUpdateMs = now
        onUpdate(buildState(trackingStable, currentStep(), viewProj))
    }

    private fun currentStep(): MeasureStep = when {
        anchors.size == 0 -> MeasureStep.POINT_A
        anchors.size == 1 -> MeasureStep.POINT_B
        anchors.size == 2 -> MeasureStep.POINT_C
        heightAnchor == null -> MeasureStep.HEIGHT
        else -> MeasureStep.DONE
    }

    private fun handleTap(frame: Frame, tap: Offset, invViewProj: FloatArray) {
        // 이미 찍은 점 근처를 탭하면 새 점을 찍지 않는다 (끌어서 옮기려다 탭으로 끝난 경우)
        if (nearestHandle(tap) != null) return
        when (currentStep()) {
            MeasureStep.POINT_A, MeasureStep.POINT_B, MeasureStep.POINT_C -> {
                val point = floorPointAt(frame, tap, invViewProj)
                tapMissed = point == null
                badCircle = false
                point ?: return
                if (shape == MeasureShape.CYLINDER && anchors.size == 2) {
                    val (a, b) = anchors.map { it.pose.toVec3() }
                    if (!validCircle(a, b, point)) {
                        badCircle = true
                        return
                    }
                }
                anchors += anchorAt(point)
            }
            MeasureStep.HEIGHT -> {
                val top = heightTopAt(frame, tap, invViewProj)
                tapMissed = top == null
                heightAnchor = top?.let(::anchorAt)
            }
            MeasureStep.DONE -> Unit
        }
    }

    /** 끌기: 바닥 점은 바닥 위에서, 높이 점은 높이만 바뀐다. 인식 못 한 위치면 그대로 둔다 */
    private fun moveHandle(frame: Frame, index: Int, at: Offset, invViewProj: FloatArray) {
        if (index < anchors.size) {
            val point = floorPointAt(frame, at, invViewProj) ?: return
            if (shape == MeasureShape.CYLINDER && anchors.size == 3) {
                val others = anchors.indices.filter { it != index }.map { anchors[it].pose.toVec3() }
                if (!validCircle(others[0], others[1], point)) return
            }
            anchors[index].detach()
            anchors[index] = anchorAt(point)
            badCircle = false
        } else if (index == HEIGHT_HANDLE) {
            val top = heightTopAt(frame, at, invViewProj) ?: return
            heightAnchor?.detach()
            heightAnchor = anchorAt(top)
        }
        tapMissed = false
    }

    private fun nearestHandle(at: Offset): Int? =
        lastHandles.withIndex()
            .mapNotNull { (i, p) -> p?.let { i to (it - at).getDistance() } }
            .filter { it.second <= grabRadiusPx }
            .minByOrNull { it.second }
            ?.first

    private fun validCircle(a: Vec3, b: Vec3, c: Vec3): Boolean {
        val circle = MeasureGeometry.circleThrough(a, b, c)
        return circle != null && circle.radius <= MAX_RADIUS_M
    }

    private fun anchorAt(p: Vec3): Anchor = session.createAnchor(Pose.makeTranslation(p.x, p.y, p.z))

    /**
     * 터치한 곳의 바닥 지점 = 터치 시선과 바닥 평면의 교점.
     * 바닥 높이: 이미 찍은 점이 있으면 그 높이, 없으면 터치 위치의 수평면 → 인식한 바닥 중 가장 낮은 면.
     */
    private fun floorPointAt(frame: Frame, at: Offset, invViewProj: FloatArray): Vec3? {
        val y = floorY ?: (horizontalPlaneYAt(frame, at) ?: trackedFloorY())?.also { floorY = it } ?: return null
        val ray = MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj)
        return MeasureGeometry.intersectHorizontalPlane(ray, y)
    }

    /** 터치 위치에 닿은 위를 향한 수평면의 높이 (폴리곤 밖이어도 평면 높이는 쓸 수 있다) */
    private fun horizontalPlaneYAt(frame: Frame, at: Offset): Float? =
        frame.hitTest(at.x, at.y).firstOrNull {
            (it.trackable as? Plane)?.type == Plane.Type.HORIZONTAL_UPWARD_FACING
        }?.hitPose?.ty()

    /** 인식한 위를 향한 수평면 중 가장 낮은 것 = 바닥 */
    private fun trackedFloorY(): Float? =
        session.getAllTrackables(Plane::class.java)
            .filter {
                it.trackingState == TrackingState.TRACKING &&
                    it.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                    it.subsumedBy == null
            }
            .minOfOrNull { it.centerPose.ty() }

    /**
     * 높이 끝점. 물건 윗면이 평면으로 인식돼 있고 그 위를 찍었으면 그 높이,
     * 아니면 바닥 꼭짓점(원통은 둘레) 수직선 중 시선과 가장 가까운 수직선 위 지점.
     * 노이즈가 큰 특징점·깊이 값은 높이에 쓰지 않는다.
     */
    private fun heightTopAt(frame: Frame, at: Offset, invViewProj: FloatArray): Vec3? {
        val baseY = floorY ?: return null
        val ray = MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj)
        val verticals: List<Vec3>
        val insideTop: (Vec3) -> Boolean
        if (shape == MeasureShape.CYLINDER) {
            val circle = floorCircle() ?: return null
            verticals = circle.rim(RIM_SAMPLES)
            insideTop = { circle.containsHorizontally(it, TOP_SURFACE_MARGIN_M) }
        } else {
            val rect = floorRect() ?: return null
            verticals = rect.corners
            insideTop = { rect.containsHorizontally(it, TOP_SURFACE_MARGIN_M) }
        }
        val onTopPlane = frame.hitTest(at.x, at.y).firstOrNull {
            val plane = it.trackable as? Plane
            plane != null &&
                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                it.hitPose.ty() - baseY >= MIN_HEIGHT_M
        }?.hitPose?.toVec3()?.takeIf(insideTop)
        val top = onTopPlane ?: MeasureGeometry.topOnNearestVertical(verticals, ray)
        return top?.takeIf { it.y - baseY >= MIN_HEIGHT_M }
    }

    private fun floorCircle(): FloorCircle? {
        if (anchors.size < 3) return null
        val (a, b, c) = anchors.take(3).map { it.pose.toVec3() }
        return MeasureGeometry.circleThrough(a, b, c)
    }

    private fun floorRect(): FloorRect? {
        if (anchors.size < 3) return null
        val (a, b, c) = anchors.take(3).map { it.pose.toVec3() }
        return MeasureGeometry.rectifyFloorAnyOrder(a, b, c)
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
        val top = heightAnchor?.pose?.toVec3()

        val edges = mutableListOf<ScreenEdge>()
        val labels = mutableListOf<DimLabel>()
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
            placed.size >= 3 && shape == MeasureShape.CYLINDER -> {
                MeasureGeometry.circleThrough(placed[0], placed[1], placed[2])?.let { circle ->
                    val height = top?.let { it.y - circle.center.y }
                    addCylinder(circle, placed[0], height, ::edge, ::label, project, size = { size = it })
                }
            }
            placed.size >= 3 -> {
                val rect = MeasureGeometry.rectifyFloorAnyOrder(placed[0], placed[1], placed[2])
                val height = top?.let { it.y - rect.a.y }
                addBox(rect, height, ::edge, ::label, project, size = { size = it })
            }
            // 두 점: 순서가 자유라 가로인지 대각선인지 아직 모르므로 선만 잇는다
            placed.size == 2 -> edge(placed[0], placed[1], measured = false)
        }

        // 손잡이: 바닥 점 0..2 + 높이 점(index 3)
        val handles = placed.map(project) + List(HEIGHT_HANDLE - placed.size) { null } + listOf(top?.let(project))
        lastHandles = handles
        val points = handles.filterNotNull()
        val draggingPoint = dragIndex?.let { handles.getOrNull(it) }

        return MeasureUiState(
            tracking = tracking,
            floorFound = floorFoundLatched,
            step = step,
            tapMissed = tapMissed,
            badCircle = badCircle,
            points = points,
            draggingIndex = draggingPoint?.let { points.indexOf(it) }?.takeIf { it >= 0 },
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
        /** 높이 손잡이 index */
        const val HEIGHT_HANDLE = 3

        /** 이보다 낮은 높이는 측정 실패로 본다 (m) */
        const val MIN_HEIGHT_M = 0.01f

        const val UI_UPDATE_INTERVAL_MS = 33L

        /** 추적이 이 시간 넘게 끊겨야 "폰을 천천히 움직여 주세요"를 띄운다 */
        const val TRACKING_LOST_GRACE_MS = 800L

        const val TOP_SURFACE_MARGIN_M = 0.05f

        /** 원통 둘레를 그릴 때 선분 수 (짝수 — 지름 반대편 점이 정확히 잡히도록) */
        const val RIM_SEGMENTS = 36

        /** 원통 높이 수직선 후보 수 */
        const val RIM_SAMPLES = 24

        /** 원통 반지름 상한 (m). 이보다 크면 잘못 찍은 것으로 본다 */
        const val MAX_RADIUS_M = 1.0f
    }
}
