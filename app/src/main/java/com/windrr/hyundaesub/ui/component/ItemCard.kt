package com.windrr.hyundaesub.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary

/**
 * 물건 카드: 썸네일·이름·크기·체크박스 + 수량. 체크 시 네이비 테두리.
 * 짐 선택 목록, "내 물건 가져오기", "꼭 가져갈 짐"에서 같이 쓴다.
 * onDecrease·onIncrease가 null이면 수량 조절 없이 "N개"만 표시한다.
 */
@Composable
fun ItemCard(
    name: String,
    sizeLabel: String,
    checked: Boolean,
    quantity: Int,
    onToggle: () -> Unit,
    onDecrease: (() -> Unit)?,
    onIncrease: (() -> Unit)?,
    modifier: Modifier = Modifier,
    maxQuantity: Int = Int.MAX_VALUE,
    /** 체크 모양: false = 사각 체크박스, true = 원형 체크 (짐 선택 화면 시안) */
    circleCheck: Boolean = false,
    /** 대표 사진: 기기 사진(file://) 또는 서버 사진 주소(팀 인증). 없거나 못 열면 기본 이미지 */
    photoUri: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, if (checked) ChagokNavy else Color.Transparent, shape)
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ItemThumbnail(photoUri)
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
            CheckBox(checked = checked, circle = circleCheck)
        }
        if (onDecrease != null && onIncrease != null) {
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
        } else {
            Text(
                text = "${quantity}개",
                color = ChagokTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** 썸네일: 대표 사진이 있으면 사진, 없거나 못 열면 기본 이미지 */
@Composable
private fun ItemThumbnail(photoUri: String?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color.White, ThumbnailTint))),
        contentAlignment = Alignment.Center,
    ) {
        val placeholder: @Composable () -> Unit = {
            Image(
                painter = painterResource(R.drawable.img_luggage_placeholder_applied),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }
        if (photoUri.isNullOrBlank()) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = photoUri,
                imageLoader = ChagokImageLoader.get(LocalContext.current),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { placeholder() } },
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { placeholder() } },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CheckBox(checked: Boolean, circle: Boolean) {
    if (checked) {
        Icon(
            painter = painterResource(if (circle) R.drawable.ic_check_circle else R.drawable.ic_checkbox_checked),
            contentDescription = "선택됨",
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
    } else {
        // 시안: 미선택은 회색 채움 사각형
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .clip(if (circle) CircleShape else RoundedCornerShape(3.dp))
                .background(UncheckedFill),
        )
    }
}

/** 물건 크기 표기: "42 × 58 × 88 cm" */
fun sizeLabelOf(widthCm: Int, depthCm: Int, heightCm: Int): String = "$widthCm × $depthCm × $heightCm cm"

private val ThumbnailTint = Color(0xFFDCE6FB)
private val ThumbnailIcon = Color(0xFFB07A3E)
private val UncheckedFill = Color(0xFFEDEEF1)
