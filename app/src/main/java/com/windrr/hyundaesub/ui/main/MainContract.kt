package com.windrr.hyundaesub.ui.main

import androidx.annotation.DrawableRes
import com.windrr.hyundaesub.R

enum class MainTab(val label: String, @DrawableRes val iconRes: Int) {
    HOME("홈", R.drawable.ic_tab_home),
    PLAN("계획", R.drawable.ic_tab_plan),
    MY_ITEMS("내 짐", R.drawable.ic_tab_luggage),
    MY("마이", R.drawable.ic_tab_my),
}

data class MainUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val isMeasuring: Boolean = false,
)

sealed interface MainIntent {
    data class SelectTab(val tab: MainTab) : MainIntent

    /** 홈 히어로 카드: 짐 등록(측정) 시작 */
    data object OpenMeasure : MainIntent

    data object CloseMeasure : MainIntent

    /** 홈 "자주 싣는 짐 추가하기" → 내 짐 탭 */
    data object OpenFrequentItems : MainIntent
}
