package com.windrr.hyundaesub.ui.main

import androidx.annotation.DrawableRes
import com.windrr.hyundaesub.R
import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.measure.MeasureShape
import com.windrr.hyundaesub.ui.item.ItemSource
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.result.ConditionChange

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

    /** 짐 등록 3단계: 어떤 짐을 가져갈지 */
    ITEM_SELECT,

    /** 짐 선택 > 등록된 물건 불러오기 */
    SAVED_ITEMS,

    /** 짐 선택 > 새 물건 추가: 측정 방법 안내 */
    NEW_ITEM_GUIDE,

    /** AR 측정 전: 물건 바닥 모양 선택 (네모 / 둥근) */
    SHAPE_SELECT,

    /** AR 촬영 후 측정값 확인 / 직접 입력 */
    ITEM_CONFIRM,

    /** 배치 추천 (POST /calculations/preview 결과) */
    RECOMMEND,

    /** 배치 확인 후 계획 이름·카테고리·메모 입력 */
    PLAN_SAVE,

    /** 배치 추천 결과 없음 */
    LAYOUT_FAILED,

    /** 배치 실패 > 조건 변경 (짐 조정 / 탑승 인원 변경 선택) */
    CONDITION_CHANGE,

    /** 조건 변경 > 꼭 가져갈 짐 체크 */
    REQUIRED_ITEMS,

    /** 조건 변경 > 탑승 좌석 다시 선택 */
    SEAT_RESELECT,

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
    /** 이번 계획에 담은 짐 */
    val pickedItems: List<PickedItem> = emptyList(),
    /** 짐 선택 화면에서 마지막으로 고른 방식 (카드 강조용) */
    val lastItemSource: ItemSource? = null,
    /** AR 측정에서 뒤로 갈 때 돌아갈 화면 */
    val measureReturnRoute: MainRoute = MainRoute.TABS,
    /** 확인 화면에 채울 AR 측정값 (null = 직접 입력) */
    val measuredSize: BoxSize? = null,
    /** AR 측정 중 가장 마지막에 찍은 사진 (없으면 null → 확인 화면에 기본 이미지) */
    val measuredPhotoUri: String? = null,
    /** AR 측정 방식 (바닥 모양) */
    val measureShape: MeasureShape = MeasureShape.BOX,
    /** 확인 화면에 들어올 때마다 증가 — 확인 화면 ViewModel을 새로 만드는 키 */
    val confirmId: Int = 0,
    /** 배치 실패 후 바꾸기로 고른 조건 */
    val conditionChanges: Set<ConditionChange> = emptySet(),
    /** 좌석 다시 선택에 들어올 때마다 증가 — 좌석 ViewModel을 새로 만드는 키 */
    val seatReselectId: Int = 0,
    /** 배치 추천을 새로 요청할 때마다 증가 — 추천 ViewModel(= 새 Idempotency-Key)을 새로 만드는 키 */
    val recommendId: Int = 0,
    /** 추천 화면에서 확인한 미리보기·배치. 계획 저장 때 POST /plans(source_preview_id, selected_arrangement_id) */
    val confirmedPreviewId: String? = null,
    val confirmedArrangementId: String? = null,
)

sealed interface MainIntent {
    data class SelectTab(val tab: MainTab) : MainIntent

    /** 홈 히어로 카드 화살표: 짐 등록 흐름 시작 */
    data object StartLoadFlow : MainIntent

    /** 차량 선택 완료 → 좌석 선택 */
    data class VehicleSelected(val vehicleId: String) : MainIntent

    /** 좌석 선택 완료 → 짐 선택 */
    data class SeatsSelected(val seats: Set<SeatCode>) : MainIntent

    /** 좌석 선택에서 뒤로 → 차량 선택 */
    data object BackToVehicleSelect : MainIntent

    /** 짐 선택에서 뒤로 → 좌석 선택 */
    data object BackToSeatSelect : MainIntent

    /** 짐 선택 > 새 물건 추가 → 측정 안내 */
    data object AddNewItem : MainIntent

    /** 측정 안내 > AR 측정하기 → 바닥 모양 선택 */
    data object StartArMeasure : MainIntent

    data class SelectMeasureShape(val shape: MeasureShape) : MainIntent

    /** 바닥 모양 선택 > 다음 → 전체 화면 AR 카메라 */
    data object ShapeSelected : MainIntent

    /** 바닥 모양 선택 > 이전 → 측정 안내 */
    data object BackFromShapeSelect : MainIntent

    /** 측정 안내 > 직접 입력하기 → 빈 확인 화면 */
    data object OpenManualInput : MainIntent

    /** 짐 선택 > 등록된 물건 불러오기 */
    data object OpenSavedItems : MainIntent

    /** 불러오기 화면에서 저장 → 짐 선택으로 돌아가며 담기 */
    data class SavedItemsPicked(val items: List<PickedItem>) : MainIntent

    /** 불러오기·측정 화면에서 뒤로 → 짐 선택 */
    data object BackToItemSelect : MainIntent

    /** 짐 선택 목록: 체크(배치 추천에 포함) 토글 */
    data class TogglePickedItem(val key: String) : MainIntent

    data class IncreasePickedQuantity(val key: String) : MainIntent

    data class DecreasePickedQuantity(val key: String) : MainIntent

    /** 짐 배치 추천받기 */
    data object ItemsDone : MainIntent

    /** 배치 실패 화면 > 조건 변경하기 */
    data object ChangeConditions : MainIntent

    data class ToggleConditionChange(val change: ConditionChange) : MainIntent

    /** 조건 변경 > 다음 (짐 조정 → 좌석 재선택 순) */
    data object ConditionChangesDone : MainIntent

    /** 꼭 가져갈 짐 체크 토글 */
    data class ToggleRequiredItem(val key: String) : MainIntent

    /** 꼭 가져갈 짐 > 선택 완료 */
    data object RequiredItemsDone : MainIntent

    /** 좌석 다시 선택 > 선택 완료 → 다시 추천 */
    data class SeatsReselected(val seats: Set<SeatCode>) : MainIntent

    /** 꼭 가져갈 짐 / 좌석 다시 선택에서 뒤로 */
    data object BackInConditionFlow : MainIntent

    /** 조건 변경에서 뒤로 → 실패 화면 */
    data object BackToLayoutFailed : MainIntent

    /** AR 측정 화면에서 뒤로 */
    data object CloseMeasure : MainIntent

    /** AR 촬영 완료 → 측정값 확인 화면 */
    data class ItemMeasured(val size: BoxSize, val photoUri: String?) : MainIntent

    /** 확인 화면에서 뒤로 → 측정에서 왔으면 다시 촬영, 직접 입력이면 안내 화면 */
    data object BackFromConfirm : MainIntent

    /** 확인 화면에서 저장 → 짐 목록에 담고 짐 선택으로 */
    data class ItemConfirmed(val item: PickedItem) : MainIntent

    /** 추천 화면: 배치를 못 찾음 → 실패 화면 */
    data object NoLayoutFound : MainIntent

    /** 추천 화면 확인 → 계획 저장 */
    data class ArrangementConfirmed(val previewId: String, val arrangementId: String) : MainIntent

    /** 계획 저장에서 뒤로 → 추천 화면 */
    data object BackToRecommend : MainIntent

    /** 전체 화면에서 뒤로 → 탭 화면 */
    data object BackToTabs : MainIntent

    /** 홈 "자주 싣는 짐 추가하기" → 내 짐 탭 */
    data object OpenFrequentItems : MainIntent
}
