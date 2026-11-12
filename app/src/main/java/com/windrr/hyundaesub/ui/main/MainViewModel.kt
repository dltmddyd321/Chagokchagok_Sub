package com.windrr.hyundaesub.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            MainIntent.StartLoadFlow -> _state.update { it.copy(route = MainRoute.VEHICLE_SELECT) }
            // TODO: 2·3단계 화면 시안 나오면 교체. 지금은 AR 측정으로 바로 연결
            is MainIntent.VehicleSelected -> _state.update {
                it.copy(selectedVehicleId = intent.vehicleId, route = MainRoute.MEASURE)
            }
            MainIntent.BackToTabs -> _state.update { it.copy(route = MainRoute.TABS) }
            MainIntent.OpenFrequentItems -> _state.update { it.copy(selectedTab = MainTab.MY_ITEMS) }
        }
    }
}
