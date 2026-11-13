package com.windrr.hyundaesub.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.data.local.entity.PlanCategory
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.FlowTopBar
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 짐 등록 흐름 마지막: 계획 이름·카테고리·메모를 정하고 저장.
 * 저장하면 [onSaved]로 입력값을 넘긴다 (짐·좌석·차량과 합쳐 DB 저장은 상위 흐름 담당).
 */
@Composable
fun PlanSaveRoute(
    onBack: () -> Unit,
    onSaved: (PlanMeta) -> Unit,
    viewModel: PlanSaveViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlanSaveEffect.Saved -> onSaved(effect.meta)
            }
        }
    }
    BackHandler(onBack = onBack)

    PlanSaveScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun PlanSaveScreen(
    state: PlanSaveUiState,
    onIntent: (PlanSaveIntent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        FlowTopBar(step = 2, onBack = onBack, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "계획을 저장할까요?",
                    color = ChagokTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(text = "이름과 카테고리를 정하고 저장해요.", color = ChagokTextSecondary, fontSize = 15.sp)

                SectionLabel("계획 이름")
                InputBox(
                    value = state.name,
                    onValueChange = { onIntent(PlanSaveIntent.NameChanged(it)) },
                    placeholder = "예: 주말 나들이",
                    singleLine = true,
                    minHeight = 56,
                )

                SectionLabel("카테고리")
            }
            // 서버 카테고리 3종 (넘치는 화면에서도 잘리지 않게 가로 스크롤)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(PlanCategory.entries) { category ->
                    CategoryChip(
                        label = category.label,
                        selected = state.category == category,
                        onClick = { onIntent(PlanSaveIntent.SelectCategory(category)) },
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionLabel("메모 (선택)")
                InputBox(
                    value = state.memo,
                    onValueChange = { onIntent(PlanSaveIntent.MemoChanged(it)) },
                    placeholder = "기억해둘 내용을 남겨보세요.",
                    singleLine = false,
                    minHeight = 112,
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        ChagokPrimaryButton(
            text = "계획 저장하기",
            onClick = { onIntent(PlanSaveIntent.Save) },
            enabled = state.canSave,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(28.dp))
    Text(text = text, color = ChagokTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Int,
) {
    val textStyle = TextStyle(color = ChagokTextPrimary, fontSize = 16.sp, lineHeight = 22.sp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(ChagokNavy),
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = if (singleLine) 17.dp else 16.dp)
                    // 메모는 최소 높이만 두고 내용만큼 늘어난다
                    .heightIn(min = if (singleLine) 22.dp else (minHeight - 32).dp),
            ) {
                if (value.isEmpty()) Text(text = placeholder, style = textStyle.copy(color = PlaceholderColor, fontSize = 15.sp))
                inner()
            }
        },
    )
}

/** 선택 = 검정 채움 + 흰 글자, 미선택 = 흰 채움 */
@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.White else ChagokTextPrimary,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) SelectedChip else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
    )
}

private val SelectedChip = Color(0xFF141414)
private val PlaceholderColor = Color(0xFF8A90A0)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PlanSaveScreenPreview() {
    HyundaeSubTheme {
        PlanSaveScreen(
            state = PlanSaveUiState(name = "주말 나들이", category = PlanCategory.TRAVEL),
            onIntent = {},
            onBack = {},
        )
    }
}
