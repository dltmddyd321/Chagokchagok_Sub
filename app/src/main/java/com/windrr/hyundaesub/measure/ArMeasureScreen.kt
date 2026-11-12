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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ar.ArMeasureRenderer
import com.windrr.hyundaesub.ar.MeasureStep
import com.windrr.hyundaesub.ar.MeasureUiState

/**
 * 전체 화면 AR 측정. 바닥 모서리 세 점 + 높이를 찍으면 직육면체와 치수를 실시간으로 보여준다.
 * 측정이 끝난 뒤 셔터를 누르면 [onMeasured]로 가로·세로·높이(cm)를 넘긴다.
 */
@Composable
fun ArMeasureScreen(onBack: () -> Unit, onMeasured: (BoxSize) -> Unit) {
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
        ArMeasureContent(onBack, onMeasured)
    } else {
        MessageScreen("물건을 재려면 카메라 권한이 필요해요.", onBack)
    }
}

@Composable
private fun ArMeasureContent(onBack: () -> Unit, onMeasured: (BoxSize) -> Unit) {
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
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 상단: 뒤로 + 되돌리기
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "뒤로",
                    tint = Color.White,
                )
            }
            if (uiState.step != MeasureStep.POINT_A) {
                Text(
                    text = "되돌리기",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { renderer?.undo() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // 카메라 영역
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
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
                drawCornerBrackets()
                drawMeasureOverlay(uiState, textMeasurer)
                if (uiState.step != MeasureStep.DONE) drawAim(uiState.aimValid)
            }
            GuidePill(
                state = uiState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp),
            )
        }

        // 하단: 셔터 (측정 중 = 조준점에 점 찍기, 완료 = 짐에 담기)
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(132.dp),
            contentAlignment = Alignment.Center,
        ) {
            ShutterButton(
                done = uiState.step == MeasureStep.DONE,
                enabled = uiState.canCapture,
                onClick = {
                    if (uiState.step == MeasureStep.DONE) {
                        if (uiState.size.isComplete) onMeasured(uiState.size)
                    } else {
                        renderer?.captureAtAim()
                    }
                },
            )
        }
    }
}

private fun guideMessage(state: MeasureUiState): String = when {
    !state.tracking -> "폰을 천천히 움직여 주세요"
    !state.floorFound -> "바닥을 비추며 폰을 좌우로 천천히 움직여 주세요"
    state.step == MeasureStep.POINT_A -> "가운데 원을 물건 바깥쪽 바닥 모서리에 맞추고 셔터를 누르세요"
    state.step == MeasureStep.POINT_B -> "가로 끝 모서리에 맞추고 셔터를 누르세요"
    state.step == MeasureStep.POINT_C -> "세로 끝 모서리에 맞추고 셔터를 누르세요"
    state.step == MeasureStep.HEIGHT -> "가운데 원을 물건의 가장 높은 곳에 맞추고 셔터를 누르세요"
    else -> "셔터를 누르면 측정값을 확인할 수 있어요"
}

@Composable
private fun GuidePill(state: MeasureUiState, modifier: Modifier = Modifier) {
    Text(
        text = guideMessage(state),
        color = Color.White,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ShutterButton(done: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text(text = "확인", color = DoneText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 카메라 영역 네 귀퉁이 흰 브래킷 */
private fun DrawScope.drawCornerBrackets() {
    val insetX = 28.dp.toPx()
    val insetY = 24.dp.toPx()
    val arm = 30.dp.toPx()
    val stroke = 3.dp.toPx()
    val l = insetX
    val r = size.width - insetX
    val t = insetY
    val b = size.height - insetY
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(Color.White, Offset(x, y), Offset(x + dx * arm, y), stroke, cap = StrokeCap.Square)
        drawLine(Color.White, Offset(x, y), Offset(x, y + dy * arm), stroke, cap = StrokeCap.Square)
    }
    corner(l, t, 1f, 1f)
    corner(r, t, -1f, 1f)
    corner(l, b, 1f, -1f)
    corner(r, b, -1f, -1f)
}

/** 화면 가운데 조준점. 찍을 수 있는 곳이면 진하게 */
private fun DrawScope.drawAim(valid: Boolean) {
    val c = center
    val color = if (valid) Color.White else Color.White.copy(alpha = 0.45f)
    drawCircle(Color.Black.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = c, style = Stroke(4.dp.toPx()))
    drawCircle(color, radius = 16.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
    drawCircle(color, radius = 3.dp.toPx(), center = c)
}

/** 직육면체 점선 + 치수 라벨 + 찍은 점 */
private fun DrawScope.drawMeasureOverlay(state: MeasureUiState, textMeasurer: TextMeasurer) {
    val measuredDash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
    val outlineDash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
    state.edges.forEach { e ->
        if (e.measured) {
            drawLine(Color.Black.copy(alpha = 0.3f), e.from, e.to, 4.dp.toPx(), pathEffect = measuredDash)
            drawLine(Color.White, e.from, e.to, 2.5.dp.toPx(), pathEffect = measuredDash)
        } else {
            drawLine(Color.White.copy(alpha = 0.5f), e.from, e.to, 1.5.dp.toPx(), pathEffect = outlineDash)
        }
    }

    state.points.forEach { p ->
        drawCircle(Color.Black.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = p)
        drawCircle(Color.White, radius = 7.dp.toPx(), center = p)
    }
    state.aimPoint?.let { p -> drawCircle(Color.White.copy(alpha = 0.8f), radius = 5.dp.toPx(), center = p) }

    val style = TextStyle(
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), blurRadius = 8f),
    )
    state.labels.forEach { label ->
        val layout = textMeasurer.measure(label.text, style)
        val w = layout.size.width.toFloat()
        val h = layout.size.height.toFloat()
        // 라벨 중심을 선에서 바깥쪽으로 (라벨 크기 절반 + 8dp) 띄운다
        val gap = 8.dp.toPx() + kotlin.math.abs(label.outward.x) * w / 2f + kotlin.math.abs(label.outward.y) * h / 2f
        val center = label.at + label.outward * gap
        drawText(layout, topLeft = Offset(center.x - w / 2f, center.y - h / 2f))
    }
}

private val DoneText = Color(0xFF05082B)

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
