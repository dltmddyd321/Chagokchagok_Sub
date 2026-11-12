package com.windrr.hyundaesub.ui.main

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.ui.theme.ChagokTabSelected
import com.windrr.hyundaesub.ui.theme.ChagokTabSelectedBg
import com.windrr.hyundaesub.ui.theme.ChagokTabUnselected
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private val BarHeight = 62.dp
private val BarShape = RoundedCornerShape(percent = 50)
private val TabIconSize = 28.dp
private val TabHighlightHeight = 50.dp
private val TabHighlightShape = RoundedCornerShape(percent = 50)

private val TabLabelStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 13.sp,
    letterSpacing = 0.sp,
)

/**
 * 글래스모피즘 플로팅 하단 탭바 (BoatLab BoatFloatingBottomBar 이식).
 * - NavController·FAB 제거, 선택 상태는 상위(MVI state)에서 받는다.
 * - Scaffold bottomBar가 아니라 콘텐츠 위 오버레이로 얹어야 뒤 콘텐츠가 비쳐 보인다.
 * - 콘텐츠 쪽에 Modifier.hazeSource(hazeState)가 걸려 있어야 블러가 적용된다.
 */
@Composable
fun ChagokBottomBar(
    hazeState: HazeState,
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 흰색 틴트 알파를 낮춰 뒤 콘텐츠가 비치게, noise 0. API 31 미만은 흰 반투명 폴백.
    val glassStyle = HazeStyle(
        backgroundColor = Color.White,
        tint = HazeTint(Color.White.copy(alpha = 0.12f)),
        blurRadius = 24.dp,
        noiseFactor = 0f,
        fallbackTint = HazeTint(Color.White.copy(alpha = 0.72f)),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(BarHeight)
            .md3SoftShadow(BarShape)
            .clip(BarShape)
            .hazeEffect(state = hazeState, style = glassStyle)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MainTab.entries.forEach { tab ->
            FloatingTabItem(
                tab = tab,
                selected = tab == selectedTab,
                onClick = { onTabClick(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 탭 1개 — 선택 시 연한 파랑 하이라이트가 아이콘+라벨을 감싼다. */
@Composable
private fun FloatingTabItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) ChagokTabSelected else ChagokTabUnselected
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(interactionSource = noRipple, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(TabHighlightHeight)
                .clip(TabHighlightShape)
                .then(if (selected) Modifier.background(ChagokTabSelectedBg) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 임시 아이콘은 선택/미선택 동일 에셋 + 틴트. 디자인 에셋(fill/outline) 받으면 분리.
            Icon(
                painter = painterResource(tab.iconRes),
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier
                    .size(TabIconSize)
                    .padding(3.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(text = tab.label, style = TabLabelStyle, color = contentColor)
        }
    }
}

/** MD3 느낌의 부드러운 2겹 그림자. API 28 미만은 기본 elevation 그림자. */
private fun Modifier.md3SoftShadow(shape: Shape): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        this
            .roundedDropShadow(offsetY = 3.dp, blurRadius = 15.dp, alpha = 0.10f)
            .roundedDropShadow(offsetY = 1.dp, blurRadius = 7.dp, alpha = 0.10f)
    } else {
        this.shadow(elevation = 6.dp, shape = shape)
    }

/** 스타디움 도형용 드롭 섀도우 1겹 (코너 반경 = 높이 / 2). */
private fun Modifier.roundedDropShadow(
    offsetY: Dp,
    blurRadius: Dp,
    alpha: Float,
): Modifier = drawBehind {
    val radius = size.height / 2f
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                blurRadius.toPx(),
                0f,
                offsetY.toPx(),
                Color.Black.copy(alpha = alpha).toArgb(),
            )
        }
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
    }
}
