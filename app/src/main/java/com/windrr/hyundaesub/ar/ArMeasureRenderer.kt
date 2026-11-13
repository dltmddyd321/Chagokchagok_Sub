package com.windrr.hyundaesub.ar

import android.graphics.Bitmap
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
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.windrr.hyundaesub.measure.MeasureGeometry
import com.windrr.hyundaesub.measure.MeasureShape
import com.windrr.hyundaesub.measure.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 추적이 안 될 때 이유 (안내 문구용) */
enum class TrackingIssue { INITIALIZING, LOW_LIGHT, FEW_FEATURES, FAST_MOTION, CAMERA }

/** 확정된 측정 선 하나 (시작점 → 끝점). trashAt = 선 끝 삭제 버튼 위치 */
data class ScreenSegment(val from: Offset, val to: Offset, val text: String, val trashAt: Offset)

/** 시작점 → 조준점 실시간 노란 선 */
data class PreviewLine(val from: Offset, val to: Offset, val text: String)

/** 화면에 보여줄 측정 상태 (GL 스레드에서 만들어 최대 30fps로 전달) */
data class MeasureUiState(
    /** 추적 상태. 잠깐 끊기는 것은 무시하고 일정 시간 이상 끊겼을 때만 false */
    val tracking: Boolean = false,
    val trackingIssue: TrackingIssue? = TrackingIssue.INITIALIZING,
    /** 표면을 한 번이라도 인식했는지 (한 번 찾으면 계속 true) */
    val surfaceFound: Boolean = false,
    /** 조준 링: 조준점이 닿은 표면 방향으로 누운 원을 화면에 투영한 점들 (표면이 없으면 빈 목록) */
    val aimRing: List<Offset> = emptyList(),
    /** 조준점이 기존 점에 달라붙었는지 */
    val aimSnapped: Boolean = false,
    /** 카메라에서 조준점까지 거리 (cm) — "대상까지 88cm" */
    val aimDistanceCm: Int? = null,
    /** 확정된 선들 */
    val segments: List<ScreenSegment> = emptyList(),
    /** 시작점만 찍고 끝점을 기다리는 중 */
    val waitingEnd: Boolean = false,
    /** 찍은 점들 (화면 좌표) */
    val points: List<Offset> = emptyList(),
    val previewLine: PreviewLine? = null,
    /** 확정된 선 길이 (cm, 찍은 순서) */
    val lengthsCm: List<Float> = emptyList(),
)

/**
 * 삼성 간편 측정과 같은 선 측정.
 * - 화면 가운데 조준 링을 카메라로 움직여 점에 맞추고 + → 시작점, 다시 + → 끝점 (선 하나 완성)
 * - 시작점을 찍으면 시작점 → 조준점 노란 선과 3D 길이를 실시간으로 보여준다
 * - 조준점이 이미 찍은 점 근처면 그 점에 달라붙는다 (꼭짓점에서 이어 재기)
 * - 되돌리기는 마지막 점부터, 선 끝의 휴지통은 그 선을 지운다
 * 점은 조준점이 닿은 실제 표면 깊이에 고정한다. 표면을 못 찾으면 찍지 않는다.
 */
class ArMeasureRenderer(
    private val session: Session,
    private val displayRotation: () -> Int,
    /** 바닥 모양 (선 측정에서는 안내 문구에만 쓰인다) */
    @Suppress("unused") private val shape: MeasureShape = MeasureShape.BOX,
    /** 조준점이 기존 점에 달라붙는 화면 거리 (px) */
    private val snapRadiusPx: Float = 90f,
    private val onUpdate: (MeasureUiState) -> Unit,
) : GLSurfaceView.Renderer {

    private sealed interface Command {
        data object Capture : Command
        data object Undo : Command
        data class Delete(val segmentIndex: Int) : Command
        data object Reset : Command
        data class Photo(val onCaptured: (Bitmap) -> Unit) : Command
    }

    private class Segment(val start: Anchor, val end: Anchor)

    private val background = CameraBackgroundRenderer()
    private val commands = ConcurrentLinkedQueue<Command>()
    private val segments = mutableListOf<Segment>()
    private var pendingStart: Anchor? = null
    private var viewWidth = 1
    private var viewHeight = 1
    private var lastUiUpdateMs = 0L
    private var surfaceFoundLatched = false
    private var lostSinceMs: Long? = null

    /** + 버튼: 조준점 위치에 시작점 또는 끝점 */
    fun captureAtAim() {
        commands.add(Command.Capture)
    }

    /** 마지막 점 되돌리기 */
    fun undo() {
        commands.add(Command.Undo)
    }

    /** 선 하나 지우기 (휴지통) */
    fun deleteSegment(index: Int) {
        commands.add(Command.Delete(index))
    }

    fun reset() {
        commands.add(Command.Reset)
    }

    /** ○ 버튼: 지금 카메라 화면(측정 선·버튼 제외)을 사진으로. 콜백은 GL 스레드에서 불린다 */
    fun capturePhoto(onCaptured: (Bitmap) -> Unit) {
        commands.add(Command.Photo(onCaptured))
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
        val frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            return
        }
        background.draw(frame)

        val camera = frame.camera
        val now = SystemClock.elapsedRealtime()
        val trackingNow = camera.trackingState == TrackingState.TRACKING
        // 추적이 잠깐 끊겼다 붙는 것은 안내를 바꾸지 않는다
        lostSinceMs = if (trackingNow) null else lostSinceMs ?: now
        val trackingStable = lostSinceMs?.let { now - it < TRACKING_LOST_GRACE_MS } ?: true

        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, 0.05f, 100f) }
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val viewProj = FloatArray(16).also { Matrix.multiplyMM(it, 0, projection, 0, view, 0) }
        val invViewProj = FloatArray(16).also { Matrix.invertM(it, 0, viewProj, 0) }
        val project = { p: Vec3 ->
            MeasureGeometry.projectToScreen(p, viewProj, viewWidth, viewHeight)?.let { (x, y) -> Offset(x, y) }
        }

        // 조준: 가운데 시선이 닿은 표면, 기존 점 근처면 그 점에 달라붙기
        val center = Offset(viewWidth / 2f, viewHeight / 2f)
        val surface = if (trackingNow) aimSurfaceAt(frame, center, invViewProj) else null
        if (surface != null) surfaceFoundLatched = true
        val snapTarget = if (trackingNow) snapPointNear(center, project) else null
        val aimPoint = snapTarget ?: surface?.first

        var changed = false
        while (true) {
            val command = commands.poll() ?: break
            when (command) {
                Command.Capture -> if (trackingNow) capture(aimPoint)
                Command.Undo -> undoLast()
                is Command.Delete -> segments.getOrNull(command.segmentIndex)?.let {
                    it.start.detach()
                    it.end.detach()
                    segments.removeAt(command.segmentIndex)
                }
                is Command.Photo -> command.onCaptured(readCameraPixels())
                Command.Reset -> {
                    segments.forEach {
                        it.start.detach()
                        it.end.detach()
                    }
                    segments.clear()
                    pendingStart?.detach()
                    pendingStart = null
                }
            }
            changed = true
        }

        if (!changed && now - lastUiUpdateMs < UI_UPDATE_INTERVAL_MS) return
        lastUiUpdateMs = now

        val ring = when {
            snapTarget != null -> ringAround(snapTarget, Vec3.UP, project)
            surface != null -> ringAround(surface.first, surface.second, project)
            else -> emptyList()
        }
        val issue = if (trackingStable) null else trackingIssueOf(camera.trackingFailureReason)
        onUpdate(
            buildState(trackingStable, issue, project, aimPoint, ring, snapTarget != null, camera.pose.toVec3()),
        )
    }

    /** + : 시작점이 없으면 시작점, 있으면 끝점으로 선 완성. 조준점이 없으면(표면 미인식) 무시 */
    private fun capture(aim: Vec3?) {
        aim ?: return
        val start = pendingStart
        if (start == null) {
            pendingStart = anchorAt(aim)
        } else {
            if ((aim - start.pose.toVec3()).length() < MIN_SEGMENT_M) return
            segments += Segment(start, anchorAt(aim))
            pendingStart = null
        }
    }

    /** 되돌리기: 끝점을 기다리는 시작점 → 지움, 아니면 마지막 선의 끝점을 지워 다시 끝점 대기 */
    private fun undoLast() {
        val start = pendingStart
        if (start != null) {
            start.detach()
            pendingStart = null
            return
        }
        val last = segments.removeLastOrNull() ?: return
        last.end.detach()
        pendingStart = last.start
    }

    /** 이미 찍은 점 중 조준점(화면 가운데)에서 가까운 점 */
    private fun snapPointNear(center: Offset, project: (Vec3) -> Offset?): Vec3? =
        allPoints()
            .mapNotNull { p -> project(p)?.let { p to (it - center).getDistance() } }
            .filter { it.second <= snapRadiusPx }
            .minByOrNull { it.second }
            ?.first

    private fun allPoints(): List<Vec3> =
        segments.flatMap { listOf(it.start.pose.toVec3(), it.end.pose.toVec3()) } +
            listOfNotNull(pendingStart?.pose?.toVec3())

    /** 조준점이 닿은 실제 표면 지점(시선 위)과 표면 방향(법선) */
    private fun aimSurfaceAt(frame: Frame, at: Offset, invViewProj: FloatArray): Pair<Vec3, Vec3>? {
        val hit = frame.hitTest(at.x, at.y).firstOrNull { it.isOnSurface() } ?: return null
        val eye = frame.camera.pose.toVec3()
        val direction = MeasureGeometry.rayFromScreen(at.x, at.y, viewWidth, viewHeight, invViewProj).direction.normalized()
        val axis = FloatArray(3).also { hit.hitPose.getTransformedAxis(1, 1f, it, 0) }
        var normal = Vec3(axis[0], axis[1], axis[2]).normalized()
        if (normal dot direction > 0f) normal = normal * -1f // 카메라 쪽을 보게
        return (eye + direction * hit.distance) to normal
    }

    private fun HitResult.isOnSurface(): Boolean = when (val target = trackable) {
        is Plane -> target.isPoseInPolygon(hitPose)
        is DepthPoint -> true
        is Point -> true
        else -> false
    }

    /** 표면 방향으로 누운 원 (화면 투영) */
    private fun ringAround(point: Vec3, normal: Vec3, project: (Vec3) -> Offset?): List<Offset> {
        val ref = if (kotlin.math.abs(normal.y) < 0.9f) Vec3.UP else Vec3(1f, 0f, 0f)
        val u = cross(normal, ref).normalized()
        val v = cross(normal, u).normalized()
        val ring = List(RING_SEGMENTS) { i ->
            val t = (2.0 * Math.PI * i / RING_SEGMENTS).toFloat()
            point + (u * cos(t) + v * sin(t)) * RING_RADIUS_M
        }.mapNotNull(project)
        return if (ring.size == RING_SEGMENTS) ring else emptyList()
    }

    private fun cross(a: Vec3, b: Vec3) = Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    private fun anchorAt(p: Vec3): Anchor = session.createAnchor(Pose.makeTranslation(p.x, p.y, p.z))

    private fun trackingIssueOf(reason: TrackingFailureReason): TrackingIssue = when (reason) {
        TrackingFailureReason.INSUFFICIENT_LIGHT -> TrackingIssue.LOW_LIGHT
        TrackingFailureReason.INSUFFICIENT_FEATURES -> TrackingIssue.FEW_FEATURES
        TrackingFailureReason.EXCESSIVE_MOTION -> TrackingIssue.FAST_MOTION
        TrackingFailureReason.CAMERA_UNAVAILABLE -> TrackingIssue.CAMERA
        else -> TrackingIssue.INITIALIZING
    }

    private fun buildState(
        tracking: Boolean,
        issue: TrackingIssue?,
        project: (Vec3) -> Offset?,
        aim: Vec3?,
        ring: List<Offset>,
        snapped: Boolean,
        eye: Vec3,
    ): MeasureUiState {
        val screenSegments = mutableListOf<ScreenSegment>()
        val lengths = mutableListOf<Float>()
        segments.forEach { seg ->
            val a = seg.start.pose.toVec3()
            val b = seg.end.pose.toVec3()
            val cm = (b - a).length() * 100f
            lengths += cm
            val from = project(a)
            val to = project(b)
            if (from != null && to != null) {
                // 휴지통: 끝점에서 선 방향으로 조금 더 나간 곳
                val d = to - from
                val dir = d / d.getDistance().coerceAtLeast(1f)
                screenSegments += ScreenSegment(from, to, "${cm.roundToInt()} cm", to + dir * TRASH_OFFSET_PX)
            }
        }
        val start = pendingStart?.pose?.toVec3()
        val preview = if (start != null && aim != null) {
            val from = project(start)
            val to = project(aim)
            if (from != null && to != null) PreviewLine(from, to, "${((aim - start).length() * 100f).roundToInt()} cm") else null
        } else {
            null
        }
        return MeasureUiState(
            tracking = tracking,
            trackingIssue = issue,
            surfaceFound = surfaceFoundLatched,
            aimRing = ring,
            aimSnapped = snapped,
            aimDistanceCm = aim?.let { ((it - eye).length() * 100f).roundToInt() },
            segments = screenSegments,
            waitingEnd = start != null,
            points = allPoints().mapNotNull(project),
            previewLine = preview,
            lengthsCm = lengths,
        )
    }

    /** 방금 그린 카메라 배경을 비트맵으로 (GL은 아래→위 순서라 세로로 뒤집는다) */
    private fun readCameraPixels(): Bitmap {
        val buffer = ByteBuffer.allocateDirect(viewWidth * viewHeight * 4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0, 0, viewWidth, viewHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        buffer.rewind()
        val raw = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        raw.copyPixelsFromBuffer(buffer)
        val flip = android.graphics.Matrix().apply { preScale(1f, -1f) }
        return Bitmap.createBitmap(raw, 0, 0, viewWidth, viewHeight, flip, false).also { raw.recycle() }
    }

    private fun Pose.toVec3() = Vec3(tx(), ty(), tz())

    private companion object {
        const val UI_UPDATE_INTERVAL_MS = 33L

        /** 추적이 이 시간 넘게 끊겨야 "카메라를 움직여 보세요"를 띄운다 */
        const val TRACKING_LOST_GRACE_MS = 800L

        /** 조준 링 반지름 (m)·선분 수 */
        const val RING_RADIUS_M = 0.035f
        const val RING_SEGMENTS = 32

        /** 이보다 짧은 선은 같은 자리를 두 번 누른 것으로 보고 무시 (m) */
        const val MIN_SEGMENT_M = 0.005f

        /** 휴지통을 선 끝에서 띄우는 거리 (px) */
        const val TRASH_OFFSET_PX = 36f
    }
}
