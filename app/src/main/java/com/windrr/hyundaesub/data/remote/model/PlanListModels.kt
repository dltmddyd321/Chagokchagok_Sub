package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

/** GET /api/v1/plans 응답 data */
data class PlanListResponse(
    @SerializedName("plans") val plans: List<PlanSummaryResponse>?,
    @SerializedName("limit") val limit: Int?,
    @SerializedName("offset") val offset: Int?,
    @SerializedName("total") val total: Int?,
)

/** 계획 목록 한 줄 (Swagger PlanSummary) */
data class PlanSummaryResponse(
    @SerializedName("plan_id") val planId: String,
    @SerializedName("name") val name: String?,
    /** TRAVEL / LEISURE / MOVING, null이면 앱에서 여행 */
    @SerializedName("category") val category: String?,
    @SerializedName("memo") val memo: String?,
    @SerializedName("is_favorite") val isFavorite: Boolean?,
    @SerializedName("vehicle_id") val vehicleId: String?,
    @SerializedName("vehicle_name") val vehicleName: String?,
    /** 운전자 포함 */
    @SerializedName("passenger_count") val passengerCount: Int?,
    @SerializedName("item_type_count") val itemTypeCount: Int?,
    @SerializedName("item_total_quantity") val itemTotalQuantity: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("revision") val revision: Int?,
    @SerializedName("favorited_at") val favoritedAt: String?,
    @SerializedName("item_summaries") val itemSummaries: List<PlanItemSummaryResponse>?,
)

data class PlanItemSummaryResponse(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("name") val name: String?,
    @SerializedName("quantity") val quantity: Int?,
)

/** 목록 정렬 (서버 sort 값) */
enum class PlanSort(val value: String) { LATEST("latest"), FAVORITE("favorite") }
