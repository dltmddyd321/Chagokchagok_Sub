package com.windrr.hyundaesub.ui.item

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.FlowTopBar
import com.windrr.hyundaesub.ui.component.ItemCard
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 짐 등록 흐름: 어떤 짐을 가져갈지.
 * - 담은 짐이 없으면: 새 물건 추가 / 등록된 물건 불러오기 카드만
 * - 담은 짐이 있으면: 짐 카드 목록(체크·수량) + 아래에 추가 카드, "짐 배치 추천받기"
 * 상태(담은 짐·마지막 선택)는 상위 MainViewModel이 들고 있다.
 */
@Composable
fun ItemSelectScreen(
    pickedItems: List<PickedItem>,
    lastSource: ItemSource?,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onLoadSaved: () -> Unit,
    onToggleItem: (key: String) -> Unit,
    onIncrease: (key: String) -> Unit,
    onDecrease: (key: String) -> Unit,
    onNext: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val hasItems = pickedItems.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        FlowTopBar(step = 1, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "어떤 짐을 가져갈까요?",
                        color = ChagokTextPrimary,
                        fontSize = 28.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "저장한 물건을 선택하거나 새로 등록해요.",
                        color = ChagokTextSecondary,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(if (hasItems) 12.dp else 32.dp))
                }
            }

            items(pickedItems, key = { it.key }) { item ->
                ItemCard(
                    name = item.name,
                    sizeLabel = item.sizeLabel,
                    checked = item.selected,
                    quantity = item.quantity,
                    onToggle = { onToggleItem(item.key) },
                    onDecrease = { onDecrease(item.key) },
                    onIncrease = { onIncrease(item.key) },
                    maxQuantity = PickedItem.MAX_QUANTITY,
                )
            }

            // 짐이 있으면 목록 아래에 작게, 없으면 시안 기본 크기로
            item {
                SourceCard(
                    text = "+ 새 물건 추가",
                    selected = !hasItems && lastSource == ItemSource.NEW,
                    height = if (hasItems) 64.dp else 108.dp,
                    onClick = onAddNew,
                )
            }
            item {
                SourceCard(
                    text = "+ 등록된 물건 불러오기",
                    selected = !hasItems && lastSource == ItemSource.SAVED,
                    height = if (hasItems) 64.dp else 80.dp,
                    onClick = onLoadSaved,
                )
            }
        }

        ChagokPrimaryButton(
            text = "짐 배치 추천받기",
            onClick = onNext,
            enabled = pickedItems.any { it.selected },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** 선택 시 네이비 테두리 + 연한 파랑 그라데이션 (차량 선택 카드와 같은 스타일) */
@Composable
private fun SourceCard(
    text: String,
    selected: Boolean,
    height: Dp,
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
            .height(height)
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

private val SelectedCardTint = Color(0xFFE6EDFB)
private val UnselectedBorder = Color(0xFFE6EAF1)

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "짐 있음")
@Composable
private fun ItemSelectScreenPreview() {
    HyundaeSubTheme {
        ItemSelectScreen(
            pickedItems = listOf(
                PickedItem("1", "유모차", 42, 58, 88),
                PickedItem("2", "캠핑박스 L", 60, 40, 35, savedItemId = "s1"),
                PickedItem("3", "캠핑박스 S", 50, 40, 40, savedItemId = "s2"),
            ),
            lastSource = ItemSource.SAVED,
            onBack = {},
            onAddNew = {},
            onLoadSaved = {},
            onToggleItem = {},
            onIncrease = {},
            onDecrease = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "빈 상태")
@Composable
private fun ItemSelectEmptyPreview() {
    HyundaeSubTheme {
        ItemSelectScreen(
            pickedItems = emptyList(),
            lastSource = ItemSource.SAVED,
            onBack = {},
            onAddNew = {},
            onLoadSaved = {},
            onToggleItem = {},
            onIncrease = {},
            onDecrease = {},
            onNext = {},
        )
    }
}
