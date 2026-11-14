package com.windrr.hyundaesub.ui.main

import androidx.lifecycle.ViewModel
import com.windrr.hyundaesub.ui.item.ItemSource
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.result.ConditionChange
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
            MainIntent.AddNewItem -> _state.update {
                it.copy(route = MainRoute.NEW_ITEM_GUIDE, lastItemSource = ItemSource.NEW)
            }
            MainIntent.StartArMeasure -> _state.update { it.copy(route = MainRoute.SHAPE_SELECT) }
            is MainIntent.SelectMeasureShape -> _state.update { it.copy(measureShape = intent.shape) }
            MainIntent.ShapeSelected -> _state.update {
                it.copy(route = MainRoute.MEASURE, measureReturnRoute = MainRoute.SHAPE_SELECT)
            }
            MainIntent.BackFromShapeSelect -> _state.update { it.copy(route = MainRoute.NEW_ITEM_GUIDE) }
            MainIntent.OpenManualInput -> _state.update {
                it.copy(route = MainRoute.ITEM_CONFIRM, measuredSize = null, measuredPhotoUri = null, confirmId = it.confirmId + 1)
            }
            MainIntent.OpenSavedItems -> _state.update {
                it.copy(route = MainRoute.SAVED_ITEMS, lastItemSource = ItemSource.SAVED)
            }
            is MainIntent.SavedItemsPicked -> _state.update {
                it.copy(route = MainRoute.ITEM_SELECT, pickedItems = merge(it.pickedItems, intent.items))
            }
            MainIntent.BackToItemSelect -> _state.update { it.copy(route = MainRoute.ITEM_SELECT) }
            is MainIntent.TogglePickedItem -> updatePicked(intent.key) { it.copy(selected = !it.selected) }
            is MainIntent.IncreasePickedQuantity -> updatePicked(intent.key) {
                // +는 체크도 같이 (내 물건 가져오기와 동일)
                it.copy(quantity = (it.quantity + 1).coerceAtMost(PickedItem.MAX_QUANTITY), selected = true)
            }
            is MainIntent.DecreasePickedQuantity -> updatePicked(intent.key) {
                it.copy(quantity = (it.quantity - 1).coerceAtLeast(1))
            }
            MainIntent.ItemsDone -> requestRecommendation()
            MainIntent.ChangeConditions -> _state.update {
                it.copy(route = MainRoute.CONDITION_CHANGE, conditionChanges = emptySet())
            }
            is MainIntent.ToggleConditionChange -> _state.update {
                val current = it.conditionChanges
                it.copy(
                    conditionChanges = if (intent.change in current) current - intent.change else current + intent.change,
                )
            }
            MainIntent.ConditionChangesDone -> {
                val changes = _state.value.conditionChanges
                when {
                    ConditionChange.ITEMS in changes -> _state.update { it.copy(route = MainRoute.REQUIRED_ITEMS) }
                    ConditionChange.PASSENGERS in changes -> openSeatReselect()
                }
            }
            is MainIntent.ToggleRequiredItem -> updatePicked(intent.key) { it.copy(required = !it.required) }
            MainIntent.RequiredItemsDone -> {
                if (ConditionChange.PASSENGERS in _state.value.conditionChanges) openSeatReselect() else requestRecommendation()
            }
            is MainIntent.SeatsReselected -> {
                _state.update { it.copy(occupiedSeats = intent.seats) }
                requestRecommendation()
            }
            MainIntent.BackInConditionFlow -> _state.update {
                val back = if (it.route == MainRoute.SEAT_RESELECT && ConditionChange.ITEMS in it.conditionChanges) {
                    MainRoute.REQUIRED_ITEMS
                } else {
                    MainRoute.CONDITION_CHANGE
                }
                it.copy(route = back)
            }
            MainIntent.BackToLayoutFailed -> _state.update { it.copy(route = MainRoute.LAYOUT_FAILED) }
            MainIntent.CloseMeasure -> _state.update {
                it.copy(route = it.measureReturnRoute, measureReturnRoute = MainRoute.TABS)
            }
            is MainIntent.ItemMeasured -> _state.update {
                it.copy(
                    route = MainRoute.ITEM_CONFIRM,
                    measuredSize = intent.size,
                    measuredPhotoUri = intent.photoUri,
                    confirmId = it.confirmId + 1,
                )
            }
            MainIntent.BackFromConfirm -> _state.update {
                it.copy(route = if (it.measuredSize != null) MainRoute.MEASURE else MainRoute.NEW_ITEM_GUIDE)
            }
            is MainIntent.ItemConfirmed -> _state.update {
                it.copy(
                    route = MainRoute.ITEM_SELECT,
                    measureReturnRoute = MainRoute.TABS,
                    measuredSize = null,
                    pickedItems = it.pickedItems + intent.item,
                )
            }
            MainIntent.NoLayoutFound -> _state.update { it.copy(route = MainRoute.LAYOUT_FAILED) }
            is MainIntent.ArrangementConfirmed -> _state.update {
                it.copy(
                    route = MainRoute.PLAN_SAVE,
                    confirmedPreviewId = intent.previewId,
                    confirmedArrangementId = intent.arrangementId,
                )
            }
            MainIntent.BackToRecommend -> _state.update { it.copy(route = MainRoute.RECOMMEND) }
            is MainIntent.PlanSaved -> _state.update {
                it.copy(
                    route = MainRoute.PLAN_SAVED,
                    confirmedPreviewId = null,
                    confirmedArrangementId = null,
                    // 홈(탭 화면)은 저장 흐름 뒤에 그대로 있으므로 여기서 갱신 신호를 올린다
                    homeRefreshKey = it.homeRefreshKey + 1,
                )
            }
            MainIntent.GoHomeAfterSave -> _state.update {
                it.copy(route = MainRoute.TABS, selectedTab = MainTab.HOME, pickedItems = emptyList(), occupiedSeats = emptySet())
            }
            MainIntent.BackToTabs -> _state.update { it.copy(route = MainRoute.TABS) }
            MainIntent.OpenFrequentItems -> _state.update { it.copy(selectedTab = MainTab.MY_ITEMS) }
        }
    }

    private fun openSeatReselect() {
        _state.update { it.copy(route = MainRoute.SEAT_RESELECT, seatReselectId = it.seatReselectId + 1) }
    }

    /**
     * 배치 추천 요청 → 추천 화면이 POST /calculations/preview.
     * 체크한 짐(selected)만, 꼭 가져갈 짐은 required_quantity로 보낸다.
     * 조건이 바뀌어 다시 계산할 때마다 recommendId를 올려 새 ViewModel(새 Idempotency-Key)로 요청한다.
     */
    private fun requestRecommendation() {
        _state.update {
            it.copy(
                route = MainRoute.RECOMMEND,
                recommendId = it.recommendId + 1,
                confirmedPreviewId = null,
                confirmedArrangementId = null,
            )
        }
    }

    private fun updatePicked(key: String, transform: (PickedItem) -> PickedItem) {
        _state.update { state ->
            state.copy(pickedItems = state.pickedItems.map { if (it.key == key) transform(it) else it })
        }
    }

    /** 같은 물건을 다시 불러오면 수량을 새 값으로 덮어쓴다 */
    private fun merge(current: List<PickedItem>, added: List<PickedItem>): List<PickedItem> {
        val addedKeys = added.map { it.key }.toSet()
        return current.filterNot { it.key in addedKeys } + added
    }
}
