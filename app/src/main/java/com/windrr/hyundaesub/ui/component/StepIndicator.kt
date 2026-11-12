package com.windrr.hyundaesub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.windrr.hyundaesub.ui.theme.ChagokNavy

/** 짐 등록 흐름 상단의 단계 점 표시 (현재 단계만 진하게) */
@Composable
fun StepIndicator(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) ChagokNavy else StepInactive),
            )
        }
    }
}

private val StepInactive = Color(0xFFD7DBE3)
