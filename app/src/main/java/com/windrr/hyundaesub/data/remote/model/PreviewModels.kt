package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

// ── POST /api/v1/calculations/preview 요청 ──────────────────────────────

/** 저장 전 미리 계산. 새 계획은 source_plan_id 없이 plan만 보낸다 */
data class PreviewRequest(
    @SerializedName("plan") val plan: PreviewPlanInput,
    @SerializedName("calculation_options") val calculationOptions: CalculationOptions = CalculationOptions(),
)

data class PreviewPlanInput(
    @SerializedName("vehicle_id") val vehicleId: String,
    /** 운전자 포함 탑승 인원 (1~9) */
    @SerializedName("passenger_count") val passengerCount: Int,
    /** 사람이 앉는 좌석 (운전석 포함) */
    @SerializedName("passenger_seats") val passengerSeats: List<String>,
    @SerializedName("car_seats") val carSeats: List<String> = emptyList(),
    @SerializedName("retained_seats") val retainedSeats: List<String> = emptyList(),
    @SerializedName("items") val items: List<PreviewItemInput>,
)

/**
 * 짐 한 종류. 앱에서 정하지 않는 값은 Swagger 기본값을 그대로 둔다
 * (allowed_orientations 생략 = 6방향, transport_state UNKNOWN).
 */
data class PreviewItemInput(
    @SerializedName("name") val name: String,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("quantity") val quantity: Int,
    /** 꼭 가져갈 개수. 0이면 못 실을 때 먼저 뺄 수 있음 */
    @SerializedName("required_quantity") val requiredQuantity: Int = 0,
    @SerializedName("transport_state") val transportState: String = "UNKNOWN",
    /** AR / MANUAL / SAVED_ITEM / PRODUCT_SOURCE / UNSPECIFIED */
    @SerializedName("dimension_source") val dimensionSource: String = "UNSPECIFIED",
    @SerializedName("saved_item_id") val savedItemId: String? = null,
)

data class CalculationOptions(
    @SerializedName("packing_mode") val packingMode: String = "REPACK_ALL",
    @SerializedName("allow_exclusions") val allowExclusions: Boolean = false,
    /** 좌석이 비었는지 알려야 서버가 그 좌석·접은 좌석에 짐을 올린다 (없으면 STATE_REQUIRED) */
    @SerializedName("vehicle_state") val vehicleState: VehicleStateInput? = null,
)

data class VehicleStateInput(
    /** ISO-8601 (오프셋 포함) */
    @SerializedName("observed_at") val observedAt: String,
    @SerializedName("seats") val seats: List<SeatObservationInput>,
    @SerializedName("fold_segments") val foldSegments: List<FoldObservationInput> = emptyList(),
)

data class SeatObservationInput(
    @SerializedName("seat_code") val seatCode: String,
    /** OCCUPIED / EMPTY / UNKNOWN */
    @SerializedName("occupancy") val occupancy: String,
    /** SDK / USER / UNKNOWN */
    @SerializedName("source") val source: String = "USER",
)

data class FoldObservationInput(
    @SerializedName("segment_code") val segmentCode: String,
    @SerializedName("state") val state: String,
    @SerializedName("source") val source: String = "USER",
)

// ── 응답 ────────────────────────────────────────────────────────────────

data class PreviewResponse(
    @SerializedName("preview_id") val previewId: String,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("calculation") val calculation: PreviewCalculation,
)

data class PreviewCalculation(
    @SerializedName("is_success") val isSuccess: Boolean,
    /** LAYOUT_FOUND, EXCLUSION_OPTIONS_FOUND, NO_LAYOUT_FOUND, TIME_LIMIT, NO_VALID_SEAT_CONFIGURATION, STATE_REQUIRED */
    @SerializedName("result_code") val resultCode: String,
    @SerializedName("input_snapshot") val inputSnapshot: InputSnapshot?,
    /** rank 순 배치안 */
    @SerializedName("arrangements") val arrangements: List<ArrangementResponse>?,
    @SerializedName("alternatives") val alternatives: List<ArrangementResponse>?,
    @SerializedName("required_inputs") val requiredInputs: List<RequiredInputResponse>?,
    @SerializedName("warnings") val warnings: List<String>?,
)

/** 서버가 받은 입력 그대로 + 서버가 붙인 item_id */
data class InputSnapshot(
    @SerializedName("items") val items: List<SnapshotItem>?,
)

data class SnapshotItem(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("quantity") val quantity: Int?,
)

data class ArrangementResponse(
    @SerializedName("arrangement_id") val arrangementId: String,
    @SerializedName("rank") val rank: Int,
    /** 이 배치에서 사람이 앉는 좌석 (운전석 포함) */
    @SerializedName("passenger_seats") val passengerSeats: List<String>?,
    /** 이 배치를 위해 접어야 하는 구간 코드 */
    @SerializedName("target_folded_segments") val targetFoldedSegments: List<String>?,
    @SerializedName("packing_spaces") val packingSpaces: List<PackingSpaceResponse>?,
    @SerializedName("placements") val placements: List<PlacementResponse>?,
    @SerializedName("all_in_trunk") val allInTrunk: Boolean?,
    @SerializedName("reason_codes") val reasonCodes: List<String>?,
)

/**
 * 짐을 놓는 공간. origin_cm은 차량 기준 좌표:
 * x = 차 왼쪽(운전석 쪽)→오른쪽, y = 트렁크 끝(0)→앞쪽, z = 바닥→위.
 */
data class PackingSpaceResponse(
    @SerializedName("space_id") val spaceId: String,
    /** TRUNK / SEAT / FOLDED_SEAT */
    @SerializedName("kind") val kind: String,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("origin_cm") val originCm: SpaceOriginResponse?,
    @SerializedName("seat_codes") val seatCodes: List<String>?,
)

data class SpaceOriginResponse(
    @SerializedName("x_cm") val xCm: Double,
    @SerializedName("y_cm") val yCm: Double,
    @SerializedName("z_cm") val zCm: Double,
)

/** 짐 한 개의 위치. x/y/z는 해당 공간(space_id) 원점 기준 */
data class PlacementResponse(
    @SerializedName("placement_id") val placementId: String,
    @SerializedName("item_id") val itemId: String,
    @SerializedName("unit_index") val unitIndex: Int,
    @SerializedName("space_id") val spaceId: String,
    @SerializedName("x_cm") val xCm: Double,
    @SerializedName("y_cm") val yCm: Double,
    @SerializedName("z_cm") val zCm: Double,
    /** 세 글자 = x·y·z 축에 놓인 짐 치수 (W 가로, D 세로, H 높이). 예: WHD → x=가로, y=높이, z=세로 */
    @SerializedName("orientation") val orientation: String,
    /** 싣는 순서 (1부터) */
    @SerializedName("load_order") val loadOrder: Int,
)

data class RequiredInputResponse(
    @SerializedName("field") val field: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("seat_code") val seatCode: String?,
    @SerializedName("segment_code") val segmentCode: String?,
)
