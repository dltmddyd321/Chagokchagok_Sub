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
                    s.configure(
                        Config(s).apply {
                            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                            focusMode = Config.FocusMode.AUTO
                            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                            if (s.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                depthMode = Config.DepthMode.AUTOMATIC
                            }
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
                AndroidView(
                    factory = { glView.apply { doOnLayout { view -> view.post { view.requestLayout() } } } },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(renderer) {
                        detectTapGestures { offset -> renderer?.onTap(offset.x, offset.y) }
                    }
                    // 찍은 점 근처에서 끌면 그 점을 옮긴다 (빈 곳에서 끌면 무시)
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
                drawCornerBrackets()
                drawMeasureOverlay(uiState, textMeasurer)
            }
            GuidePill(
                state = uiState,
                shape = shape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp),
            )
        }

        // 하단: 측정 중 = 진행 단계, 완료 = 확인
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(132.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.step == MeasureStep.DONE) {
                ShutterButton(
                    done = true,
                    enabled = uiState.size.isComplete,
                    onClick = { if (uiState.size.isComplete) onMeasured(uiState.size) },
                )
            } else {
                Text(
                    text = "꼭짓점 ${uiState.step.ordinal + 1} / $VERTEX_COUNT",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * 안내 문구. 우선순위: 추적 끊김(0.8초 이상) → 바닥 미인식 → 직전 터치 실패 → 단계 안내.
 * 단계 안내는 순서를 강요하지 않는다 (꼭짓점은 아무 순서로, 찍은 점은 끌어서 옮길 수 있다).
 */
private fun guideMessage(state: MeasureUiState, shape: MeasureShape): String = when {
    !state.tracking -> "폰을 천천히 움직여 주세요"
    !state.floorFound && state.step == MeasureStep.POINT_A -> "물건 주변 바닥을 비추며 폰을 천천히 움직여 주세요"
    state.tapMissed -> "그 위치를 인식하지 못했어요. 바닥이나 물건 가장자리를 다시 터치해 주세요"
    state.badCircle -> "세 점이 한 줄에 가까워요. 둘레를 따라 서로 떨어진 곳을 터치해 주세요"
    shape == MeasureShape.CYLINDER -> cylinderGuide(state.step)
    else -> boxGuide(state.step)
}

/** 네모: 바닥 꼭짓점 아무 순서로 세 개 → 윗면 꼭짓점 하나 */
private fun boxGuide(step: MeasureStep): String = when (step) {
    MeasureStep.POINT_A -> "물건 바닥 꼭짓점 하나를 터치하세요"
    MeasureStep.POINT_B -> "바닥 꼭짓점을 하나 더 터치하세요"
    MeasureStep.POINT_C -> "바닥 꼭짓점을 하나 더 터치하세요 (순서는 상관없어요)"
    MeasureStep.HEIGHT -> "물건 윗면 꼭짓점을 터치하세요 · 찍은 점은 끌어서 옮길 수 있어요"
    MeasureStep.DONE -> "점을 끌어 맞춘 뒤 확인을 눌러 주세요"
}

/** 원통: 바닥 둘레 아무 곳 세 점 → 윗면 가장자리 한 점 */
private fun cylinderGuide(step: MeasureStep): String = when (step) {
    MeasureStep.POINT_A -> "물건 바닥 둘레의 한 곳을 터치하세요"
    MeasureStep.POINT_B -> "바닥 둘레를 따라 떨어진 다른 곳을 터치하세요"
    MeasureStep.POINT_C -> "바닥 둘레의 세 번째 곳을 터치하세요 (고르게 떨어질수록 정확해요)"
    MeasureStep.HEIGHT -> "물건 윗면 가장자리를 터치하세요 · 찍은 점은 끌어서 옮길 수 있어요"
    MeasureStep.DONE -> "점을 끌어 맞춘 뒤 확인을 눌러 주세요"
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
private val DragRing = Color(0xFF3D7BFF)

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
