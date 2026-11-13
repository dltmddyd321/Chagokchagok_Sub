package com.windrr.hyundaesub.ui.seat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import com.windrr.hyundaesub.R
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.FlowTopBar
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokHeroTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 짐 등록 흐름 2단계: 탑승·카시트 좌석 선택 */
@Composable
fun SeatSelectRoute(
    onBack: () -> Unit,
    onNext: (occupiedSeats: Set<SeatCode>) -> Unit,
    viewModel: SeatSelectViewModel = viewModel(),
    /** true = 배치 실패 후 "탑승 인원 변경" (단계 점 없음, 문구·버튼 다름) */
    reselect: Boolean = false,
    initialSeats: Set<SeatCode> = emptySet(),
) {
    LaunchedEffect(viewModel) { viewModel.start(initialSeats) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SeatSelectEffect.GoNext -> onNext(effect.occupiedSeats)
            }
        }
    }
    BackHandler(onBack = onBack)

    SeatSelectScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack, reselect = reselect)
}

@Composable
fun SeatSelectScreen(
    state: SeatSelectUiState,
    onIntent: (SeatSelectIntent) -> Unit,
    onBack: () -> Unit,
    reselect: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (reselect) BackOnlyTopBar(onBack) else FlowTopBar(step = 0, onBack = onBack)
            Spacer(Modifier.height(40.dp))
            Text(
                text = if (reselect) "탑승할 좌석을\n다시 선택해 주세요" else "탑승할 좌석을\n선택해 주세요",
                color = ChagokTextPrimary,
                fontSize = 28.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "카시트를 설치한 좌석도 선택해 주세요.",
                color = ChagokTextSecondary,
                fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(24.dp))

        CarTopView(
            selectedSeats = state.selectedSeats,
            onSeatClick = { onIntent(SeatSelectIntent.ToggleSeat(it)) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            SelectedSeatSummary(count = state.selectedCount)
            Spacer(Modifier.weight(1f))
            ChagokPrimaryButton(
                text = if (reselect) "선택 완료" else "다음",
                onClick = { onIntent(SeatSelectIntent.Next) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BackOnlyTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = ChagokTextPrimary,
            )
        }
    }
}

/**
 * 임시 차량 실내 평면도. 차량 이미지 에셋 받으면 배경을 Image로 바꾸고
 * 좌석 슬롯은 같은 위치에 겹쳐 올린다.
 */
@Composable
private fun CarTopView(
    selectedSeats: Set<SeatCode>,
    onSeatClick: (SeatCode) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(Brush.verticalGradient(listOf(ChagokHeroTop, ChagokNavy))),
        contentAlignment = Alignment.Center,
    ) {
        // 차체 자리
        Column(
            modifier = Modifier
                .width(260.dp)
                .height(330.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(CarBody)
                .padding(horizontal = 18.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            // 1열: 운전석(고정) · 콘솔 · 조수석
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DriverSeat()
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SeatColor),
                )
                SeatSlot(SeatCode.ROW_1_RIGHT, selectedSeats, onSeatClick)
            }
            // 2열
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SeatSlot(SeatCode.ROW_2_LEFT, selectedSeats, onSeatClick)
                SeatSlot(SeatCode.ROW_2_CENTER, selectedSeats, onSeatClick)
                SeatSlot(SeatCode.ROW_2_RIGHT, selectedSeats, onSeatClick)
            }
        }
    }
}

@Composable
private fun DriverSeat() {
    Box(
        modifier = Modifier
            .size(SeatSize)
            .clip(SeatShape)
            .background(SeatColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "운전석", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

/**
 * 선택 가능한 좌석 (시안). 두 상태 모두 흰 점선 테두리.
 * 미선택 = 반투명 회색 + "좌석", 선택 = 남색 + "탑승".
 */
@Composable
private fun SeatSlot(
    seat: SeatCode,
    selectedSeats: Set<SeatCode>,
    onClick: (SeatCode) -> Unit,
) {
    val selected = seat in selectedSeats
    val fill by animateColorAsState(if (selected) SelectedFill else UnselectedFill, label = "seatFill")

    Box(
        modifier = Modifier
            .size(SeatSize)
            .clip(SeatShape)
            .background(fill)
            .drawBehind {
                val radius = 8.dp.toPx()
                val inset = 0.75.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                    ),
                )
            }
            .clickable { onClick(seat) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (selected) "탑승" else "좌석",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SelectedSeatSummary(count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "선택한 좌석 ${count}개",
            color = ChagokTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "선택 좌석을 제외 후 보관 계획을 세워드릴게요.",
            color = ChagokTextSecondary,
            fontSize = 13.sp,
        )
    }
}

private val SeatSize = 60.dp
private val SeatShape = RoundedCornerShape(8.dp)
private val CarBody = Color(0xFF2B2E35)
private val SeatColor = Color(0xFF4A4E57)
/** 선택 좌석: 시안의 남색 (반투명이라 이미지 에셋 위에서도 좌석이 비친다) */
private val SelectedFill = Color(0xE61D4A86)

/** 미선택 좌석: 반투명 회색 */
private val UnselectedFill = Color(0xB3707378)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SeatSelectScreenPreview() {
    HyundaeSubTheme {
        SeatSelectScreen(
            state = SeatSelectUiState(selectedSeats = setOf(SeatCode.ROW_1_RIGHT, SeatCode.ROW_2_LEFT)),
            onIntent = {},
            onBack = {},
        )
    }
}
