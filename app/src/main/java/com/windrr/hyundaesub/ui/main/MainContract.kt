package com.windrr.hyundaesub.ui.main

import androidx.annotation.DrawableRes
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.data.local.entity.SeatCode

enum class MainTab(val label: String, @DrawableRes val iconRes: Int) {
    HOME("홈", R.drawable.ic_tab_home),
    PLAN("계획", R.drawable.ic_tab_plan),
    MY_ITEMS("내 짐", R.drawable.ic_tab_luggage),
    MY("마이", R.drawable.ic_tab_my),
}

/** 탭 화면 위에 덮어 띄우는 전체 화면 */
enum class MainRoute {
    TABS,

    /** 짐 등록 1단계: 차량 선택 */
    VEHICLE_SELECT,

    /** 짐 등록 2단계: 좌석 선택 */
    SEAT_SELECT,

    /** AR 크기 측정 */
    MEASURE,
}

data class MainUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val route: MainRoute = MainRoute.TABS,
    /** 차량 선택 단계에서 고른 차량 */
    val selectedVehicleId: String? = null,
    /** 좌석 선택 단계에서 고른 탑승·카시트 좌석 (운전석 제외) */
    val occupiedSeats: Set<SeatCode> = emptySet(),
    /** 짐 등록 흐름을 새로 시작할 때마다 증가 — 단계별 ViewModel을 새로 만드는 키 */
    val loadFlowId: Int = 0,
)

sealed interface MainIntent {
    data class SelectTab(val tab: MainTab) : MainIntent

    /** 홈 히어로 카드 화살표: 짐 등록 흐름 시작 */
    data object StartLoadFlow : MainIntent

    /** 차량 선택 완료 → 좌석 선택 */
    data class VehicleSelected(val vehicleId: String) : MainIntent

    /** 좌석 선택 완료 → 다음 단계 */
    data class SeatsSelected(val seats: Set<SeatCode>) : MainIntent

    /** 좌석 선택에서 뒤로 → 차량 선택 */
    data object BackToVehicleSelect : MainIntent

    /** 전체 화면에서 뒤로 → 탭 화면 */
    data object BackToTabs : MainIntent

    /** 홈 "자주 싣는 짐 추가하기" → 내 짐 탭 */
    data object OpenFrequentItems : MainIntent
}
