package com.windrr.hyundaesub.ui.main

import androidx.lifecycle.ViewModel
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.measure.MeasureGeometry
import com.windrr.hyundaesub.ui.item.ItemSource
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

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
            MainIntent.AddNewItem -> _state.update {
                it.copy(route = MainRoute.NEW_ITEM_GUIDE, lastItemSource = ItemSource.NEW)
            }
            MainIntent.StartArMeasure -> _state.update {
                it.copy(route = MainRoute.MEASURE, measureReturnRoute = MainRoute.NEW_ITEM_GUIDE)
            }
            // TODO: 직접 입력 화면 시안 나오면 연결
            MainIntent.OpenManualInput -> Unit
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
            is MainIntent.ItemMeasured -> _state.update {
                val item = measuredItem(intent.size, it.pickedItems) ?: return@update it
                it.copy(
                    route = MainRoute.ITEM_SELECT,
                    measureReturnRoute = MainRoute.TABS,
                    pickedItems = it.pickedItems + item,
                )
            }
            MainIntent.BackToTabs -> _state.update { it.copy(route = MainRoute.TABS) }
            MainIntent.OpenFrequentItems -> _state.update { it.copy(selectedTab = MainTab.MY_ITEMS) }
        }
    }

    /**
     * AR 측정값(cm, 소수) → 짐. 판정용으로 여유를 더해 올림한 정수 cm를 쓴다.
     * TODO: 이름 입력 시안 나오면 사용자가 정한 이름으로
     */
    private fun measuredItem(size: BoxSize, current: List<PickedItem>): PickedItem? {
        val width = size.widthCm ?: return null
        val depth = size.depthCm ?: return null
        val height = size.heightCm ?: return null
        val number = current.count { it.savedItemId == null } + 1
        return PickedItem(
            key = "measured-${UUID.randomUUID()}",
            name = "측정한 물건 $number",
            widthCm = MeasureGeometry.roundUpForFit(width).toInt(),
            depthCm = MeasureGeometry.roundUpForFit(depth).toInt(),
            heightCm = MeasureGeometry.roundUpForFit(height).toInt(),
        )
    }

    /** 같은 물건을 다시 불러오면 수량을 새 값으로 덮어쓴다 */
    private fun merge(current: List<PickedItem>, added: List<PickedItem>): List<PickedItem> {
        val addedKeys = added.map { it.key }.toSet()
        return current.filterNot { it.key in addedKeys } + added
    }
}
