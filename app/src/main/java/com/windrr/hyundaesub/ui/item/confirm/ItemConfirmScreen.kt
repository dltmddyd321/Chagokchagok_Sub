package com.windrr.hyundaesub.ui.item.confirm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.ui.component.ChagokPrimaryButton
import com.windrr.hyundaesub.ui.component.QuantityStepper
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokNavy
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * AR 촬영 후 측정값 확인 (measured != null) / 직접 입력 (measured == null).
 * 이름·치수는 고칠 수 있고, 저장하면 짐 목록에 담긴다.
 */
@Composable
fun ItemConfirmRoute(
    measured: BoxSize?,
    onBack: () -> Unit,
    onSaved: (PickedItem) -> Unit,
    viewModel: ItemConfirmViewModel = viewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.start(measured) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ItemConfirmEffect.Saved -> onSaved(effect.item)
            }
        }
    }
    BackHandler(onBack = onBack)

    ItemConfirmScreen(state = state, onIntent = viewModel::onIntent, onBack = onBack)
}

@Composable
fun ItemConfirmScreen(
    state: ItemConfirmUiState,
    onIntent: (ItemConfirmIntent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = if (state.fromMeasure) "입력된 수치를 확인해 주세요." else "짐 정보를 입력해 주세요.",
                color = GuideText,
                fontSize = 19.sp,
            )
            Spacer(Modifier.height(32.dp))

            WhiteCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "짐 이름", color = ChagokTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                InputField(
                    value = state.name,
                    onValueChange = { onIntent(ItemConfirmIntent.NameChanged(it)) },
                    placeholder = "예: 유모차",
                    textStyle = TextStyle(color = ChagokTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
                    keyboardType = KeyboardType.Text,
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(text = "짐 크기", color = ChagokTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DimensionCard("가로", state.width, Dimension.WIDTH, onIntent, Modifier.weight(1f))
                DimensionCard("세로", state.depth, Dimension.DEPTH, onIntent, Modifier.weight(1f))
                DimensionCard("높이", state.height, Dimension.HEIGHT, onIntent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            WhiteCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "수량",
                        color = ChagokTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, top = 2.dp),
                    )
                    QuantityStepper(
                        quantity = state.quantity,
                        onDecrease = { onIntent(ItemConfirmIntent.Decrease) },
                        onIncrease = { onIntent(ItemConfirmIntent.Increase) },
                        buttonSize = 48.dp,
                        maxQuantity = ItemConfirmUiState.MAX_QUANTITY,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        ChagokPrimaryButton(
            text = "저장하기",
            onClick = { onIntent(ItemConfirmIntent.Save) },
            enabled = state.canSave,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DimensionCard(
    label: String,
    value: String,
    dimension: Dimension,
    onIntent: (ItemConfirmIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(modifier = modifier.height(100.dp)) {
        Text(text = label, color = ChagokTextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Box(modifier = Modifier.width(56.dp)) {
                InputField(
                    value = value,
                    onValueChange = { onIntent(ItemConfirmIntent.DimensionChanged(dimension, it)) },
                    placeholder = "0",
                    textStyle = TextStyle(color = ChagokTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
                    keyboardType = KeyboardType.Number,
                )
            }
            Text(
                text = "cm",
                color = ChagokTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/** 테두리 없는 입력칸 + 빈 값일 때 흐린 안내 문구 */
@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    keyboardType: KeyboardType,
) {
    Box {
        if (value.isEmpty()) Text(text = placeholder, style = textStyle.copy(color = PlaceholderColor))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(ChagokNavy),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        content()
    }
}

private val GuideText = Color(0xFF5A606B)
private val PlaceholderColor = Color(0xFFC3C8D2)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ItemConfirmScreenPreview() {
    HyundaeSubTheme {
        ItemConfirmScreen(
            state = ItemConfirmUiState(name = "유모차", width = "42", depth = "58", height = "88", fromMeasure = true),
            onIntent = {},
            onBack = {},
        )
    }
}
