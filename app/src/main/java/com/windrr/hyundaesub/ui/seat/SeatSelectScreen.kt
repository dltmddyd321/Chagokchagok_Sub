package com.windrr.hyundaesub.ui.seat

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.windrr.hyundaesub.ui.component.StepIndicator
import com.windrr.hyundaesub.ui.component.LOAD_FLOW_STEPS
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
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
        // 내용은 스크롤, 하단 버튼은 항상 고정 크기로 (화면이 작으면 버튼이 찌그러지던 문제)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (reselect) {
                    BackOnlyTopBar(onBack)
                } else {
                    // 시안: 상단은 단계 점만
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StepIndicator(current = 0, total = LOAD_FLOW_STEPS)
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    text = if (reselect) "탑승할 좌석을\n다시 선택해 주세요" else "탑승할 좌석을\n선택해 주세요",
                    color = ChagokTextPrimary,
                    fontSize = 26.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "카시트를 설치한 좌석도 선택해 주세요.",
                    color = ChagokTextSecondary,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(28.dp))

            CarTopView(
                selectedSeats = state.selectedSeats,
                onSeatClick = { onIntent(SeatSelectIntent.ToggleSeat(it)) },
            )

            Spacer(Modifier.height(16.dp))
            SelectedSeatSummary(
                count = state.selectedCount,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
        }

        // 하단 버튼: 처음 선택 = 이전 / 다음, 다시 선택 = 선택 완료
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!reselect) {
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrevButtonBg, contentColor = ChagokTextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(text = "이전", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            ChagokPrimaryButton(
                text = if (reselect) "선택 완료" else "다음",
                onClick = { onIntent(SeatSelectIntent.Next) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
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
 * 투싼 실내 탑뷰 이미지 위에 좌석 버튼을 올린다.
 * 이미지(976×1612px, drawable-nodpi)에서 좌석 부분만 잘라 화면 폭에 맞추고, 좌석 중심 좌표(원본 px)를 같은 비율로 옮긴다.
 * 운전석은 항상 탑승이라 버튼을 두지 않는다.
 */
@Composable
private fun CarTopView(
    selectedSeats: Set<SeatCode>,
    onSeatClick: (SeatCode) -> Unit,
) {
    val image = ImageBitmap.imageResource(R.drawable.img_vehicle_tucson_top_view)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CROP_W.toFloat() / CROP_H)
            .background(Brush.horizontalGradient(listOf(TopViewBgEdge, TopViewBgCenter, TopViewBgEdge)))
            .drawBehind {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(CROP_LEFT, CROP_TOP),
                    srcSize = IntSize(CROP_W, CROP_H),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.High,
                )
            },
    ) {
        val scale = maxWidth / CROP_W.toFloat() // dp per 원본 px
        val slotSize = scale * SLOT_PX
        SEAT_CENTERS_PX.forEach { (seat, center) ->
            SeatSlot(
                seat = seat,
                selectedSeats = selectedSeats,
                onClick = onSeatClick,
                slotSize = slotSize,
                modifier = Modifier.offset(
                    x = scale * (center.first - CROP_LEFT) - slotSize / 2,
                    y = scale * (center.second - CROP_TOP) - slotSize / 2,
                ),
            )
        }
    }
}

/** 탑뷰 이미지에서 보여줄 영역 (원본 px): 대시보드 아래 ~ 트렁크 앞 */
private const val CROP_LEFT = 40
private const val CROP_TOP = 470
private const val CROP_W = 896
private const val CROP_H = 771 // 360dp 폭에서 약 310dp (시안 비율)

/** 좌석 버튼 크기 (원본 px) */
private const val SLOT_PX = 150f

/** 좌석 쿠션 중심 (원본 px) */
private val SEAT_CENTERS_PX = listOf(
    SeatCode.ROW_1_RIGHT to (668f to 625f),
    SeatCode.ROW_2_LEFT to (305f to 990f),
    SeatCode.ROW_2_CENTER to (492f to 990f),
    SeatCode.ROW_2_RIGHT to (680f to 990f),
)

private val PrevButtonBg = Color(0xFFDDE3EE)
private val TopViewBgEdge = Color(0xFF0B1A3C)
private val TopViewBgCenter = Color(0xFF14284F)

/**
 * 선택 가능한 좌석 (시안). 두 상태 모두 흰 점선 테두리.
 * 미선택 = 반투명 회색 + "좌석", 선택 = 남색 + "탑승".
 */
@Composable
private fun SeatSlot(
    seat: SeatCode,
    selectedSeats: Set<SeatCode>,
    onClick: (SeatCode) -> Unit,
    slotSize: Dp,
    modifier: Modifier = Modifier,
) {
    val selected = seat in selectedSeats
    val fill by animateColorAsState(if (selected) SelectedFill else UnselectedFill, label = "seatFill")

    Box(
        modifier = modifier
            .size(slotSize)
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
private fun SelectedSeatSummary(count: Int, modifier: Modifier = Modifier) {
    // 시안: 선택 전에는 설명 한 줄, 선택하면 "선택한 좌석 N개" 제목이 붙는다
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = if (count > 0) 18.dp else 26.dp),
    ) {
        if (count > 0) {
            Text(
                text = "선택한 좌석 ${count}개",
                color = ChagokTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = "선택 좌석을 제외 후 보관 계획을 세워드릴게요.",
            color = ChagokTextSecondary,
            fontSize = 13.sp,
        )
    }
}

private val SeatShape = RoundedCornerShape(8.dp)
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
