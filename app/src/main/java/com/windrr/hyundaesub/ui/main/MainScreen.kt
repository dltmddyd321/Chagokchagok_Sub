package com.windrr.hyundaesub.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import com.windrr.hyundaesub.ui.home.HomeScreen
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundBottom
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** 하단 탭 4개를 가진 메인 화면. 탭 콘텐츠 위에 글래스 탭바를 오버레이로 얹는다. */
@Composable
fun MainScreen(
    state: MainUiState,
    onIntent: (MainIntent) -> Unit,
) {
    val hazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ChagokBackgroundTop, ChagokBackgroundBottom))),
    ) {
        // 탭바 뒤로 비칠 콘텐츠 = hazeSource
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
        ) {
            when (state.selectedTab) {
                MainTab.HOME -> HomeScreen(onIntent = onIntent, refreshKey = state.homeRefreshKey)
                MainTab.PLAN -> TabPlaceholder("계획 탭 (준비 중)")
                MainTab.MY_ITEMS -> TabPlaceholder("내 짐 탭 (준비 중)")
                MainTab.MY -> TabPlaceholder("마이 탭 (준비 중)")
            }
        }

        ChagokBottomBar(
            hazeState = hazeState,
            selectedTab = state.selectedTab,
            onTabClick = { onIntent(MainIntent.SelectTab(it)) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TabPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = ChagokTextSecondary)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenPreview() {
    HyundaeSubTheme {
        MainScreen(state = MainUiState(), onIntent = {})
    }
}
