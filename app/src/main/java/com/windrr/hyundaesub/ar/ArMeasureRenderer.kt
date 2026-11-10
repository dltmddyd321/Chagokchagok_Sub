package com.windrr.hyundaesub.ar

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.measure.MeasureGeometry
import com.windrr.hyundaesub.measure.Vec3
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** 화면에 보여줄 측정 상태 (GL 스레드에서 매 프레임 생성) */
data class MeasureUiState(
    val tracking: Boolean = false,
    val floorFound: Boolean = false,
    val pointCount: Int = 0,
    val hasHeight: Boolean = false,
    /** A, B, C, D(높이 끝) 순서의 화면 좌표. 화면 밖이거나 아직 없으면 null */
    val screenPoints: List<Offset?> = emptyList(),
    /** 높이 측정 단계에서 A 위로 세운 안내선 (아래, 위) */
    val verticalGuide: Pair<Offset, Offset>? = null,
    val size: BoxSize = BoxSize(null, null, null),
)

/**
 * 점 찍기 방식 측정.
 * A·B·C = 물건 바깥쪽 바닥 (A→B 가로, B→C 세로), 높이 = A 위 수직선과 탭한 시선의 교차점.
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

    fun onTap(x: Float, y: Float) {
        taps.add(Offset(x, y))
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

        while (true) {
            val tap = taps.poll() ?: break
            if (tracking) handleTap(frame, tap, invViewProj)
        }

        onUpdate(buildState(tracking, floorFound, viewProj))
    }

    private fun handleTap(frame: Frame, tap: Offset, invViewProj: FloatArray) {
        when {
            anchors.size < 3 -> {
                // 바닥(위를 향한 수평면)에 닿은 지점만 인정
                val hit = frame.hitTest(tap.x, tap.y).firstOrNull {
                    val plane = it.trackable as? Plane
                    plane != null &&
                        plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        plane.isPoseInPolygon(it.hitPose)
                } ?: return
                anchors += hit.createAnchor()
            }
            heightM == null -> {
                val base = anchors.first().pose.toVec3()
                val ray = MeasureGeometry.rayFromScreen(tap.x, tap.y, viewWidth, viewHeight, invViewProj)
                heightM = MeasureGeometry.heightAlongVertical(base, ray)
            }
        }
    }

    private fun buildState(tracking: Boolean, floorFound: Boolean, viewProj: FloatArray): MeasureUiState {
        val points = anchors.map { it.pose.toVec3() }
        val project = { p: Vec3 ->
            MeasureGeometry.projectToScreen(p, viewProj, viewWidth, viewHeight)?.let { (x, y) -> Offset(x, y) }
        }
        val top = heightM?.let { points.first() + Vec3.UP * it }
        val guide = if (points.size == 3 && heightM == null) {
            val a = points.first()
            val bottom = project(a)
            val upper = project(a + Vec3.UP * GUIDE_LINE_HEIGHT_M)
            if (bottom != null && upper != null) bottom to upper else null
        } else {
            null
        }
        return MeasureUiState(
            tracking = tracking,
            floorFound = floorFound,
            pointCount = points.size,
            hasHeight = heightM != null,
            screenPoints = (points + listOfNotNull(top)).map(project),
            verticalGuide = guide,
            size = BoxSize(
                widthCm = if (points.size >= 2) MeasureGeometry.horizontalDistance(points[0], points[1]) * 100f else null,
                depthCm = if (points.size >= 3) MeasureGeometry.horizontalDistance(points[1], points[2]) * 100f else null,
                heightCm = heightM?.times(100f),
            ),
        )
    }

    private fun com.google.ar.core.Pose.toVec3() = Vec3(tx(), ty(), tz())

    private companion object {
        const val GUIDE_LINE_HEIGHT_M = 1.5f
    }
}
