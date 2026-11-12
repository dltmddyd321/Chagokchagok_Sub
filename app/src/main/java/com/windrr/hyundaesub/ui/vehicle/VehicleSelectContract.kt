package com.windrr.hyundaesub.ui.vehicle

import com.windrr.hyundaesub.data.local.entity.VehicleEntity

enum class VehicleChoice { MY_CAR, OTHER }

data class VehicleSelectUiState(
    /** 대표 차량 (DB is_main). 로딩 전이면 null */
    val myCar: VehicleEntity? = null,
    val choice: VehicleChoice = VehicleChoice.MY_CAR,
) {
    val canProceed: Boolean get() = choice == VehicleChoice.MY_CAR && myCar != null
}

sealed interface VehicleSelectIntent {
    data object SelectMyCar : VehicleSelectIntent

    /** 1차는 차량 1대만 지원 — 다른 차량 검색은 아직 동작하지 않음 */
    data object SelectOther : VehicleSelectIntent

    data object Next : VehicleSelectIntent
}

sealed interface VehicleSelectEffect {
    data class GoNext(val vehicleId: String) : VehicleSelectEffect
}
