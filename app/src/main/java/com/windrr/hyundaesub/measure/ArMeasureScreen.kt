package com.windrr.hyundaesub.measure

import android.Manifest
import com.windrr.hyundaesub.ar.PreviewLine
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.platform.LocalDensity
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
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.windrr.hyundaesub.ar.TrackingIssue
import java.util.EnumSet
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.CameraConfig
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
fun ArMeasureScreen(
    onBack: () -> Unit,
    onMeasured: (BoxSize) -> Unit,
    shape: MeasureShape = MeasureShape.BOX,
) {
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
        ArMeasureContent(onBack, onMeasured, shape)
    } else {
        MessageScreen("물건을 재려면 카메라 권한이 필요해요.", onBack)
    }
}

@Composable
private fun ArMeasureContent(onBack: () -> Unit, onMeasured: (BoxSize) -> Unit, shape: MeasureShape) {
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
    // 찍은 점을 손가락으로 잡았다고 볼 거리
    val grabRadiusPx = with(LocalDensity.current) { 36.dp.toPx() }
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
                    // 30fps 카메라 설정: 60fps에서 영상 처리가 밀려 추적 초기화(VIO)가 계속 실패하던 문제 완화
                    val fps30 = CameraConfigFilter(s).setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
                    s.getSupportedCameraConfigs(fps30).firstOrNull()?.let { s.cameraConfig = it }
                    s.configure(
                        Config(s).apply {
                            // 옆면에도 조준 링이 붙도록 수직면까지 인식
                            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                            focusMode = Config.FocusMode.AUTO
                            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                            // 표면을 못 찾아도 탭한 자리에 점을 두고, 실제 깊이를 찾으면 보정
                            instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                            // ML 깊이 추정은 끔: 처리가 밀리며 IMU-카메라 동기화가 깨져 추적이 시작되지 않았음
                            depthMode = Config.DepthMode.DISABLED
                        },
                    )
                    val r = ArMeasureRenderer(
                        session = s,
                        displayRotation = { glView.display?.rotation ?: 0 },
                        shape = shape,
                        grabRadiusPx = grabRadiusPx,
                    ) { state ->
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

    // 점을 찍거나 되돌릴 때 잠깐 뜨는 안내 (삼성 간편 측정의 "시작 지점을 추가했어요.")
    var toast by remember { mutableStateOf<String?>(null) }
    var lastStep by remember { mutableStateOf(MeasureStep.POINT_A) }
    LaunchedEffect(uiState.step) {
        val step = uiState.step
        if (step != lastStep) {
            toast = if (step.ordinal > lastStep.ordinal) addedMessage(step, shape) else "되돌렸어요."
            lastStep = step
            delay(1500)
            toast = null
        }
    }
    val scanning = !uiState.tracking ||
        (uiState.step == MeasureStep.POINT_A && uiState.aimRing.isEmpty() && !uiState.floorFound)

    // 카메라는 전체 화면, 버튼·안내는 그 위에 겹친다
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 렌더러 연결 전에 GLSurfaceView가 붙으면 surfaceCreated에서 크래시 → 세션 준비 후에만 표시
        if (renderer != null) {
            AndroidView(
                factory = { glView.apply { doOnLayout { view -> view.post { view.requestLayout() } } } },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Canvas(
            Modifier
                .fillMaxSize()
                // 찍은 점 근처에서 끌면 그 점을 옮긴다 (점 찍기는 + 버튼)
                .pointerInput(renderer) {
                    detectDragGestures(
                        onDragStart = { offset -> renderer?.onDragStart(offset.x, offset.y) },
                        onDrag = { change, _ ->
                            change.consume()
                            renderer?.onDrag(change.position.x, change.position.y)
                        },
                        onDragEnd = { renderer?.onDragEnd() },
                        onDragCancel = { renderer?.onDragEnd() },
                    )
                },
        ) {
            if (scanning) return@Canvas
            drawMeasureOverlay(uiState, textMeasurer)
            uiState.previewLine?.let { drawPreviewLine(it, textMeasurer) }
            if (uiState.step != MeasureStep.DONE) drawAimRing(uiState)
        }

        // 거리 표시: 조준 링 바로 아래 "대상까지 88cm"
        if (!scanning && uiState.step != MeasureStep.DONE) {
            uiState.aimDistanceCm?.let { cm ->
                Text(
                    text = "대상까지 ${cm}cm",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        if (scanning) {
            ScanningGuide(
                message = guideMessage(uiState, shape),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            GuidePill(
                state = uiState,
                shape = shape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp, start = 24.dp, end = 24.dp),
            )
        }

        // 상단: 뒤로
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = Color.White)
        }

        // 하단: ↺ 되돌리기 · + 점 찍기 · ○ 확인
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            toast?.let {
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xCC2B2B2B))
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 40.dp),
            ) {
                UndoButton(
                    enabled = uiState.step != MeasureStep.POINT_A,
                    onClick = { renderer?.undo() },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                AddPointButton(
                    enabled = !scanning && uiState.step != MeasureStep.DONE,
                    onClick = { renderer?.captureAtAim() },
                    modifier = Modifier.align(Alignment.Center),
                )
                ShutterButton(
                    done = uiState.step == MeasureStep.DONE,
                    enabled = uiState.step == MeasureStep.DONE && uiState.size.isComplete,
                    onClick = { if (uiState.size.isComplete) onMeasured(uiState.size) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

/** 점을 찍은 뒤 잠깐 보여줄 문구 (새 단계 기준) */
private fun addedMessage(step: MeasureStep, shape: MeasureShape): String {
    val noun = if (shape == MeasureShape.CYLINDER) "둘레 점" else "꼭짓점"
    return when (step) {
        MeasureStep.POINT_B -> "첫 번째 ${noun}을 추가했어요."
        MeasureStep.POINT_C -> "두 번째 ${noun}을 추가했어요."
        MeasureStep.HEIGHT -> "세 번째 ${noun}을 추가했어요."
        MeasureStep.DONE -> "높이를 추가했어요."
        MeasureStep.POINT_A -> "되돌렸어요."
    }
}

/** 표면 인식 전: 평면 위에서 폰이 좌우로 움직이는 안내 (삼성 간편 측정 시작 화면) */
@Composable
private fun ScanningGuide(message: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scanShift",
    )
    Column(modifier = modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(width = 170.dp, height = 70.dp)) {
            // 바닥 평면 (사다리꼴)
            val w = size.width
            val h = size.height
            val plane = Path().apply {
                moveTo(w * 0.22f, h * 0.30f)
                lineTo(w * 0.78f, h * 0.30f)
                lineTo(w, h * 0.85f)
                lineTo(0f, h * 0.85f)
                close()
            }
            drawPath(plane, Color.White.copy(alpha = 0.35f))
            // 폰 (좌우로 이동)
            val phoneW = 20.dp.toPx()
            val phoneH = 38.dp.toPx()
            val left = w / 2f - phoneW / 2f + shift * w * 0.18f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(phoneW, phoneH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = Stroke(2.dp.toPx()),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(text = "카메라를 움직여 보세요", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(text = message, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun UndoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "↺", color = Color.White, fontSize = 20.sp)
    }
}

/** "+" 버튼: 가운데 조준 링 위치에 점 찍기 */
@Composable
private fun AddPointButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "+", color = DoneText, fontSize = 36.sp, fontWeight = FontWeight.Light)
    }
}

/** 조준 링: 표면 방향으로 누운 흰 원 + 가운데 점. 표면이 없으면 화면에 평평한 원 */
private fun DrawScope.drawAimRing(state: MeasureUiState) {
    val ring = state.aimRing
    if (ring.isNotEmpty()) {
        val path = Path().apply {
            moveTo(ring.first().x, ring.first().y)
            ring.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, Color.Black.copy(alpha = 0.25f), style = Stroke(4.dp.toPx()))
        drawPath(path, Color.White, style = Stroke(2.dp.toPx()))
    } else {
        drawCircle(Color.White.copy(alpha = 0.5f), radius = 22.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
    }
    drawCircle(Color.White, radius = 3.5.dp.toPx(), center = center)
}

/** 노란 실시간 선 + 선 방향으로 기운 노란 길이 라벨 */
private fun DrawScope.drawPreviewLine(line: PreviewLine, textMeasurer: TextMeasurer) {
    drawLine(Color.Black.copy(alpha = 0.25f), line.from, line.to, 5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(PreviewYellow, line.from, line.to, 3.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(Color.White, radius = 5.dp.toPx(), center = line.from)
    drawPill(line.text, (line.from + line.to) / 2f, line.to - line.from, PreviewYellow, textMeasurer)
}

/** 선 방향(dir)으로 기운 알약 라벨. 글자가 뒤집히지 않게 -90~90도로 맞춘다 */
private fun DrawScope.drawPill(text: String, at: Offset, dir: Offset, background: Color, textMeasurer: TextMeasurer) {
    val layout = textMeasurer.measure(text, TextStyle(color = DoneText, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    val w = layout.size.width + 14.dp.toPx()
    val h = layout.size.height + 6.dp.toPx()
    var degrees = Math.toDegrees(kotlin.math.atan2(dir.y, dir.x).toDouble()).toFloat()
    if (degrees > 90f) degrees -= 180f
    if (degrees < -90f) degrees += 180f
    rotate(degrees, pivot = at) {
        drawRoundRect(
            color = background,
            topLeft = Offset(at.x - w / 2f, at.y - h / 2f),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
        )
        drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
    }
}

private fun guideMessage(state: MeasureUiState, shape: MeasureShape): String = when {
    !state.tracking -> when (state.trackingIssue) {
        TrackingIssue.LOW_LIGHT -> "너무 어두워요. 조명을 켜거나 밝은 곳에서 재 주세요"
        TrackingIssue.FEW_FEATURES -> "바닥 무늬가 잘 안 보여요. 물건과 주변 바닥을 함께 비춰 주세요"
        TrackingIssue.FAST_MOTION -> "조금 더 천천히 움직여 주세요"
        TrackingIssue.CAMERA -> "카메라를 쓸 수 없어요. 다른 앱을 닫고 다시 시도해 주세요"
        else -> "주변을 인식하고 있어요. 물건 주변을 비추며 폰을 천천히 좌우로 움직여 주세요"
    }
    !state.floorFound && state.step == MeasureStep.POINT_A -> "물건 주변 바닥을 비추며 폰을 천천히 움직여 주세요"
    state.tapMissed -> "그 위치를 인식하지 못했어요. 바닥이나 물건 가장자리를 다시 터치해 주세요"
    state.badCircle -> "세 점이 한 줄에 가까워요. 둘레를 따라 서로 떨어진 곳을 터치해 주세요"
    shape == MeasureShape.CYLINDER -> cylinderGuide(state.step)
    else -> boxGuide(state.step)
}

/** 네모: 가운데 원을 바닥 꼭짓점 셋(아무 순서) → 윗면 꼭짓점 하나에 차례로 맞추고 + */
private fun boxGuide(step: MeasureStep): String = when (step) {
    MeasureStep.POINT_A -> "가운데 원을 물건 바닥 꼭짓점에 맞추고 + 를 누르세요"
    MeasureStep.POINT_B -> "카메라를 옮겨 다음 바닥 꼭짓점에 맞추고 + 를 누르세요"
    MeasureStep.POINT_C -> "바닥 꼭짓점 하나 더 (순서는 상관없어요)"
    MeasureStep.HEIGHT -> "가운데 원을 물건 윗면 꼭짓점에 맞추고 + 를 누르세요"
    MeasureStep.DONE -> "측정이 끝났어요. 점을 끌어 맞추거나 확인을 누르세요"
}

/** 원통: 가운데 원을 바닥 둘레 세 곳 → 윗면 가장자리에 맞추고 + */
private fun cylinderGuide(step: MeasureStep): String = when (step) {
    MeasureStep.POINT_A -> "가운데 원을 물건 바닥 둘레에 맞추고 + 를 누르세요"
    MeasureStep.POINT_B -> "둘레를 따라 떨어진 다른 곳에 맞추고 + 를 누르세요"
    MeasureStep.POINT_C -> "둘레의 세 번째 곳 (고르게 떨어질수록 정확해요)"
    MeasureStep.HEIGHT -> "가운데 원을 물건 윗면 가장자리에 맞추고 + 를 누르세요"
    MeasureStep.DONE -> "측정이 끝났어요. 점을 끌어 맞추거나 확인을 누르세요"
}

@Composable
private fun GuidePill(state: MeasureUiState, shape: MeasureShape, modifier: Modifier = Modifier) {
    Text(
        text = guideMessage(state, shape),
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
private fun ShutterButton(done: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (done) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text(text = "확인", color = DoneText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    state.points.forEachIndexed { i, p ->
        if (i == state.draggingIndex) {
            // 끌고 있는 점: 크게 + 파란 테두리
            drawCircle(DragRing.copy(alpha = 0.35f), radius = 22.dp.toPx(), center = p)
            drawCircle(Color.White, radius = 9.dp.toPx(), center = p)
            drawCircle(DragRing, radius = 9.dp.toPx(), center = p, style = Stroke(2.5.dp.toPx()))
        } else {
            drawCircle(Color.Black.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = p)
            drawCircle(Color.White, radius = 7.dp.toPx(), center = p)
        }
    }

    // 확정된 치수: 선 방향으로 기운 흰 알약 라벨 (선에 직각인 outward로 선 방향을 되돌린다)
    state.labels.forEach { label ->
        drawPill(label.text, label.at, Offset(label.outward.y, -label.outward.x), Color.White, textMeasurer)
    }
}

private val DoneText = Color(0xFF05082B)
private val DragRing = Color(0xFF3D7BFF)
private val PreviewYellow = Color(0xFFE6F53A)

private const val VERTEX_COUNT = 4

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
