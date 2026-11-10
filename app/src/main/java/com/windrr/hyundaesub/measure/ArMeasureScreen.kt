package com.windrr.hyundaesub.measure

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.windrr.hyundaesub.ar.ArMeasureRenderer
import com.windrr.hyundaesub.ar.MeasureUiState
import java.util.Locale

private val PointColor = Color(0xFF2563EB)
private val TopPointColor = Color(0xFFF97316)

@Composable
fun ArMeasureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    BackHandler(onBack = onBack)

    if (hasCameraPermission) {
        ArMeasureContent(onBack)
    } else {
        MessageScreen("물건을 재려면 카메라 권한이 필요해요.", onBack)
    }
}

@Composable
private fun ArMeasureContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember {
        GLSurfaceView(context).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        }
    }
    var renderer by remember { mutableStateOf<ArMeasureRenderer?>(null) }
    var uiState by remember { mutableStateOf(MeasureUiState()) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        var session: Session? = null
        var installRequested = false

        fun createSession(): Session? {
            return try {
                // ARCore(Google Play 서비스 AR)가 없으면 설치 화면으로 이동, 돌아오면 다시 시도
                if (ArCoreApk.getInstance().requestInstall(activity, !installRequested) ==
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED
                ) {
                    installRequested = true
                    return null
                }
                Session(context).also { s ->
                    s.configure(
                        Config(s).apply {
                            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                            focusMode = Config.FocusMode.AUTO
                            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                        },
                    )
                    val r = ArMeasureRenderer(s, { glView.display?.rotation ?: 0 }) { state ->
                        glView.post { uiState = state }
                    }
                    glView.setRenderer(r)
                    glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    renderer = r
                }
            } catch (e: UnavailableDeviceNotCompatibleException) {
                error = "이 기기는 AR 측정을 지원하지 않아요."
                null
            } catch (e: UnavailableUserDeclinedInstallationException) {
                error = "AR 측정을 쓰려면 'Google Play 서비스 AR' 설치가 필요해요."
                null
            } catch (e: UnavailableException) {
                error = "AR을 시작할 수 없어요. (${e.javaClass.simpleName})"
                null
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (session == null && error == null) session = createSession()
                    session?.let {
                        try {
                            it.resume()
                            glView.onResume()
                        } catch (e: CameraNotAvailableException) {
                            error = "카메라를 열 수 없어요. 다른 앱이 카메라를 쓰고 있는지 확인해 주세요."
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (session != null) {
                        glView.onPause()
                        session?.pause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (session != null) {
                glView.onPause()
                session?.pause()
                session?.close()
            }
        }
    }

    error?.let {
        MessageScreen(it, onBack)
        return
    }

    val textMeasurer = rememberTextMeasurer()
    Box(Modifier.fillMaxSize()) {
        // 렌더러 연결 전에 GLSurfaceView가 붙으면 surfaceCreated에서 크래시 → 세션 준비 후에만 표시
        if (renderer != null) {
            AndroidView(factory = { glView }, modifier = Modifier.fillMaxSize())
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(renderer) {
                    detectTapGestures { offset -> renderer?.onTap(offset.x, offset.y) }
                },
        ) {
            drawMeasureOverlay(uiState, textMeasurer)
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            GuideCard(guideMessage(uiState))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            if (uiState.size.widthCm != null) ResultCard(uiState)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("뒤로") }
                Button(onClick = { renderer?.reset() }, modifier = Modifier.weight(1f)) { Text("다시 재기") }
            }
        }
    }
}

private fun guideMessage(state: MeasureUiState): String = when {
    !state.tracking -> "폰을 천천히 움직여 주세요"
    !state.floorFound -> "바닥을 비추며 폰을 좌우로 천천히 움직여 주세요"
    state.pointCount == 0 -> "물건 바깥쪽 바닥 모서리 A를 탭하세요"
    state.pointCount == 1 -> "B를 탭하세요 (A → B = 가로)"
    state.pointCount == 2 -> "C를 탭하세요 (B → C = 세로)"
    !state.hasHeight -> "A 위로 올라간 선을 물건의 가장 높은 곳에 맞춰 탭하세요"
    else -> "측정 완료! 다시 재려면 '다시 재기'를 누르세요"
}

private fun DrawScope.drawMeasureOverlay(state: MeasureUiState, textMeasurer: TextMeasurer) {
    val points = state.screenPoints
    val base = points.take(3)
    for (i in 0 until base.size - 1) {
        val from = base[i] ?: continue
        val to = base[i + 1] ?: continue
        drawLine(PointColor, from, to, strokeWidth = 8f)
    }
    state.verticalGuide?.let { (bottom, top) ->
        drawLine(
            TopPointColor, bottom, top, strokeWidth = 6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f)),
        )
    }
    val a = points.getOrNull(0)
    val d = points.getOrNull(3)
    if (a != null && d != null) drawLine(TopPointColor, a, d, strokeWidth = 8f)

    listOf("A", "B", "C", "D").forEachIndexed { i, label ->
        val p = points.getOrNull(i) ?: return@forEachIndexed
        val color = if (i == 3) TopPointColor else PointColor
        drawCircle(Color.White, radius = 18f, center = p)
        drawCircle(color, radius = 13f, center = p)
        drawText(
            textMeasurer, label,
            topLeft = p + Offset(18f, -52f),
            style = TextStyle(color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun GuideCard(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC111827), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ResultCard(state: MeasureUiState) {
    val size = state.size
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("측정값", style = MaterialTheme.typography.labelLarge, color = Color(0xFF6B7280))
        Text(
            "가로 ${size.widthCm.cm()} · 세로 ${size.depthCm.cm()} · 높이 ${size.heightCm.cm()}",
            style = MaterialTheme.typography.titleMedium,
        )
        if (size.widthCm != null && size.depthCm != null && size.heightCm != null) {
            val fit = listOf(size.widthCm, size.depthCm, size.heightCm).map { MeasureGeometry.roundUpForFit(it).toInt() }
            Spacer(Modifier.height(8.dp))
            Text(
                "판정용 ${fit[0]} × ${fit[1]} × ${fit[2]} cm",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PointColor,
            )
            Text(
                "여유 ${MeasureGeometry.DEFAULT_MARGIN_CM.toInt()}cm를 더해 5cm 단위로 올렸어요 (임시값)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
            )
        }
    }
}

@Composable
private fun MessageScreen(message: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("돌아가기") }
    }
}

private fun Float?.cm(): String = this?.let { String.format(Locale.KOREA, "%.1fcm", it) } ?: "-"
