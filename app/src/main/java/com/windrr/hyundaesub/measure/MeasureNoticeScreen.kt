package com.windrr.hyundaesub.measure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * AR 측정 앞뒤 안내 화면 (카메라 권한 요청·권한 꺼짐·AR 시작 불가).
 * 짐 등록 안내 화면과 같은 톤: 연한 하늘색 그라데이션, 왼쪽 정렬 제목, 어두운 카드, 아래 알약 버튼.
 */
@Composable
fun MeasureNoticeScreen(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    onBack: () -> Unit,
    secondaryText: String? = null,
    onSecondary: () -> Unit = onBack,
    /** 카드 아래 안내 줄 (권한 요청 화면에서 카메라 쓰임새). 비면 카드에 아이콘만 */
    points: List<String> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ChagokBackgroundTop, BackgroundBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "뒤로", tint = ChagokTextPrimary)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            color = ChagokTextPrimary,
            fontSize = 24.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            color = BodyText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardBackground)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CameraMark()
            if (points.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    points.forEach { PointRow(it) }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        secondaryText?.let {
            ChagokSecondaryButton(text = it, onClick = onSecondary)
            Spacer(Modifier.height(12.dp))
        }
        ChagokPrimaryButton(text = primaryText, onClick = onPrimary)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PointRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Highlight),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** 카드 가운데 카메라 그림 (측정 안내 카드의 하늘색 강조와 같은 색) */
@Composable
private fun CameraMark() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(Highlight.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 44.dp, height = 36.dp)) {
            val stroke = Stroke(width = 3.dp.toPx())
            val bodyTop = size.height * 0.2f
            // 위 볼록 (셔터 자리)
            drawRoundRect(
                color = Highlight,
                topLeft = Offset(size.width * 0.3f, 0f),
                size = Size(size.width * 0.4f, bodyTop + 4.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = stroke,
            )
            drawRoundRect(
                color = CardBackground,
                topLeft = Offset(0f, bodyTop),
                size = Size(size.width, size.height - bodyTop),
                cornerRadius = CornerRadius(7.dp.toPx()),
            )
            drawRoundRect(
                color = Highlight,
                topLeft = Offset(0f, bodyTop),
                size = Size(size.width, size.height - bodyTop),
                cornerRadius = CornerRadius(7.dp.toPx()),
                style = stroke,
            )
            drawCircle(Highlight, radius = size.height * 0.24f, center = Offset(size.width / 2f, bodyTop + (size.height - bodyTop) / 2f), style = stroke)
        }
    }
}

// 짐 등록 안내 화면(NewItemGuideScreen)과 같은 색
private val BackgroundBottom = Color(0xFFC9D6EA)
private val BodyText = Color(0xFF5A606B)
private val CardBackground = Color(0xFF0A1520)
private val Highlight = Color(0xFFC9DCF8)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PermissionPreview() {
    HyundaeSubTheme {
        MeasureNoticeScreen(
            title = "물건 크기를 재려면\n카메라 권한이 필요해요",
            body = "카메라로 짐을 비추면 가로·세로·높이를 바로 잴 수 있어요.",
            primaryText = "카메라 권한 허용하기",
            onPrimary = {},
            onBack = {},
            secondaryText = "다음에 할게요",
            points = listOf("카메라 화면에 선을 그어 크기를 재요", "측정하는 동안에만 카메라를 사용해요"),
        )
    }
}
