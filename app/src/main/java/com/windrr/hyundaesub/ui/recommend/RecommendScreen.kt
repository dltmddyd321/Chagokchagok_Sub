package com.windrr.hyundaesub.ui.recommend

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.ExitConfirmDialog
import com.windrr.hyundaesub.ui.component.LOAD_FLOW_STEPS
import com.windrr.hyundaesub.ui.component.StepIndicator
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** 배치 추천 (짐 등록 흐름 마지막 단계) */
@Composable
fun RecommendRoute(
    input: RecommendInput,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onNoLayout: (resultCode: String) -> Unit,
    onConfirmed: (previewId: String, arrangementId: String) -> Unit,
    viewModel: RecommendViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.start(input) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RecommendEffect.NoLayout -> onNoLayout(effect.resultCode)
                is RecommendEffect.Confirmed -> onConfirmed(effect.previewId, effect.arrangementId)
                is RecommendEffect.Toast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 가이드 화면들에서 뒤로 → 배치 이미지, 배치 이미지에서 뒤로 → 짐 선택
    BackHandler {
        if (state.mode == RecommendMode.MAP) onBack() else viewModel.onIntent(RecommendIntent.BackToMap)
    }

    when {
        // 배치 계산 중 (처음 · 새 계획 만들기 · 다시 시도): 전체 화면 로딩
        state.phase == RecommendPhase.LOADING -> ArrangeLoadingScreen(onClose = onClose)
        else -> RecommendModeContent(state = state, viewModel = viewModel, onBack = onBack, onClose = onClose)
    }
}

@Composable
private fun RecommendModeContent(
    state: RecommendUiState,
    viewModel: RecommendViewModel,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    when (state.mode) {
        RecommendMode.MAP -> RecommendScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack, onClose = onClose)
        RecommendMode.GUIDE_LOADING -> GuideLoadingScreen(
            guideReady = state.selectedGuide is GuideStatus.Ready,
            onExit = { viewModel.onIntent(RecommendIntent.BackToMap) },
            onFinished = { viewModel.onIntent(RecommendIntent.GuideLoadingFinished) },
        )
        RecommendMode.GUIDE -> GuideScreen(state = state, onIntent = viewModel::onIntent, onClose = onClose)
    }
}

@Composable
fun RecommendScreen(
    state: RecommendUiState,
    onIntent: (RecommendIntent) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    // ✕는 홈까지 나가 버리므로 한 번 확인받는다
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    if (showExitConfirm) {
        ExitConfirmDialog(
            onContinue = { showExitConfirm = false },
            onExit = {
                showExitConfirm = false
                onClose()
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            TopBar(onClose = { showExitConfirm = true })
            Spacer(Modifier.height(28.dp))
            Text(
                text = "이렇게 배치해 보세요",
                color = ChagokTextPrimary,
                fontSize = 26.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(10.dp))
            Text(text = "저장한 물건을 선택하거나 새로 등록해요.", color = ChagokTextSecondary, fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))

            when (state.phase) {
                // 계산 중에는 전체 화면 로딩(ArrangeLoadingScreen)이 대신 보인다
                RecommendPhase.LOADING -> Unit
                RecommendPhase.ERROR -> StatusCard {
                    Text(
                        text = state.errorMessage ?: "배치를 계산하지 못했어요.",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color.White)
                            .clickable { onIntent(RecommendIntent.Retry) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(text = "다시 시도", color = ChagokTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                RecommendPhase.READY -> {
                    RecommendTabs(
                        count = state.layouts.size,
                        selected = state.selectedIndex,
                        onSelect = { onIntent(RecommendIntent.SelectTab(it)) },
                    )
                    Spacer(Modifier.height(20.dp))
                    state.selected?.let { ArrangementView(layout = it, onOpenGuide = { onIntent(RecommendIntent.OpenGuide) }) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 시안: 새 계획 만들기(배치 다시 계산) / 이 배치 선택 (가이드 화면과 같은 버튼)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinePillButton(text = "새 계획 만들기", onClick = { onIntent(RecommendIntent.NewPlan) })
            Spacer(Modifier.height(12.dp))
            ChagokPrimaryButton(
                text = "이 배치 선택",
                onClick = { onIntent(RecommendIntent.Confirm) },
                enabled = state.phase == RecommendPhase.READY,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun TopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        // 시안: 차량·좌석=0, 짐=1, 결과=2
        StepIndicator(current = 2, total = LOAD_FLOW_STEPS, modifier = Modifier.align(Alignment.Center))
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(CloseButtonBg)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", color = ChagokTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun RecommendTabs(count: Int, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val isSelected = index == selected
            val shape = RoundedCornerShape(percent = 50)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) TabSelectedBg else Color.White)
                    .border(1.dp, if (isSelected) TabSelectedBg else ChagokCardBorder, shape)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "추천 ${index + 1}",
                    color = if (isSelected) Color.White else ChagokTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** 로딩·오류 안내: 차량 카드와 같은 크기의 남색 카드 */
@Composable
private fun StatusCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(AvanteTopView.CROP_W.toFloat() / AvanteTopView.CROP_H)
            .clip(CardShape)
            .background(Brush.horizontalGradient(listOf(CardBgEdge, CardBgCenter, CardBgEdge)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

/**
 * 탑뷰 이미지 위에 좌석 표시칸과 짐 사각형을 올린다.
 * - 배치 화면([guideSteps] 없음): 접는 좌석은 칸을 지우고, 짐은 싣는 순서(load_order)대로 하나씩 페이드인. 탭을 바꾸면 처음부터 다시.
 * - 가이드 화면([guideSteps] 있음): 접을 자리를 점선 칸으로 표시한다. 애니메이션 없음 (시안: 번호 배지 없음).
 */
@Composable
internal fun ArrangementView(
    layout: ArrangementLayoutUi,
    guideSteps: List<GuideStepUi>? = null,
    /** 있으면 이미지 오른쪽 아래에 "가이드 보기" 알약 버튼 */
    onOpenGuide: (() -> Unit)? = null,
) {
    val isGuide = guideSteps != null
    val image = ImageBitmap.imageResource(R.drawable.img_vehicle_avante_top_view_applied)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(AvanteTopView.CROP_W.toFloat() / AvanteTopView.CROP_H)
            .clip(CardShape)
            .background(Brush.horizontalGradient(listOf(CardBgEdge, CardBgCenter, CardBgEdge)))
            .drawBehind {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(AvanteTopView.CROP_LEFT, AvanteTopView.CROP_TOP),
                    srcSize = IntSize(AvanteTopView.CROP_W, AvanteTopView.CROP_H),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.High,
                )
            },
    ) {
        val scale = maxWidth / AvanteTopView.CROP_W.toFloat() // dp per 원본 px
        layout.seats.forEach { seat ->
            OverlayBox(
                rect = seat.rect,
                scale = scale,
                fill = if (seat.mark == SeatMark.BOARDING) BoardingFill else EmptySeatFill,
                label = if (seat.mark == SeatMark.BOARDING) "탑승" else "좌석",
            )
        }
        if (isGuide) {
            // 접을 자리: 어두운 점선 칸 (짐이 그 위에 놓이면 짐이 덮는다)
            layout.foldedGroups.forEach { group ->
                group.markRects.forEach { rect -> OverlayBox(rect = rect, scale = scale, fill = FoldedMarkFill, label = "좌석") }
            }
        }
        layout.items.forEach { item ->
            if (isGuide) {
                OverlayBox(rect = item.rect, scale = scale, fill = ItemFill, label = item.name)
            } else {
                // 탭(배치안)이 바뀌면 key가 바뀌어 애니메이션을 처음부터
                key(layout.arrangementId, item.placementId) {
                    val alpha = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(ITEM_FIRST_DELAY_MS + item.order * ITEM_STEP_MS)
                        alpha.animateTo(1f, tween(ITEM_FADE_MS))
                    }
                    OverlayBox(
                        rect = item.rect,
                        scale = scale,
                        fill = ItemFill,
                        label = item.name,
                        modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
                    )
                }
            }
        }
        onOpenGuide?.let { open ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(GuidePillBg)
                    .clickable(onClick = open)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(text = "가이드 보기", color = ChagokTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 흰 점선 테두리 사각형 + 가운데 이름 */
@Composable
private fun OverlayBox(rect: PxRect, scale: Dp, fill: Color, label: String, modifier: Modifier = Modifier) {
    val width = (scale * rect.width).coerceAtLeast(MIN_BOX)
    val height = (scale * rect.height).coerceAtLeast(MIN_BOX)
    Box(
        modifier = modifier
            .offset(x = scale * (rect.left - AvanteTopView.CROP_LEFT), y = scale * (rect.top - AvanteTopView.CROP_TOP))
            .size(width, height)
            .clip(BoxShape)
            .background(fill)
            .drawBehind {
                val inset = 0.75.dp.toPx()
                val radius = 4.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

private const val ITEM_FIRST_DELAY_MS = 250L
private const val ITEM_STEP_MS = 450L
private const val ITEM_FADE_MS = 400

/** 아주 작은 짐도 보이도록 최소 크기 */
private val MIN_BOX = 14.dp
private val FoldedMarkFill = Color(0x99202833)
private val GuidePillBg = Color(0xF2EEF1F6)

private val CardShape = RoundedCornerShape(16.dp)
private val BoxShape = RoundedCornerShape(4.dp)
private val CardBgEdge = Color(0xFF0B1A3C)
private val CardBgCenter = Color(0xFF14284F)
private val BoardingFill = Color(0xCC5D6672)
private val EmptySeatFill = Color(0xB38C9199)
private val ItemFill = Color(0xE62C5DA6)
private val TabSelectedBg = Color(0xFF05082B)
private val CloseButtonBg = Color(0xFFE3E7EF)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RecommendScreenPreview() {
    val stroller = ItemBox("p1", "유모차", 0, PxRect(340f, 1300f, 600f, 1440f))
    HyundaeSubTheme {
        RecommendScreen(
            state = RecommendUiState(
                phase = RecommendPhase.READY,
                previewId = "preview",
                layouts = List(3) { index ->
                    ArrangementLayoutUi(
                        arrangementId = "a$index",
                        seats = listOf(
                            SeatBox(SeatCode.ROW_1_RIGHT, SeatMark.BOARDING, AvanteTopView.seatMarkRect(SeatCode.ROW_1_RIGHT)),
                            SeatBox(SeatCode.ROW_2_LEFT, SeatMark.BOARDING, AvanteTopView.seatMarkRect(SeatCode.ROW_2_LEFT)),
                            SeatBox(SeatCode.ROW_2_CENTER, SeatMark.EMPTY_SEAT, AvanteTopView.seatMarkRect(SeatCode.ROW_2_CENTER)),
                            SeatBox(SeatCode.ROW_2_RIGHT, SeatMark.BOARDING, AvanteTopView.seatMarkRect(SeatCode.ROW_2_RIGHT)),
                        ),
                        items = listOf(stroller),
                    )
                },
            ),
            onIntent = {},
            onBack = {},
            onClose = {},
        )
    }
}
