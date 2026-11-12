package com.windrr.hyundaesub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.main.MainIntent
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokHeroBottom
import com.windrr.hyundaesub.ui.theme.ChagokHeroTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 플로팅 탭바(62dp + 상하 12dp 여백)에 마지막 콘텐츠가 가리지 않도록 두는 하단 여백 */
private val BottomBarClearance = 100.dp

@Composable
fun HomeScreen(onIntent: (MainIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        LogoRow()
        Spacer(Modifier.height(28.dp))
        Text(
            text = "짐 싣기 전,\n미리 확인하세요.",
            color = ChagokTextPrimary,
            fontSize = 30.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "내 차에 맞는 적재 방법을 알아보세요.",
            color = ChagokTextSecondary,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(28.dp))
        HeroCard(onStartClick = { onIntent(MainIntent.OpenMeasure) })
        Spacer(Modifier.height(16.dp))
        FrequentItemsCard(onClick = { onIntent(MainIntent.OpenFrequentItems) })
        Spacer(Modifier.height(BottomBarClearance))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun LogoRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ChagokNavy),
            contentAlignment = Alignment.Center,
        ) {
            // 임시 로고: 로고 에셋 받으면 교체
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "차곡차곡",
            color = ChagokTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** "이 짐, 내 차에 다 실릴까?" 히어로 카드. 화살표 버튼 → 짐 등록(AR 측정) */
@Composable
private fun HeroCard(onStartClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(ChagokHeroTop, ChagokHeroBottom)))
            .clickable(onClick = onStartClick),
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 28.dp, end = 24.dp)) {
            Text(
                text = "이 짐, 내 차에\n다 실릴까?",
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "짐을 등록하고 적재 배치를 확인하세요.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
            )
        }

        // 임시 이미지 자리: 차량 트렁크 사진 받으면 Image(painterResource(...))로 교체
        Icon(
            painter = painterResource(R.drawable.ic_car),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(220.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onStartClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up_right),
                contentDescription = "짐 등록 시작",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun FrequentItemsCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, ChagokCardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "자주 싣는 짐 추가하기",
            color = ChagokTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ChagokTextPrimary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF1F4FA, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    HyundaeSubTheme {
        HomeScreen(onIntent = {})
    }
}
