package com.windrr.hyundaesub.ui.item.saved

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.FlowTopBar
import com.windrr.hyundaesub.ui.component.ItemCard
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokCardBorder
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/** 짐 선택 > "등록된 물건 불러오기": 저장해 둔 물건을 골라 수량과 함께 담는다 */
@Composable
fun SavedItemsRoute(
    onBack: () -> Unit,
    onDone: (List<PickedItem>) -> Unit,
    viewModel: SavedItemsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SavedItemsEffect.Done -> onDone(effect.items)
            }
        }
    }
    BackHandler(onBack = onBack)

    SavedItemsScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun SavedItemsScreen(
    state: SavedItemsUiState,
    onIntent: (SavedItemsIntent) -> Unit,
    onBack: () -> Unit,
) {
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
            text = "내 물건 가져오기",
            color = ChagokTextPrimary,
            fontSize = 28.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(24.dp))
        SearchField(query = state.query, onQueryChange = { onIntent(SavedItemsIntent.QueryChanged(it)) })
        Spacer(Modifier.height(28.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    color = ChagokNavy,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.isEmpty -> EmptyMessage(
                    title = "아직 저장한 물건이 없어요",
                    body = "자주 싣는 짐을 저장해 두면\n여기서 바로 불러올 수 있어요.",
                )
                state.isSearchEmpty -> EmptyMessage(
                    title = "'${state.query.trim()}' 물건이 없어요",
                    body = "다른 이름으로 검색해 보세요.",
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                ) {
                    items(state.visibleItems, key = { it.id }) { item ->
                        ItemCard(
                            name = item.name,
                            sizeLabel = item.sizeLabel,
                            checked = item.id in state.selectedIds,
                            quantity = state.quantityOf(item.id),
                            onToggle = { onIntent(SavedItemsIntent.ToggleItem(item.id)) },
                            onDecrease = { onIntent(SavedItemsIntent.Decrease(item.id)) },
                            onIncrease = { onIntent(SavedItemsIntent.Increase(item.id)) },
                        )
                    }
                }
            }
        }

        ChagokPrimaryButton(
            text = "저장하기",
            onClick = { onIntent(SavedItemsIntent.Save) },
            enabled = state.canSave,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, ChagokCardBorder, shape)
            .padding(start = 16.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(text = "이름으로 저장한 물건을 검색해보세요.", color = Placeholder, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = ChagokTextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(ChagokNavy),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "검색",
            tint = ChagokTextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EmptyMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_luggage_placeholder_applied),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(text = title, color = ChagokTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = body, color = ChagokTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

private val Placeholder = Color(0xFF8A90A0)

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "목록")
@Composable
private fun SavedItemsScreenPreview() {
    HyundaeSubTheme {
        SavedItemsScreen(
            state = SavedItemsUiState(
                isLoading = false,
                allItems = listOf(
                    SavedItem("1", "캠핑박스 L", 60, 40, 35),
                    SavedItem("2", "캠핑박스 S", 50, 40, 40),
                ),
                selectedIds = setOf("1", "2"),
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "빈 상태")
@Composable
private fun SavedItemsEmptyPreview() {
    HyundaeSubTheme {
        SavedItemsScreen(state = SavedItemsUiState(isLoading = false), onIntent = {}, onBack = {})
    }
}
