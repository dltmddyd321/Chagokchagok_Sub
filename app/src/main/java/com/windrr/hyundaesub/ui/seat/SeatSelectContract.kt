package com.windrr.hyundaesub.ui.seat

import com.windrr.hyundaesub.data.local.entity.SeatCode

/** 아반떼(5인승) 좌석 배치. 운전석은 항상 탑승이라 선택 대상이 아니다 */
object AvanteSeats {
    val DRIVER = SeatCode.ROW_1_LEFT
    val SELECTABLE = listOf(
        SeatCode.ROW_1_RIGHT,
        SeatCode.ROW_2_LEFT,
        SeatCode.ROW_2_CENTER,
        SeatCode.ROW_2_RIGHT,
    )
}

data class SeatSelectUiState(
    /** 사람·카시트가 차지할 좌석 (운전석 제외) */
    val selectedSeats: Set<SeatCode> = emptySet(),
) {
    val selectedCount: Int get() = selectedSeats.size

    /** 서버 passenger_count = 운전자 + 선택 좌석 */
    val passengerCount: Int get() = 1 + selectedSeats.size
}

sealed interface SeatSelectIntent {
    data class ToggleSeat(val seat: SeatCode) : SeatSelectIntent

    data object Next : SeatSelectIntent
}

sealed interface SeatSelectEffect {
    data class GoNext(val occupiedSeats: Set<SeatCode>) : SeatSelectEffect
}
