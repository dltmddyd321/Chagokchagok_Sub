package com.windrr.hyundaesub.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.ChagokSecondaryButton
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 계획 저장 완료.
 * 뒤로가기는 저장 흐름으로 되돌아가지 않고 홈으로 보낸다 (이미 저장된 계획을 다시 저장하지 않도록).
 */
@Composable
fun PlanSavedScreen(
    onNewPlan: () -> Unit,
    onGoHome: () -> Unit,
) {
    BackHandler(onBack = onGoHome)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, ChagokBackgroundTop)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "저장 완료", color = ChagokTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.weight(1f))
        SavedCarPlaceholder()
        Spacer(Modifier.height(36.dp))
        Text(
            text = "계획을 저장했어요",
            color = ChagokTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "저장한 계획은 ‘내 계획’에서\n언제든 다시 볼 수 있어요.",
            color = ChagokTextSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.3f))

        ChagokSecondaryButton(text = "새 계획 만들기", onClick = onNewPlan)
        Spacer(Modifier.height(12.dp))
        ChagokPrimaryButton(text = "홈으로 가기", onClick = onGoHome)
        Spacer(Modifier.height(16.dp))
    }
}

/** 임시 이미지: 은색 차 + 파란 체크 배지. 일러스트 에셋 받으면 Image로 교체 */
@Composable
private fun SavedCarPlaceholder() {
    Box(modifier = Modifier.size(width = 140.dp, height = 110.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_car),
            contentDescription = null,
            tint = CarColor,
            modifier = Modifier
                .align(Alignment.Center)
                .size(130.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 2.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(BadgeTop, BadgeBottom)))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "저장됨",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private val CarColor = Color(0xFFC9CCD3)
private val BadgeTop = Color(0xFF4F8DFD)
private val BadgeBottom = Color(0xFF1F5FE0)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PlanSavedScreenPreview() {
    HyundaeSubTheme {
        PlanSavedScreen(onNewPlan = {}, onGoHome = {})
    }
}
