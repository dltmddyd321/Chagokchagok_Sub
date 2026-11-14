package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlanCreateRequest(
    @SerializedName("vehicle_id") val vehicleId: String,
    @SerializedName("passenger_count") val passengerCount: Int,
    @SerializedName("car_seats") val carSeats: List<String> = emptyList(),
    @SerializedName("retained_seats") val retainedSeats: List<String> = emptyList(),
    @SerializedName("items") val items: List<PlanItemRequest>,
)

data class PlanItemRequest(
    @SerializedName("name") val name: String,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("quantity") val quantity: Int,
)

/** 새 계획 저장: 미리보기 결과 + 고른 배치로 만든다 (POST /api/v1/plans, PlanFromResult) */
data class PlanFromResultRequest(
    @SerializedName("source_preview_id") val sourcePreviewId: String,
    @SerializedName("selected_arrangement_id") val selectedArrangementId: String,
)

/** 계획 이름·카테고리·메모·즐겨찾기 (PATCH /api/v1/plans/{plan_id}/metadata). expected_revision 필수 */
data class PlanMetadataRequest(
    @SerializedName("expected_revision") val expectedRevision: Int,
    @SerializedName("name") val name: String?,
    /** TRAVEL / LEISURE / MOVING */
    @SerializedName("category") val category: String?,
    @SerializedName("memo") val memo: String?,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
)

data class PlanResponse(
    @SerializedName("plan_id") val planId: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("memo") val memo: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = null,
    /** 낙관적 잠금 버전. 수정 요청에 expected_revision으로 보낸다 */
    @SerializedName("revision") val revision: Int? = null,
    @SerializedName("vehicle_id") val vehicleId: String?,
    @SerializedName("passenger_count") val passengerCount: Int?,
    @SerializedName("car_seats") val carSeats: List<String>?,
    @SerializedName("retained_seats") val retainedSeats: List<String>?,
    @SerializedName("items") val items: List<PlanItemResponse>?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("latest_calculation_id") val latestCalculationId: String?,
)

data class PlanItemResponse(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("width_cm") val widthCm: Double?,
    @SerializedName("depth_cm") val depthCm: Double?,
    @SerializedName("height_cm") val heightCm: Double?,
    @SerializedName("quantity") val quantity: Int?,
)
