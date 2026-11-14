package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.data.remote.model.CalculationOptions
import com.windrr.hyundaesub.data.remote.model.PreviewItemInput
import com.windrr.hyundaesub.data.remote.model.PreviewPlanInput
import com.windrr.hyundaesub.data.remote.model.PreviewRequest
import com.windrr.hyundaesub.data.remote.model.SeatObservationInput
import com.windrr.hyundaesub.data.remote.model.VehicleStateInput
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.seat.AvanteSeats
import java.time.OffsetDateTime

/** 배치 추천에 넘길 화면 입력 (차량·좌석·짐 단계에서 정한 것) */
data class RecommendInput(
    val localVehicleId: String?,
    /** 좌석 선택에서 고른 탑승 좌석 (운전석 제외) */
    val occupiedSeats: Set<SeatCode>,
    val items: List<PickedItem>,
)

/** 화면 입력 → POST /calculations/preview 요청 (SDK·네트워크와 분리된 순수 로직) */
internal object PreviewRequestBuilder {

    /**
     * 앱 차량 id → 서버 차량 id. 서버 차량 목록(GET /vehicles)에는 지금 아반떼 데모 하나뿐이라
     * 앱의 아반떼를 그쪽으로 보낸다. 차량이 늘면 여기서 매핑한다.
     */
    const val SERVER_AVANTE_ID = "avante_demo"

    fun serverVehicleId(@Suppress("UNUSED_PARAMETER") localVehicleId: String?): String = SERVER_AVANTE_ID

    /** 이 차의 전체 좌석 (운전석 포함) */
    private val ALL_SEATS: List<SeatCode> = listOf(AvanteSeats.DRIVER) + AvanteSeats.SELECTABLE

    fun build(input: RecommendInput, observedAt: OffsetDateTime = OffsetDateTime.now()): PreviewRequest {
        val passengers = listOf(AvanteSeats.DRIVER) + ALL_SEATS.filter { it in input.occupiedSeats && it != AvanteSeats.DRIVER }
        return PreviewRequest(
            plan = PreviewPlanInput(
                vehicleId = serverVehicleId(input.localVehicleId),
                passengerCount = passengers.size,
                passengerSeats = passengers.map { it.name },
                items = input.items.filter { it.selected }.map(::toItemInput),
            ),
            calculationOptions = CalculationOptions(
                // 좌석 선택에서 고르지 않은 좌석은 사용자가 "비어 있음"이라고 알려 준 것 → 짐을 올리거나 접을 수 있다
                vehicleState = VehicleStateInput(
                    observedAt = observedAt.toString(),
                    seats = ALL_SEATS.map { seat ->
                        SeatObservationInput(
                            seatCode = seat.name,
                            occupancy = if (seat in passengers) OCCUPIED else EMPTY,
                            source = SOURCE_USER,
                        )
                    },
                ),
            ),
        )
    }

    private fun toItemInput(item: PickedItem) = PreviewItemInput(
        name = item.name.ifBlank { "짐" },
        widthCm = item.widthCm.toDouble(),
        depthCm = item.depthCm.toDouble(),
        heightCm = item.heightCm.toDouble(),
        quantity = item.quantity,
        requiredQuantity = if (item.required) item.quantity else 0,
        // 등록된 물건에서 불러온 것만 출처를 안다. 나머지(AR·직접 입력 구분 없음)는 기본값
        dimensionSource = if (item.savedItemId != null) "SAVED_ITEM" else "UNSPECIFIED",
        savedItemId = item.savedItemId,
    )

    private const val OCCUPIED = "OCCUPIED"
    private const val EMPTY = "EMPTY"
    private const val SOURCE_USER = "USER"
}
