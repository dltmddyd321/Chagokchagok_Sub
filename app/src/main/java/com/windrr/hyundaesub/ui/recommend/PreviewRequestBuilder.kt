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
     * 앱 차량 id → 서버 차량 id. 1차는 투싼 하이브리드 고정 (서버 GET /vehicles의 tucson_hybrid, 실측 공간).
     * 차량이 늘면 여기서 매핑한다.
     */
    const val SERVER_VEHICLE_ID = "tucson_hybrid"

    fun serverVehicleId(@Suppress("UNUSED_PARAMETER") localVehicleId: String?): String = SERVER_VEHICLE_ID

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
        // 내 물건은 1차에서 기기(Room)에만 저장한다. 그 id를 보내면 서버가 모르는 내 짐이라 422
        // ("사용할 수 없는 내 짐 ID") → 서버에는 출처·id 없이 치수만 보낸다
        dimensionSource = "UNSPECIFIED",
        savedItemId = null,
    )

    private const val OCCUPIED = "OCCUPIED"
    private const val EMPTY = "EMPTY"
    private const val SOURCE_USER = "USER"
}
