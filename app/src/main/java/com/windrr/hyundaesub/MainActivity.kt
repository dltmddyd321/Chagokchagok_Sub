package com.windrr.hyundaesub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.measure.ArMeasureScreen
import com.windrr.hyundaesub.ui.item.ItemSelectScreen
import com.windrr.hyundaesub.ui.item.confirm.ItemConfirmRoute
import com.windrr.hyundaesub.ui.item.newitem.NewItemGuideScreen
import com.windrr.hyundaesub.ui.item.shape.ShapeSelectScreen
import com.windrr.hyundaesub.ui.item.saved.SavedItemsRoute
import com.windrr.hyundaesub.ui.main.MainIntent
import com.windrr.hyundaesub.ui.main.MainRoute
import com.windrr.hyundaesub.ui.main.MainScreen
import com.windrr.hyundaesub.ui.main.MainViewModel
import com.windrr.hyundaesub.ui.result.ConditionChangeScreen
import com.windrr.hyundaesub.ui.result.LayoutFailedScreen
import com.windrr.hyundaesub.ui.result.RequiredItemsScreen
import com.windrr.hyundaesub.ui.seat.SeatSelectRoute
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme
import com.windrr.hyundaesub.ui.vehicle.VehicleSelectRoute

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HyundaeSubTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                when (state.route) {
                    MainRoute.TABS -> MainScreen(state = state, onIntent = viewModel::onIntent)
                    MainRoute.VEHICLE_SELECT -> VehicleSelectRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackToTabs) },
                        onNext = { viewModel.onIntent(MainIntent.VehicleSelected(it)) },
                        viewModel = viewModel(key = "vehicle-${state.loadFlowId}"),
                    )
                    MainRoute.SEAT_SELECT -> SeatSelectRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackToVehicleSelect) },
                        onNext = { viewModel.onIntent(MainIntent.SeatsSelected(it)) },
                        viewModel = viewModel(key = "seat-${state.loadFlowId}"),
                    )
                    MainRoute.ITEM_SELECT -> ItemSelectScreen(
                        pickedItems = state.pickedItems,
                        lastSource = state.lastItemSource,
                        onBack = { viewModel.onIntent(MainIntent.BackToSeatSelect) },
                        onAddNew = { viewModel.onIntent(MainIntent.AddNewItem) },
                        onLoadSaved = { viewModel.onIntent(MainIntent.OpenSavedItems) },
                        onToggleItem = { viewModel.onIntent(MainIntent.TogglePickedItem(it)) },
                        onIncrease = { viewModel.onIntent(MainIntent.IncreasePickedQuantity(it)) },
                        onDecrease = { viewModel.onIntent(MainIntent.DecreasePickedQuantity(it)) },
                        onNext = { viewModel.onIntent(MainIntent.ItemsDone) },
                    )
                    MainRoute.SAVED_ITEMS -> SavedItemsRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackToItemSelect) },
                        onDone = { viewModel.onIntent(MainIntent.SavedItemsPicked(it)) },
                        viewModel = viewModel(key = "saved-${state.loadFlowId}"),
                    )
                    MainRoute.NEW_ITEM_GUIDE -> NewItemGuideScreen(
                        onBack = { viewModel.onIntent(MainIntent.BackToItemSelect) },
                        onManualInput = { viewModel.onIntent(MainIntent.OpenManualInput) },
                        onStartAr = { viewModel.onIntent(MainIntent.StartArMeasure) },
                    )
                    MainRoute.ITEM_CONFIRM -> ItemConfirmRoute(
                        measured = state.measuredSize,
                        onBack = { viewModel.onIntent(MainIntent.BackFromConfirm) },
                        onSaved = { viewModel.onIntent(MainIntent.ItemConfirmed(it)) },
                        viewModel = viewModel(key = "confirm-${state.confirmId}"),
                    )
                    MainRoute.LAYOUT_FAILED -> LayoutFailedScreen(
                        onBack = { viewModel.onIntent(MainIntent.BackToItemSelect) },
                        onNewPlan = { viewModel.onIntent(MainIntent.StartLoadFlow) },
                        onChangeConditions = { viewModel.onIntent(MainIntent.ChangeConditions) },
                    )
                    MainRoute.CONDITION_CHANGE -> ConditionChangeScreen(
                        selected = state.conditionChanges,
                        onBack = { viewModel.onIntent(MainIntent.BackToLayoutFailed) },
                        onToggle = { viewModel.onIntent(MainIntent.ToggleConditionChange(it)) },
                        onNext = { viewModel.onIntent(MainIntent.ConditionChangesDone) },
                    )
                    MainRoute.REQUIRED_ITEMS -> RequiredItemsScreen(
                        items = state.pickedItems.filter { it.selected },
                        onBack = { viewModel.onIntent(MainIntent.BackInConditionFlow) },
                        onToggle = { viewModel.onIntent(MainIntent.ToggleRequiredItem(it)) },
                        onDone = { viewModel.onIntent(MainIntent.RequiredItemsDone) },
                    )
                    MainRoute.SEAT_RESELECT -> SeatSelectRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackInConditionFlow) },
                        onNext = { viewModel.onIntent(MainIntent.SeatsReselected(it)) },
                        viewModel = viewModel(key = "seat-reselect-${state.seatReselectId}"),
                        reselect = true,
                        initialSeats = state.occupiedSeats,
                    )
                    MainRoute.SHAPE_SELECT -> ShapeSelectScreen(
                        selected = state.measureShape,
                        onSelect = { viewModel.onIntent(MainIntent.SelectMeasureShape(it)) },
                        onBack = { viewModel.onIntent(MainIntent.BackFromShapeSelect) },
                        onNext = { viewModel.onIntent(MainIntent.ShapeSelected) },
                    )
                    MainRoute.MEASURE -> ArMeasureScreen(
                        onBack = { viewModel.onIntent(MainIntent.CloseMeasure) },
                        onMeasured = { viewModel.onIntent(MainIntent.ItemMeasured(it)) },
                        shape = state.measureShape,
                    )
                }
            }
        }
    }
}
