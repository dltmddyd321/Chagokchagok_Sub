package com.windrr.hyundaesub.ui.vehicle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.data.local.entity.VehicleEntity
import com.windrr.hyundaesub.ui.component.StepIndicator
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 짐 등록 흐름 1단계: 어느 차에 실을지 고른다 */
@Composable
fun VehicleSelectRoute(
    onBack: () -> Unit,
    onNext: (vehicleId: String) -> Unit,
    viewModel: VehicleSelectViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VehicleSelectEffect.GoNext -> onNext(effect.vehicleId)
            }
        }
    }
    BackHandler(onBack = onBack)

    VehicleSelectScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun VehicleSelectScreen(
    state: VehicleSelectUiState,
    onIntent: (VehicleSelectIntent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // 상단: 뒤로가기 + 단계 표시 (1/3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 0.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "뒤로",
                    tint = ChagokTextPrimary,
                )
            }
            StepIndicator(current = 0, total = 3, modifier = Modifier.align(Alignment.Center))
        }

        Spacer(Modifier.height(40.dp))
        Text(
            text = "어느 차에\n짐을 실을까요?",
            color = ChagokTextPrimary,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(32.dp))

        MyCarCard(
            car = state.myCar,
            selected = state.choice == VehicleChoice.MY_CAR,
            onClick = { onIntent(VehicleSelectIntent.SelectMyCar) },
        )
        Spacer(Modifier.height(20.dp))
        OtherCarCard(
            selected = state.choice == VehicleChoice.OTHER,
            onClick = { onIntent(VehicleSelectIntent.SelectOther) },
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onIntent(VehicleSelectIntent.Next) },
            enabled = state.canProceed,
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = NextButtonColor,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(text = "다음", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MyCarCard(
    car: VehicleEntity?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableCard(selected = selected, onClick = onClick) {
        Column {
            Text(text = "선택한 차량", color = ChagokTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(text = "내 차", color = ChagokTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = car?.name.orEmpty(),
                color = ChagokTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(32.dp))
            Text(text = car?.desc.orEmpty(), color = ChagokTextPrimary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OtherCarCard(
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableCard(selected = selected, onClick = onClick, showCheckbox = false) {
        Column {
            Text(
                text = "다른 차량 선택",
                color = ChagokTextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(text = "차종과 모델로 찾아보세요.", color = ChagokTextSecondary, fontSize = 14.sp)
        }
    }
}

/** 선택 시 네이비 테두리 + 연한 파랑 그라데이션, 미선택 시 흰 카드 */
@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    showCheckbox: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (selected) {
        Brush.linearGradient(listOf(Color.White, SelectedCardTint))
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier.border(2.dp, ChagokNavy, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        content()
        if (showCheckbox && selected) {
            Icon(
                painter = painterResource(R.drawable.ic_checkbox_checked),
                contentDescription = "선택됨",
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
            )
        }
    }
}

private val NextButtonColor = Color(0xFF05082B)
private val SelectedCardTint = Color(0xFFE6EDFB)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun VehicleSelectScreenPreview() {
    HyundaeSubTheme {
        VehicleSelectScreen(
            state = VehicleSelectUiState(
                myCar = VehicleEntity(
                    vehicleId = "avante_cn8",
                    isMain = true,
                    name = "더 뉴 아반떼 하이브리드",
                    desc = "5인승 SUV",
                ),
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
