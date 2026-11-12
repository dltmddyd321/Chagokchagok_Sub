package com.windrr.hyundaesub.ui.item.saved

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
                        SavedItemCard(
                            item = item,
                            selected = item.id in state.selectedIds,
                            quantity = state.quantityOf(item.id),
                            onIntent = onIntent,
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

/** 물건 카드: 썸네일·이름·크기·체크 + 수량 조절. 체크 시 네이비 테두리 */
@Composable
private fun SavedItemCard(
    item: SavedItem,
    selected: Boolean,
    quantity: Int,
    onIntent: (SavedItemsIntent) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ChagokNavy else ChagokCardBorder, shape)
            .clickable { onIntent(SavedItemsIntent.ToggleItem(item.id)) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ItemThumbnail()
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                Text(text = item.name, color = ChagokTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(text = item.sizeLabel, color = ChagokTextSecondary, fontSize = 13.sp)
            }
            CheckBox(checked = selected)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "수량", color = ChagokTextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            QuantityStepper(
                quantity = quantity,
                onDecrease = { onIntent(SavedItemsIntent.Decrease(item.id)) },
                onIncrease = { onIntent(SavedItemsIntent.Increase(item.id)) },
            )
        }
    }
}

/** 임시 썸네일: 물건 사진 받으면 Image로 교체 */
@Composable
private fun ItemThumbnail() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color.White, ThumbnailTint))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_luggage),
            contentDescription = null,
            tint = ThumbnailIcon,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun CheckBox(checked: Boolean) {
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

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(text = "−", background = DecreaseBg, enabled = quantity > 1, onClick = onDecrease)
        Text(
            text = quantity.toString(),
            color = ChagokTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(50.dp),
        )
        StepperButton(text = "+", background = IncreaseBg, enabled = true, onClick = onIncrease)
    }
}

@Composable
private fun StepperButton(
    text: String,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
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

@Composable
private fun EmptyMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_luggage),
            contentDescription = null,
            tint = ChagokCardBorder,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(text = title, color = ChagokTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = body, color = ChagokTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
    }
}

private val Placeholder = Color(0xFF8A90A0)
private val ThumbnailTint = Color(0xFFDCE6FB)
private val ThumbnailIcon = Color(0xFFB07A3E)
private val UncheckedBorder = Color(0xFFB8BECB)
private val DecreaseBg = Color(0xFFF4F4F5)
private val IncreaseBg = Color(0xFFE8ECF4)
private val StepperBorder = Color(0xFFE4E6EB)

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
