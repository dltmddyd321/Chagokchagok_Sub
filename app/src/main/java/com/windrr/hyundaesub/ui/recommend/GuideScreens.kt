package com.windrr.hyundaesub.ui.recommend

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.ExitConfirmDialog
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** 기다리는 화면의 진행 단계 */
private val LOADING_STEPS = listOf("짐 크기 확인", "좌석 조건 비교", "가능한 배치 찾기")

/**
 * 가이드 문구를 기다리는 화면.
 * 1단계(짐 크기 확인)는 배치 이미지가 나온 시점이라 이미 완료.
 * 2단계(좌석 조건 비교)는 일정 시간 진행 후, 3단계(가능한 배치 찾기)는 문구가 도착할 때까지 진행 → 완료 표시 후 [onFinished].
 */
@Composable
fun GuideLoadingScreen(
    guideReady: Boolean,
    onExit: () -> Unit,
    onFinished: () -> Unit,
) {
    // 0부터: 지금 진행 중인 단계. LOADING_STEPS.size면 모두 완료
    var activeStep by remember { mutableIntStateOf(1) }
    // 들어올 때 이미 문구가 와 있으면 진행 표시를 짧게
    val compareMs = remember { if (guideReady) COMPARE_FAST_MS else COMPARE_MS }

    val ready by rememberUpdatedState(guideReady)
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(compareMs)
        activeStep = 2
        delay(FIND_MIN_MS)
        // 문구가 올 때까지 3단계 진행 중으로 둔다
        snapshotFlow { ready }.first { it }
        activeStep = LOADING_STEPS.size
        delay(DONE_HOLD_MS)
        finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        BackArrow(onClick = onExit)
        Spacer(Modifier.height(20.dp))
        Text(text = "가이드를 만들고 있어요.", color = ChagokTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(text = "서비스 이용 방법을 하나씩 정리하고 있어요.", color = ChagokTextSecondary, fontSize = 15.sp)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SparkleOrb()
            Spacer(Modifier.height(24.dp))
            Text(text = "잠시만 기다려 주세요", color = ChagokTextSecondary, fontSize = 15.sp)
            Spacer(Modifier.height(40.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LOADING_STEPS.forEachIndexed { index, label ->
                    LoadingStepRow(
                        // 시안: 진행 중인 "좌석 조건 비교"만 "~ 중"
                        label = if (index == activeStep && index == 1) "$label 중" else label,
                        state = when {
                            index < activeStep -> StepState.DONE
                            index == activeStep -> StepState.ACTIVE
                            else -> StepState.PENDING
                        },
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        ChagokPrimaryButton(text = "나가기", onClick = onExit)
        Spacer(Modifier.height(16.dp))
    }
}

private enum class StepState { DONE, ACTIVE, PENDING }

@Composable
private fun LoadingStepRow(label: String, state: StepState) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (state == StepState.ACTIVE) {
                    Modifier
                        .background(ActiveRowBg)
                        .border(1.dp, ActiveRowBorder, shape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            StepState.DONE -> Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = "완료",
                tint = Navy,
                modifier = Modifier.size(18.dp),
            )
            StepState.ACTIVE -> Box(
                Modifier
                    .padding(2.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Navy),
            )
            StepState.PENDING -> Box(
                Modifier
                    .padding(2.dp)
                    .size(14.dp)
                    .border(1.5.dp, PendingText, CircleShape),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = if (state == StepState.PENDING) PendingText else ChagokTextPrimary,
            fontSize = 15.sp,
            fontWeight = if (state == StepState.PENDING) FontWeight.Normal else FontWeight.Bold,
        )
    }
}

/** 반짝이 원 (천천히 커졌다 작아짐) */
@Composable
private fun SparkleOrb() {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "orbScale",
    )
    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(OrbTop, OrbBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = SparkleBlue,
            modifier = Modifier.size(34.dp),
        )
    }
}

/** 가이드: 배치 이미지에 단계 번호 + 단계별 안내. 화면 전체 스크롤 */
@Composable
fun GuideScreen(
    state: RecommendUiState,
    onIntent: (RecommendIntent) -> Unit,
    onClose: () -> Unit,
) {
    // 시안: 배치 화면과 같은 상단(단계 점 + ✕). ✕는 종료 확인을 거쳐 홈으로, 뒤로가기는 배치 이미지로
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
    val layout = state.selected
    val steps = (state.selectedGuide as? GuideStatus.Ready)?.steps.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        TopBar(onClose = { showExitConfirm = true })
        Spacer(Modifier.height(28.dp))
        Text(text = "이 순서대로 실어 보세요", color = ChagokTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(text = "배치가 가능해요.", color = ChagokTextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))
        RecommendTabs(
            count = state.layouts.size,
            selected = state.selectedIndex,
            onSelect = { onIntent(RecommendIntent.SelectTab(it)) },
        )
        Spacer(Modifier.height(16.dp))
        layout?.let { ArrangementView(layout = it, guideSteps = steps) }
        Spacer(Modifier.height(16.dp))
        if (steps.isNotEmpty()) GuideStepsCard(steps)
        Spacer(Modifier.height(28.dp))

        OutlinePillButton(text = "새 계획 만들기", onClick = { onIntent(RecommendIntent.NewPlan) })
        Spacer(Modifier.height(12.dp))
        ChagokPrimaryButton(text = "이 배치 선택", onClick = { onIntent(RecommendIntent.Confirm) })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GuideStepsCard(steps: List<GuideStepUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        steps.forEachIndexed { index, step ->
            if (index > 0) HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "${step.number}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(text = step.title, color = ChagokTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    step.body?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(text = it, color = ChagokTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun OutlinePillButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, ChagokCardBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = ChagokTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BackArrow(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = ChagokTextPrimary,
            )
        }
    }
}

/** 2단계(좌석 조건 비교) 표시 시간 */
private const val COMPARE_MS = 1600L
private const val COMPARE_FAST_MS = 700L

/** 3단계(가능한 배치 찾기)를 최소한 보여줄 시간 */
private const val FIND_MIN_MS = 600L

/** 모두 완료 표시 후 가이드로 넘어가기 전 */
private const val DONE_HOLD_MS = 400L

private val Navy = Color(0xFF05082B)
private val PendingText = Color(0xFF8A90A0)
private val ActiveRowBg = Color(0xFFEFF4FC)
private val ActiveRowBorder = Color(0xFFB9CDEB)
private val DividerColor = Color(0xFFEDEFF3)
private val OrbTop = Color(0xFFF2F5FB)
private val OrbBottom = Color(0xFFD3DEF8)
private val SparkleBlue = Color(0xFF1F63F2)

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "기다리는 화면")
@Composable
private fun GuideLoadingPreview() {
    HyundaeSubTheme { GuideLoadingScreen(guideReady = false, onExit = {}, onFinished = {}) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1000, name = "가이드")
@Composable
private fun GuideScreenPreview() {
    val folded = FoldedSeatGroup(listOf(SeatCode.ROW_2_LEFT))
    val stroller = ItemBox("p1", "유모차", 0, PxRect(340f, 1300f, 640f, 1440f), "TRUNK")
    val box = ItemBox("p2", "캠핑박스", 1, PxRect(660f, 1300f, 940f, 1440f), "TRUNK")
    val layout = ArrangementLayoutUi(
        arrangementId = "a1",
        seats = listOf(
            SeatBox(SeatCode.ROW_1_RIGHT, SeatMark.BOARDING, VehicleTopView.seatMarkRect(SeatCode.ROW_1_RIGHT)),
            SeatBox(SeatCode.ROW_2_CENTER, SeatMark.EMPTY_SEAT, VehicleTopView.seatMarkRect(SeatCode.ROW_2_CENTER)),
            SeatBox(SeatCode.ROW_2_RIGHT, SeatMark.BOARDING, VehicleTopView.seatMarkRect(SeatCode.ROW_2_RIGHT)),
        ),
        items = listOf(stroller, box),
        foldedGroups = listOf(folded),
    )
    val steps = listOf(
        GuideStepUi(1, "왼쪽 뒷좌석을 접어요", "오른쪽 카시트는 그대로 유지해요.", foldedGroup = folded),
        GuideStepUi(2, "유모차를 좌측으로 먼저 넣어주세요", "공간에 맞춰 눕혀주세요.", placementId = "p1"),
        GuideStepUi(3, "캠핑 박스 넣기", "남은 트렁크 공간에 놓아 주세요.", placementId = "p2"),
    )
    HyundaeSubTheme {
        GuideScreen(
            state = RecommendUiState(
                phase = RecommendPhase.READY,
                mode = RecommendMode.GUIDE,
                previewId = "p",
                layouts = listOf(layout, layout.copy(arrangementId = "a2"), layout.copy(arrangementId = "a3")),
                selectedIndex = 1,
                guides = mapOf("a2" to GuideStatus.Ready(steps)),
            ),
            onIntent = {},
            onClose = {},
        )
    }
}
