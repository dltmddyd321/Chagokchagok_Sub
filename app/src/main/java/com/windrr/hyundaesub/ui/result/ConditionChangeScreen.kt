package com.windrr.hyundaesub.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 배치 실패 후 바꿀 조건 (여러 개 선택 가능, 위에서부터 차례로 진행) */
enum class ConditionChange(val title: String, val description: String) {
    /** 꼭 가져갈 짐만 남기기 */
    ITEMS("가져갈 짐 조정", "꼭 가져갈 짐을 선택하고,\n다시 추천받아요."),

    /** 탑승 좌석 다시 고르기 */
    PASSENGERS("탑승 인원 변경", "탑승인원을 변경하고\n다시 추천받아요."),
}

/** 모든 짐을 싣기 어려워요: 어떤 조건을 바꿀지 고른다 */
@Composable
fun ConditionChangeScreen(
    selected: Set<ConditionChange>,
    onBack: () -> Unit,
    onToggle: (ConditionChange) -> Unit,
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
            text = "모든 짐을 싣기 어려워요",
            color = ChagokTextPrimary,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "조건을 변경해 배치를 다시 추천받아요.",
            color = ChagokTextSecondary,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(48.dp))

        ConditionChange.entries.forEach { change ->
            ConditionCard(
                change = change,
                checked = change in selected,
                onClick = { onToggle(change) },
            )
            Spacer(Modifier.height(20.dp))
        }

        Spacer(Modifier.weight(1f))
        ChagokPrimaryButton(text = "다음", onClick = onNext, enabled = selected.isNotEmpty())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConditionCard(
    change: ConditionChange,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(if (checked) 1.5.dp else 1.dp, if (checked) ChagokNavy else ChagokCardBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = change.title, color = ChagokTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Text(text = change.description, color = ChagokTextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        }
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
}

private val UncheckedBorder = Color(0xFFB8BECB)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ConditionChangeScreenPreview() {
    HyundaeSubTheme {
        ConditionChangeScreen(
            selected = ConditionChange.entries.toSet(),
            onBack = {},
            onToggle = {},
            onNext = {},
        )
    }
}
