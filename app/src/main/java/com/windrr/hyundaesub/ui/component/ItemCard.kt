package com.windrr.hyundaesub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary

/**
 * 물건 카드: 썸네일·이름·크기·체크박스 + 수량 조절. 체크 시 네이비 테두리.
 * 짐 선택 목록과 "내 물건 가져오기"에서 같이 쓴다.
 */
@Composable
fun ItemCard(
    name: String,
    sizeLabel: String,
    checked: Boolean,
    quantity: Int,
    onToggle: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    maxQuantity: Int = Int.MAX_VALUE,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(if (checked) 1.5.dp else 1.dp, if (checked) ChagokNavy else ChagokCardBorder, shape)
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ItemThumbnail()
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = name,
                    color = ChagokTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(text = sizeLabel, color = ChagokTextSecondary, fontSize = 13.sp)
            }
            CheckBox(checked = checked)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "수량", color = ChagokTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            QuantityStepper(
                quantity = quantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                maxQuantity = maxQuantity,
            )
        }
    }
}

/** 임시 썸네일: 물건 사진 받으면 Image로 교체 */
@Composable
private fun ItemThumbnail() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color.White, ThumbnailTint))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_luggage),
            contentDescription = null,
            tint = ThumbnailIcon,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
    if (checked) {
        Icon(
            painter = painterResource(R.drawable.ic_checkbox_checked),
            contentDescription = "선택됨",
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(3.dp))
                .border(1.5.dp, UncheckedBorder, RoundedCornerShape(3.dp)),
        )
    }
}

/** 물건 크기 표기: "42 × 58 × 88 cm" */
fun sizeLabelOf(widthCm: Int, depthCm: Int, heightCm: Int): String = "$widthCm × $depthCm × $heightCm cm"

private val ThumbnailTint = Color(0xFFDCE6FB)
private val ThumbnailIcon = Color(0xFFB07A3E)
private val UncheckedBorder = Color(0xFFB8BECB)
