package com.windrr.hyundaesub.measure

import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.border
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import android.net.Uri
import android.graphics.Bitmap
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.windrr.hyundaesub.ar.MeasureUiState
import com.windrr.hyundaesub.ar.PreviewLine
import com.windrr.hyundaesub.ar.TrackingIssue
import kotlinx.coroutines.delay

/**
 * 전체 화면 AR 측정 (삼성 간편 측정과 같은 방식).
 * - 가운데 = 크기 측정 버튼: 조준 링을 맞추고 누르면 시작점, 다시 누르면 끝점 (선 하나)
 * - 오른쪽 = 일반 사진 촬영 (측정 선·버튼 없이 카메라 화면만)
 * 크기 측정으로 필요한 선(네모 = 가로·세로·높이 3개, 둥근 = 지름·높이 2개)을 다 재면 바로 다음 화면으로 넘어가고,
 * [onMeasured]로 치수와 가장 마지막에 찍은 사진(없으면 null)을 넘긴다.
 */
@Composable
fun ArMeasureScreen(
    onBack: () -> Unit,
    onMeasured: (size: BoxSize, photoUri: String?) -> Unit,
    shape: MeasureShape = MeasureShape.BOX,
) {
    val context = LocalContext.current
    val activity = context as Activity
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var hasCameraPermission by remember { mutableStateOf(granted()) }
    // 두 번 거절해 시스템 창이 더 뜨지 않는 상태 → 설정으로 안내
    var permanentlyDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasCameraPermission = ok
        if (!ok) permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    // 설정에서 허용하고 돌아오면 바로 측정 화면으로
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !hasCameraPermission && granted()) hasCameraPermission = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(onBack = onBack)

    when {
        hasCameraPermission -> ArMeasureContent(onBack, onMeasured, shape)
        permanentlyDenied -> MeasureNoticeScreen(
            title = stringResource(R.string.ar_permission_denied_title),
            body = stringResource(R.string.ar_permission_denied_body),
            primaryText = stringResource(R.string.ar_permission_open_settings),
            onPrimary = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            onBack = onBack,
            secondaryText = stringResource(R.string.ar_later),
        )
        // 시스템 권한 창 전에 왜 필요한지 먼저 보여 준다
        else -> MeasureNoticeScreen(
            title = stringResource(R.string.ar_permission_title),
            body = stringResource(R.string.ar_permission_body),
            primaryText = stringResource(R.string.ar_permission_allow),
            onPrimary = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = onBack,
            secondaryText = stringResource(R.string.ar_later),
            points = listOf(
                stringResource(R.string.ar_permission_point_measure),
                stringResource(R.string.ar_permission_point_only_while),
            ),
        )
    }
}

/** AR을 시작할 수 없을 때 안내 (제목·본문 문자열 리소스) */
private enum class MeasureError(@StringRes val title: Int, @StringRes val body: Int) {
    UNSUPPORTED(R.string.ar_error_unsupported_title, R.string.ar_error_unsupported_body),
    INSTALL_DECLINED(R.string.ar_error_install_title, R.string.ar_error_install_body),
    START_FAILED(R.string.ar_error_start_title, R.string.ar_error_start_body),
    CAMERA(R.string.ar_error_camera_title, R.string.ar_error_camera_body),
}

@Composable
private fun ArMeasureContent(
    onBack: () -> Unit,
    onMeasured: (size: BoxSize, photoUri: String?) -> Unit,
    shape: MeasureShape,
) {
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
    // 조준점이 기존 점에 달라붙는 거리
    val snapRadiusPx = with(LocalDensity.current) { 30.dp.toPx() }
    val trashRadiusPx = with(LocalDensity.current) { 22.dp.toPx() }
    var uiState by remember { mutableStateOf(MeasureUiState()) }
    var error by remember { mutableStateOf<MeasureError?>(null) }

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
                    // 회사에서 추적이 잘 되던 설정(79d95d6): 기본 카메라 설정 + 수평면 + 깊이(지원 시)
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
                        snapRadiusPx = snapRadiusPx,
                    ) { state ->
                        glView.post { uiState = state }
                    }
                    glView.setRenderer(r)
                    glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    renderer = r
                }
            } catch (e: UnavailableDeviceNotCompatibleException) {
                error = MeasureError.UNSUPPORTED
                null
            } catch (e: UnavailableUserDeclinedInstallationException) {
                error = MeasureError.INSTALL_DECLINED
                null
            } catch (e: UnavailableException) {
                // 예외 이름은 사용자에게 보이지 않게 로그로만
                Log.w(TAG, "AR 세션 시작 실패", e)
                error = MeasureError.START_FAILED
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
                            error = MeasureError.CAMERA
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
        MeasureNoticeScreen(
            title = stringResource(it.title),
            body = stringResource(it.body),
            primaryText = stringResource(R.string.ar_back),
            onPrimary = onBack,
            onBack = onBack,
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // 점을 찍을 때 잠깐 뜨는 안내 ("시작 지점을 추가했어요." / "끝 지점을 추가했어요.")
    var toast by remember { mutableStateOf<String?>(null) }
    var toastKey by remember { mutableStateOf(0) }
    var lastPointCount by remember { mutableStateOf(0) }
    val pointCount = uiState.segments.size * 2 + if (uiState.waitingEnd) 1 else 0
    LaunchedEffect(pointCount) {
        if (pointCount > lastPointCount) {
            toast = context.getString(if (uiState.waitingEnd) R.string.ar_toast_start_point else R.string.ar_toast_end_point)
            toastKey++
        }
        lastPointCount = pointCount
    }
    LaunchedEffect(toastKey) {
        if (toast != null) {
            delay(1500)
            toast = null
        }
    }
    val scanning = !uiState.tracking || (!uiState.surfaceFound && pointCount == 0)

    // 가장 마지막에 찍은 사진 (다음 화면으로 넘긴다)
    var lastPhotoUri by remember { mutableStateOf<String?>(null) }
    var flash by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val takePhoto: () -> Unit = {
        renderer?.capturePhoto { bitmap ->
            scope.launch {
                lastPhotoUri = withContext(Dispatchers.IO) { savePhoto(context, bitmap) }
                toast = context.getString(R.string.ar_toast_photo)
                toastKey++
            }
        }
        flash = true
    }
    LaunchedEffect(flash) {
        if (flash) {
            delay(120)
            flash = false
        }
    }

    // 크기 측정 버튼으로 필요한 선을 다 재면 다음 화면으로.
    // 가로·세로·높이는 잰 순서가 아니라 선 방향으로 정한다 (가장 수직인 선 = 높이)
    LaunchedEffect(uiState.lengthsCm.size, uiState.waitingEnd) {
        if (uiState.waitingEnd) return@LaunchedEffect
        val size = MeasureGeometry.assignDimensions(uiState.lengthsCm, uiState.verticalRatios, shape) ?: return@LaunchedEffect
        delay(400) // 마지막 선이 그려지는 걸 잠깐 보여주고
        onMeasured(size, lastPhotoUri)
    }

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
                // 선 끝의 휴지통을 누르면 그 선을 지운다
                .pointerInput(uiState.segments) {
                    detectTapGestures { tap ->
                        val index = uiState.segments.indexOfFirst { (it.trashAt - tap).getDistance() <= trashRadiusPx }
                        if (index >= 0) renderer?.deleteSegment(index)
                    }
                },
        ) {
            if (scanning) return@Canvas
            drawSegments(uiState, textMeasurer)
            uiState.previewLine?.let { drawPreviewLine(it, textMeasurer) }
            drawAimRing(uiState)
        }

        // 조준 링 아래 "대상까지 88cm"
        if (!scanning) {
            uiState.aimDistanceCm?.let { cm ->
                Text(
                    text = stringResource(R.string.ar_aim_distance, cm),
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
            ScanningGuide(issue = uiState.trackingIssue, modifier = Modifier.align(Alignment.Center))
        }
        // 상단 안내: 대표 사진을 찍기 전에는 사진 안내(인식 중에도 표시), 찍은 뒤에는 잴 치수 안내
        if (lastPhotoUri == null) {
            TopHint(
                text = stringResource(R.string.ar_photo_first),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            )
        } else if (!scanning) {
            DimensionHint(
                measuredCount = uiState.segments.size,
                shape = shape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            )
        }

        if (flash) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.6f)),
            )
        }

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
                RoundIconButton(
                    text = "↺",
                    enabled = pointCount > 0,
                    onClick = { renderer?.undo() },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                // 가운데: 크기 측정 (시작점·끝점 찍기)
                MeasureButton(
                    enabled = !scanning && (uiState.aimDistanceCm != null),
                    onClick = { renderer?.captureAtAim() },
                    modifier = Modifier.align(Alignment.Center),
                )
                // 오른쪽: 일반 사진 촬영
                PhotoButton(
                    enabled = renderer != null && uiState.tracking,
                    onClick = takePhoto,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

/** 몇 번째 선을 잴 차례인지 알려주는 상단 안내 */
@Composable
private fun DimensionHint(measuredCount: Int, shape: MeasureShape, modifier: Modifier = Modifier) {
    // 어느 선이 높이인지는 방향으로 정하므로 순서는 묻지 않는다
    val text = when (shape) {
        MeasureShape.CYLINDER -> when (measuredCount) {
            0 -> R.string.ar_cylinder_first
            1 -> R.string.ar_cylinder_last
            else -> R.string.ar_done
        }
        else -> when (measuredCount) {
            0 -> R.string.ar_box_first
            1 -> R.string.ar_box_next
            2 -> R.string.ar_box_last
            else -> R.string.ar_done
        }
    }
    TopHint(text = stringResource(text), modifier = modifier)
}

/** 상단 반투명 안내 칩 */
@Composable
private fun TopHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(horizontal = 56.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** 표면 인식 전: 평면 위에서 폰이 좌우로 움직이는 안내 */
@Composable
private fun ScanningGuide(issue: TrackingIssue?, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scanShift",
    )
    val detail = stringResource(
        when (issue) {
            TrackingIssue.LOW_LIGHT -> R.string.ar_scan_low_light
            TrackingIssue.FEW_FEATURES -> R.string.ar_scan_few_features
            TrackingIssue.FAST_MOTION -> R.string.ar_scan_fast_motion
            TrackingIssue.CAMERA -> R.string.ar_scan_camera
            // 준비 중·이유를 모를 때도 할 일을 알려 준다
            TrackingIssue.INITIALIZING, null -> R.string.ar_scan_initializing
        },
    )
    Column(modifier = modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(width = 170.dp, height = 70.dp)) {
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
            val phoneW = 20.dp.toPx()
            val phoneH = 38.dp.toPx()
            val left = w / 2f - phoneW / 2f + shift * w * 0.18f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, 0f),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(2.dp.toPx()),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(text = stringResource(R.string.ar_scan_title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(text = detail, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoundIconButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, fontSize = 20.sp)
    }
}

/** 가운데 크기 측정 버튼: 반투명 원 + 흰 테두리 + "+" (시안) */
@Composable
private fun MeasureButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(60.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .border(2.5.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "+", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
    }
}

/** 오른쪽 사진 촬영 버튼: 흰 테두리 안 흰 원 (시안) */
@Composable
private fun PhotoButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** 찍은 사진을 앱 캐시에 JPEG로 저장하고 file URI를 돌려준다 (긴 변 1440px로 줄임) */
private fun savePhoto(context: android.content.Context, bitmap: Bitmap): String? = runCatching {
    val scale = PHOTO_MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    } else {
        bitmap
    }
    val dir = File(context.cacheDir, "ar_photos").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    Uri.fromFile(file).toString()
}.getOrNull()

private const val PHOTO_MAX_SIDE = 1440

/** 조준 링: 표면 방향으로 누운 흰 원 + 가운데 점. 기존 점에 달라붙으면 노란색 */
private fun DrawScope.drawAimRing(state: MeasureUiState) {
    val ring = state.aimRing
    val color = if (state.aimSnapped) PreviewYellow else Color.White
    if (ring.isNotEmpty()) {
        val path = Path().apply {
            moveTo(ring.first().x, ring.first().y)
            ring.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, Color.Black.copy(alpha = 0.25f), style = Stroke(4.dp.toPx()))
        drawPath(path, color, style = Stroke(2.dp.toPx()))
    } else {
        // 표면을 못 찾은 곳: 흐린 원 (+ 비활성)
        drawCircle(Color.White.copy(alpha = 0.4f), radius = 22.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
    }
    drawCircle(color, radius = 3.5.dp.toPx(), center = center)
}

/** 확정된 선: 흰 선 + 끝점 + 선 방향 흰 길이 라벨 + 끝의 휴지통 */
private fun DrawScope.drawSegments(state: MeasureUiState, textMeasurer: TextMeasurer) {
    state.segments.forEach { seg ->
        drawLine(Color.Black.copy(alpha = 0.25f), seg.from, seg.to, 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color.White, seg.from, seg.to, 2.dp.toPx(), cap = StrokeCap.Round)
        drawPill(seg.text, (seg.from + seg.to) / 2f, seg.to - seg.from, Color.White, textMeasurer)
        // 휴지통: 검은 원 + 흰 통 모양
        drawCircle(Color(0xFF1F1F1F), radius = 11.dp.toPx(), center = seg.trashAt)
        val c = seg.trashAt
        val bw = 8.dp.toPx()
        val bh = 9.dp.toPx()
        drawRect(Color.White, topLeft = Offset(c.x - bw / 2f, c.y - bh / 2f + 1.dp.toPx()), size = Size(bw, bh), style = Stroke(1.5.dp.toPx()))
        drawLine(Color.White, Offset(c.x - 5.dp.toPx(), c.y - bh / 2f), Offset(c.x + 5.dp.toPx(), c.y - bh / 2f), 1.5.dp.toPx())
    }
    state.points.forEach { p ->
        drawCircle(Color.Black.copy(alpha = 0.25f), radius = 6.dp.toPx(), center = p)
        drawCircle(Color.White, radius = 4.5.dp.toPx(), center = p)
    }
}

/** 노란 실시간 선 + 선 방향 노란 길이 라벨 */
private fun DrawScope.drawPreviewLine(line: PreviewLine, textMeasurer: TextMeasurer) {
    drawLine(Color.Black.copy(alpha = 0.25f), line.from, line.to, 5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(PreviewYellow, line.from, line.to, 3.dp.toPx(), cap = StrokeCap.Round)
    drawPill(line.text, (line.from + line.to) / 2f, line.to - line.from, PreviewYellow, textMeasurer)
}

/** 선 방향(dir)으로 기운 알약 라벨. 글자가 뒤집히지 않게 -90~90도로 맞춘다 */
private fun DrawScope.drawPill(text: String, at: Offset, dir: Offset, background: Color, textMeasurer: TextMeasurer) {
    val layout = textMeasurer.measure(text, TextStyle(color = DarkText, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    val w = layout.size.width + 14.dp.toPx()
    val h = layout.size.height + 6.dp.toPx()
    var degrees = Math.toDegrees(kotlin.math.atan2(dir.y, dir.x).toDouble()).toFloat()
    if (degrees > 90f) degrees -= 180f
    if (degrees < -90f) degrees += 180f
    rotate(degrees, pivot = at) {
        drawRoundRect(
            color = background,
            topLeft = Offset(at.x - w / 2f, at.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f),
        )
        drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
    }
}

private val DarkText = Color(0xFF05082B)
private val PreviewYellow = Color(0xFFE6F53A)

private const val TAG = "ArMeasure"
