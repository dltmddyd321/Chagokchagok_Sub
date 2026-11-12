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
            MainIntent.StartLoadFlow -> _state.update {
                it.copy(
                    route = MainRoute.VEHICLE_SELECT,
                    selectedVehicleId = null,
                    occupiedSeats = emptySet(),
                    loadFlowId = it.loadFlowId + 1,
                )
            }
            is MainIntent.VehicleSelected -> _state.update {
                it.copy(selectedVehicleId = intent.vehicleId, route = MainRoute.SEAT_SELECT)
            }
            // TODO: 3단계(짐 등록) 시안 나오면 교체. 지금은 AR 측정으로 바로 연결
            is MainIntent.SeatsSelected -> _state.update {
                it.copy(occupiedSeats = intent.seats, route = MainRoute.MEASURE)
            }
            MainIntent.BackToVehicleSelect -> _state.update { it.copy(route = MainRoute.VEHICLE_SELECT) }
            MainIntent.BackToTabs -> _state.update { it.copy(route = MainRoute.TABS) }
            MainIntent.OpenFrequentItems -> _state.update { it.copy(selectedTab = MainTab.MY_ITEMS) }
        }
    }
}
