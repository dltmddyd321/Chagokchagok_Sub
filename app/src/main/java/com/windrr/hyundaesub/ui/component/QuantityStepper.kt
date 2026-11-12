package com.windrr.hyundaesub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary

/** 수량 − 숫자 + 조절 (최소 1) */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 40.dp,
    maxQuantity: Int = Int.MAX_VALUE,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−", DecreaseBg, buttonSize, enabled = quantity > 1, onClick = onDecrease)
        Text(
            text = quantity.toString(),
            color = ChagokTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp),
        )
        StepperButton("+", IncreaseBg, buttonSize, enabled = quantity < maxQuantity, onClick = onIncrease)
    }
}

@Composable
private fun StepperButton(
    text: String,
    background: Color,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(background)
            .border(1.dp, StepperBorder, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) ChagokTextPrimary else ChagokTextSecondary.copy(alpha = 0.4f),
            fontSize = 20.sp,
        )
    }
}

private val DecreaseBg = Color(0xFFF4F4F5)
private val IncreaseBg = Color(0xFFE8ECF4)
private val StepperBorder = Color(0xFFE4E6EB)
