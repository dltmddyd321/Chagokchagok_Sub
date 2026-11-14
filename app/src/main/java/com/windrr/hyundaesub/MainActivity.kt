package com.windrr.hyundaesub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.windrr.hyundaesub.measure.ArMeasureScreen
import com.windrr.hyundaesub.push.PushNotifier
import com.windrr.hyundaesub.push.PushTokenSync
import com.windrr.hyundaesub.ui.plan.PlanSaveRoute
import com.windrr.hyundaesub.ui.recommend.RecommendInput
import com.windrr.hyundaesub.ui.recommend.RecommendRoute
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** 알림 권한은 거절해도 앱 사용엔 지장 없음 (푸시만 안 보임) */
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setUpPush()
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
                        onClose = { viewModel.onIntent(MainIntent.BackToTabs) },
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
                        photoUri = state.measuredPhotoUri,
                        onBack = { viewModel.onIntent(MainIntent.BackFromConfirm) },
                        onSaved = { viewModel.onIntent(MainIntent.ItemConfirmed(it)) },
                        viewModel = viewModel(key = "confirm-${state.confirmId}"),
                    )
                    MainRoute.RECOMMEND -> RecommendRoute(
                        input = RecommendInput(
                            localVehicleId = state.selectedVehicleId,
                            occupiedSeats = state.occupiedSeats,
                            items = state.pickedItems,
                        ),
                        onBack = { viewModel.onIntent(MainIntent.BackToItemSelect) },
                        onClose = { viewModel.onIntent(MainIntent.BackToTabs) },
                        onNoLayout = { viewModel.onIntent(MainIntent.NoLayoutFound) },
                        onConfirmed = { previewId, arrangementId ->
                            viewModel.onIntent(MainIntent.ArrangementConfirmed(previewId, arrangementId))
                        },
                        viewModel = viewModel(key = "recommend-${state.recommendId}"),
                    )
                    MainRoute.PLAN_SAVE -> PlanSaveRoute(
                        onBack = { viewModel.onIntent(MainIntent.BackToRecommend) },
                        // TODO: POST /plans {source_preview_id, selected_arrangement_id} → plan_id, 이후 이름·카테고리·메모는 PATCH metadata
                        onSaved = { },
                        viewModel = viewModel(key = "plan-save-${state.recommendId}"),
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
                        onMeasured = { size, photo -> viewModel.onIntent(MainIntent.ItemMeasured(size, photo)) },
                        shape = state.measureShape,
                    )
                }
            }
        }
    }

    /** FCM: 알림 채널 생성 → 알림 권한 요청(Android 13+) → 토큰 받아 저장·서버 등록 */
    private fun setUpPush() {
        PushNotifier.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch { PushTokenSync.refresh(applicationContext) }
    }
}
