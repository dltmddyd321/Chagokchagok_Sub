package com.windrr.hyundaesub.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.ItemCard
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 배치 실패 > 가져갈 짐 조정: 배치 추천에 넣었던 짐 중 꼭 가져갈 것만 체크.
 * 체크 안 한 짐은 다시 추천할 때 공간이 모자라면 빠질 수 있다.
 */
@Composable
fun RequiredItemsScreen(
    items: List<PickedItem>,
    onBack: () -> Unit,
    onToggle: (key: String) -> Unit,
    onDone: () -> Unit,
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "꼭 가져가야 하는 물건을\n체크해주세요",
                        color = ChagokTextPrimary,
                        fontSize = 28.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "기존에 선택한 짐과 수량 중에서 조정해주세요.",
                        color = ChagokTextSecondary,
                        fontSize = 15.sp,
                    )
                }
            }
            items(items, key = { it.key }) { item ->
                ItemCard(
                    name = item.name,
                    sizeLabel = item.sizeLabel,
                    checked = item.required,
                    quantity = item.quantity,
                    onToggle = { onToggle(item.key) },
                    onDecrease = null,
                    onIncrease = null,
                )
            }
        }

        ChagokPrimaryButton(text = "선택 완료", onClick = onDone)
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequiredItemsScreenPreview() {
    HyundaeSubTheme {
        RequiredItemsScreen(
            items = listOf(
                PickedItem("1", "유모차", 42, 58, 88, required = true),
                PickedItem("2", "캠핑박스 L", 60, 40, 35, required = true),
                PickedItem("3", "캠핑박스 S", 50, 40, 40),
            ),
            onBack = {},
            onToggle = {},
            onDone = {},
        )
    }
}
