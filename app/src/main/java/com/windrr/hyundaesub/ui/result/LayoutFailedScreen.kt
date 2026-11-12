package com.windrr.hyundaesub.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.ChagokSecondaryButton
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 배치 추천 결과 없음: 현재 조건으로는 짐을 모두 실을 수 없을 때 */
@Composable
fun LayoutFailedScreen(
    onBack: () -> Unit,
    onNewPlan: () -> Unit,
    onChangeConditions: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
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
        Spacer(Modifier.height(40.dp))
        Text(
            text = "가능한 배치안이 없어요",
            color = ChagokTextPrimary,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "현재 조건으로는 짐을 모두 실을 수 없어요.",
            color = ChagokTextSecondary,
            fontSize = 15.sp,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SadCarPlaceholder()
        }

        ChagokSecondaryButton(text = "새 계획 만들기", onClick = onNewPlan)
        Spacer(Modifier.height(12.dp))
        ChagokPrimaryButton(text = "조건 변경하기", onClick = onChangeConditions)
        Spacer(Modifier.height(16.dp))
    }
}

/** 임시 이미지: 지붕에 상자를 얹고 우는 차. 일러스트 에셋 받으면 Image로 교체 */
@Composable
private fun SadCarPlaceholder() {
    Box(
        modifier = Modifier
            .size(260.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 지붕 위 상자
            Box(
                modifier = Modifier
                    .offset(y = 18.dp)
                    .size(width = 84.dp, height = 46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BoxColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(46.dp)
                        .background(TapeColor),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = CarColor,
                modifier = Modifier.size(190.dp),
            )
        }
    }
}

private val CarColor = Color(0xFF4A8BF5)
private val BoxColor = Color(0xFFF3E4CF)
private val TapeColor = Color(0xFF6FA3F7)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LayoutFailedScreenPreview() {
    HyundaeSubTheme {
        LayoutFailedScreen(onBack = {}, onNewPlan = {}, onChangeConditions = {})
    }
}
