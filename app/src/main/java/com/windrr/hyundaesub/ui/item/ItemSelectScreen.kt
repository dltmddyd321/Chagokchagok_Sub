package com.windrr.hyundaesub.ui.item

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.FlowTopBar
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 짐 등록 흐름 3단계: 어떤 짐을 가져갈지.
 * 상태(담은 짐·마지막 선택)는 상위 MainViewModel이 들고 있다.
 */
@Composable
fun ItemSelectScreen(
    pickedItems: List<PickedItem>,
    lastSource: ItemSource?,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onLoadSaved: () -> Unit,
    onRemoveItem: (key: String) -> Unit,
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
        FlowTopBar(step = 1, onBack = onBack)
        Spacer(Modifier.height(40.dp))
        Text(
            text = "어떤 짐을 가져갈까요?",
            color = ChagokTextPrimary,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "저장한 짐을 새로 선택하거나 등록해요.",
            color = ChagokTextSecondary,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(48.dp))

        SourceCard(
            text = "+ 새 물건 추가",
            selected = lastSource == ItemSource.NEW,
            height = 108,
            onClick = onAddNew,
        )
        Spacer(Modifier.height(20.dp))
        SourceCard(
            text = "+ 등록된 물건 불러오기",
            selected = lastSource == ItemSource.SAVED,
            height = 80,
            onClick = onLoadSaved,
        )

        Spacer(Modifier.height(24.dp))
        // 담은 짐 목록 (없으면 아무것도 안 보임 — 시안 기본 상태)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (pickedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "담은 짐 ${pickedItems.sumOf { it.quantity }}개",
                        color = ChagokTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            items(pickedItems, key = { it.key }) { item ->
                PickedItemRow(item = item, onRemove = { onRemoveItem(item.key) })
            }
        }

        ChagokPrimaryButton(text = "다음", onClick = onNext, enabled = pickedItems.isNotEmpty())
        Spacer(Modifier.height(16.dp))
    }
}

/** 선택 시 네이비 테두리 + 연한 파랑 그라데이션 (차량 선택 카드와 같은 스타일) */
@Composable
private fun SourceCard(
    text: String,
    selected: Boolean,
    height: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (selected) {
        Brush.linearGradient(listOf(Color.White, SelectedCardTint))
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ChagokNavy else UnselectedBorder,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = ChagokTextPrimary,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PickedItemRow(item: PickedItem, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, ChagokCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, color = ChagokTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${item.sizeLabel} · ${item.quantity}개",
                color = ChagokTextSecondary,
                fontSize = 13.sp,
            )
        }
        Text(
            text = "삭제",
            color = ChagokTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRemove)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

private val SelectedCardTint = Color(0xFFE6EDFB)
private val UnselectedBorder = Color(0xFFE6EAF1)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ItemSelectScreenPreview() {
    HyundaeSubTheme {
        ItemSelectScreen(
            pickedItems = emptyList(),
            lastSource = ItemSource.SAVED,
            onBack = {},
            onAddNew = {},
            onLoadSaved = {},
            onRemoveItem = {},
            onNext = {},
        )
    }
}
