package com.windrr.hyundaesub.ui.item

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.windrr.hyundaesub.ui.component.ExitConfirmDialog
import com.windrr.hyundaesub.ui.component.ItemCard
import com.windrr.hyundaesub.ui.component.LOAD_FLOW_STEPS
import com.windrr.hyundaesub.ui.component.StepIndicator
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 짐 등록 흐름: 어떤 짐을 가져갈까요? (시안)
 * - 상단: 단계 점 + 오른쪽 닫기(✕, 흐름 종료)
 * - "새 물건 추가"(남색) / "내 물건 불러오기"(흰색) 두 버튼
 * - 담은 짐 카드 목록 (원형 체크·수량)
 * - 하단: 이전 / 다음 (체크한 짐이 있어야 다음)
 * 상태(담은 짐)는 상위 MainViewModel이 들고 있다.
 */
@Composable
fun ItemSelectScreen(
    pickedItems: List<PickedItem>,
    @Suppress("UNUSED_PARAMETER") lastSource: ItemSource?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onAddNew: () -> Unit,
    onLoadSaved: () -> Unit,
    onToggleItem: (key: String) -> Unit,
    onIncrease: (key: String) -> Unit,
    onDecrease: (key: String) -> Unit,
    onNext: () -> Unit,
) {
    BackHandler(onBack = onBack)
    // ✕는 홈까지 나가 버리므로 한 번 확인받는다
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    if (showExitConfirm) {
        ExitConfirmDialog(
            onContinue = { showExitConfirm = false },
            onExit = {
                showExitConfirm = false
                onClose()
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // 상단: 가운데 단계 점, 오른쪽 닫기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            StepIndicator(current = 1, total = LOAD_FLOW_STEPS, modifier = Modifier.align(Alignment.Center))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CloseButtonBg)
                    .clickable { showExitConfirm = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✕", color = ChagokTextSecondary, fontSize = 14.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "어떤 짐을 가져갈까요?",
                        color = ChagokTextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "저장한 물건을 선택하거나 새로 등록해요.",
                        color = ChagokTextSecondary,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SourceButton(
                            text = "새 물건 추가",
                            filled = true,
                            onClick = onAddNew,
                            modifier = Modifier.weight(1f),
                        )
                        SourceButton(
                            text = "내 물건 불러오기",
                            filled = false,
                            onClick = onLoadSaved,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                    circleCheck = true,
                )
            }
        }

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
            ChagokPrimaryButton(
                text = "다음",
                onClick = onNext,
                enabled = pickedItems.any { it.selected },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** 새 물건 추가 = 남색 그라데이션 채움, 내 물건 불러오기 = 흰 바탕 + 테두리 */
@Composable
private fun SourceButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(Brush.verticalGradient(listOf(FilledTop, FilledBottom)))
                } else {
                    Modifier
                        .background(Color.White)
                        .border(1.dp, ChagokCardBorder, shape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) Color.White else ChagokTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val FilledTop = Color(0xFF2C4A6E)
private val FilledBottom = Color(0xFF1B3352)
private val CloseButtonBg = Color(0xFFE3E7EF)
private val PrevButtonBg = Color(0xFFDDE3EE)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ItemSelectScreenPreview() {
    HyundaeSubTheme {
        ItemSelectScreen(
            pickedItems = listOf(
                PickedItem("1", "유모차", 42, 58, 88),
                PickedItem("2", "캠핑박스 L", 60, 40, 35, savedItemId = "s1"),
                PickedItem("3", "캠핑박스 S", 50, 40, 40, savedItemId = "s2"),
            ),
            lastSource = ItemSource.NEW,
            onBack = {},
            onClose = {},
            onAddNew = {},
            onLoadSaved = {},
            onToggleItem = {},
            onIncrease = {},
            onDecrease = {},
            onNext = {},
        )
    }
}
