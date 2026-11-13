package com.windrr.hyundaesub.ui.item.shape

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.measure.MeasureShape
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

private val MeasureShape.title: String
    get() = when (this) {
        MeasureShape.BOX -> "네모난 물건"
        MeasureShape.CYLINDER -> "둥근 물건"
    }

private val MeasureShape.description: String
    get() = when (this) {
        MeasureShape.BOX -> "상자·캐리어처럼 각진 물건"
        MeasureShape.CYLINDER -> "원통·둥근 받침이 있는 물건"
    }

/** AR 측정 전: 바닥 모양(네모 / 둥근)에 따라 측정 방법이 달라진다 */
@Composable
fun ShapeSelectScreen(
    selected: MeasureShape,
    onSelect: (MeasureShape) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
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
            text = "물건의 바닥 모양을\n선택해 주세요",
            color = ChagokTextPrimary,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = "모양에 따라 측정 방법이 달라져요.", color = ChagokTextSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(32.dp))

        MeasureShape.entries.forEach { shape ->
            ShapeCard(shape = shape, selected = shape == selected, onClick = { onSelect(shape) })
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            ChagokPrimaryButton(text = "다음", onClick = onNext, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ShapeCard(shape: MeasureShape, selected: Boolean, onClick: () -> Unit) {
    val cardShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(cardShape)
            .background(Color.White)
            .border(1.5.dp, if (selected) ChagokNavy else Color.Transparent, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = shape.title, color = ChagokTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(text = shape.description, color = ChagokTextSecondary, fontSize = 12.sp)
        }
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = "선택됨",
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private val PrevButtonBg = Color(0xFFDDE3EE)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ShapeSelectScreenPreview() {
    HyundaeSubTheme {
        ShapeSelectScreen(selected = MeasureShape.BOX, onSelect = {}, onBack = {}, onNext = {})
    }
}
