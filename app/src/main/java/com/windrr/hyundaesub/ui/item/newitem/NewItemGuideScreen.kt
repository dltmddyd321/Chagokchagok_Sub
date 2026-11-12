package com.windrr.hyundaesub.ui.item.newitem

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.ChagokSecondaryButton
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 짐 선택 > 새 물건 추가: 측정 방법 안내 (AR 측정 / 직접 입력) */
@Composable
fun NewItemGuideScreen(
    onBack: () -> Unit,
    onManualInput: () -> Unit,
    onStartAr: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ChagokBackgroundTop, GuideBackgroundBottom)))
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
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "뒤로",
                    tint = ChagokTextPrimary,
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = "화면처럼 카메라에 짐 전체가\n보이도록 측정해주세요.",
            color = GuideText,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(48.dp))

        ArGuideCard()

        Spacer(Modifier.weight(1f))
        ChagokSecondaryButton(text = "직접 입력하기", onClick = onManualInput)
        Spacer(Modifier.height(12.dp))
        ChagokPrimaryButton(text = "AR 측정하기", onClick = onStartAr)
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 측정 예시 카드. 시안(카드 656×712px) 좌표를 그대로 비율로 옮겨 그린다.
 * 위쪽 세 점(가로·세로 모서리)을 찍는 방식을 보여준다.
 */
@Composable
private fun ArGuideCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(DESIGN_W / DESIGN_H)
            .clip(RoundedCornerShape(28.dp))
            .background(CardBackground),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGuide()
        }
        Text(
            text = "AR 측정",
            color = ChipText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White)
                .padding(horizontal = 22.dp, vertical = 8.dp),
        )
        Text(
            text = "물건을 선에 맞춰주세요.",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }
}

private const val DESIGN_W = 656f
private const val DESIGN_H = 712f

private fun DrawScope.drawGuide() {
    val sx = size.width / DESIGN_W
    val sy = size.height / DESIGN_H
    fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
    val unit = size.width / DESIGN_W // 시안 1px

    // 바닥 기준선
    drawPolyline(listOf(p(31f, 507f), p(328f, 356f), p(621f, 507f)), GroundLine, 2f * unit)
    drawPolyline(listOf(p(83f, 615f), p(378f, 452f), p(615f, 566f)), GroundLine, 2f * unit)

    // 상자 윤곽 (회색)
    val a = p(118f, 264f)
    val b = p(409f, 184f)
    val c = p(548f, 298f)
    val d = p(256f, 380f)
    val aBottom = p(118f, 460f)
    val dBottom = p(256f, 571f)
    val cBottom = p(548f, 490f)
    val boxStroke = 3f * unit
    drawPolyline(listOf(a, d, c), BoxEdge, boxStroke)
    drawPolyline(listOf(a, aBottom, dBottom, cBottom, c), BoxEdge, boxStroke)
    drawLine(BoxEdge, d, dBottom, boxStroke)

    // 측정할 윗면 두 모서리 (강조) + 찍는 점
    drawPolyline(listOf(a, b, c), Highlight, 6f * unit)
    listOf(a, b, c).forEach {
        drawCircle(PointRing, radius = 11f * unit, center = it)
        drawCircle(Color.White, radius = 7f * unit, center = it)
    }

    // 모서리 브래킷
    val arm = 58f
    val bracket = 4f * unit
    val left = 60f
    val right = 598f
    val top = 130f
    val bottom = 635f
    drawPolyline(listOf(p(left, top + 34f), p(left, top), p(left + arm, top)), Color.White, bracket)
    drawPolyline(listOf(p(right - arm, top), p(right, top), p(right, top + 34f)), Color.White, bracket)
    drawPolyline(listOf(p(left, bottom - 38f), p(left, bottom), p(left + arm, bottom)), Color.White, bracket)
    drawPolyline(listOf(p(right - arm, bottom), p(right, bottom), p(right, bottom - 38f)), Color.White, bracket)
}

private fun DrawScope.drawPolyline(points: List<Offset>, color: Color, width: Float) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Square, join = StrokeJoin.Miter))
}

private val GuideBackgroundBottom = Color(0xFFC9D6EA)
private val GuideText = Color(0xFF5A606B)
private val CardBackground = Color(0xFF0A1520)
private val ChipText = Color(0xFF5A606B)
private val GroundLine = Color(0xFF3A4452)
private val BoxEdge = Color(0xFF6E7176)
private val Highlight = Color(0xFFC9DCF8)
private val PointRing = Color(0xFFB9D0F5)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NewItemGuideScreenPreview() {
    HyundaeSubTheme {
        NewItemGuideScreen(onBack = {}, onManualInput = {}, onStartAr = {})
    }
}
