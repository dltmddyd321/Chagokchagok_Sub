package com.windrr.hyundaesub.ui.main

import androidx.lifecycle.ViewModel
import com.windrr.hyundaesub.ui.item.ItemSource
import com.windrr.hyundaesub.ui.item.PickedItem
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
                    pickedItems = emptyList(),
                    lastItemSource = null,
                    loadFlowId = it.loadFlowId + 1,
                )
            }
            is MainIntent.VehicleSelected -> _state.update {
                it.copy(selectedVehicleId = intent.vehicleId, route = MainRoute.SEAT_SELECT)
            }
            is MainIntent.SeatsSelected -> _state.update {
                it.copy(occupiedSeats = intent.seats, route = MainRoute.ITEM_SELECT)
            }
            MainIntent.BackToVehicleSelect -> _state.update { it.copy(route = MainRoute.VEHICLE_SELECT) }
            MainIntent.BackToSeatSelect -> _state.update { it.copy(route = MainRoute.SEAT_SELECT) }
            // TODO: AR 측정 결과를 PickedItem으로 담는 연결 (측정 화면 MVI 전환 때)
            MainIntent.AddNewItem -> _state.update {
                it.copy(
                    route = MainRoute.MEASURE,
                    lastItemSource = ItemSource.NEW,
                    measureReturnRoute = MainRoute.ITEM_SELECT,
                )
            }
            MainIntent.OpenSavedItems -> _state.update {
                it.copy(route = MainRoute.SAVED_ITEMS, lastItemSource = ItemSource.SAVED)
            }
            is MainIntent.SavedItemsPicked -> _state.update {
                it.copy(route = MainRoute.ITEM_SELECT, pickedItems = merge(it.pickedItems, intent.items))
            }
            MainIntent.BackToItemSelect -> _state.update { it.copy(route = MainRoute.ITEM_SELECT) }
            is MainIntent.RemovePickedItem -> _state.update {
                it.copy(pickedItems = it.pickedItems.filterNot { item -> item.key == intent.key })
            }
            // TODO: 다음 단계(배치 결과) 시안 나오면 연결
            MainIntent.ItemsDone -> Unit
            MainIntent.CloseMeasure -> _state.update {
                it.copy(route = it.measureReturnRoute, measureReturnRoute = MainRoute.TABS)
            }
            MainIntent.BackToTabs -> _state.update { it.copy(route = MainRoute.TABS) }
            MainIntent.OpenFrequentItems -> _state.update { it.copy(selectedTab = MainTab.MY_ITEMS) }
        }
    }

    /** 같은 물건을 다시 불러오면 수량을 새 값으로 덮어쓴다 */
    private fun merge(current: List<PickedItem>, added: List<PickedItem>): List<PickedItem> {
        val addedKeys = added.map { it.key }.toSet()
        return current.filterNot { it.key in addedKeys } + added
    }
}
