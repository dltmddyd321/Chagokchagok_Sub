package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

data class CalculationRequest(
    @SerializedName("current_folded_segments") val currentFoldedSegments: List<String>? = null,
    @SerializedName("occupied_seats") val occupiedSeats: List<String>? = null,
)

data class CalculationResponse(
    @SerializedName("calculation_id") val calculationId: String,
    @SerializedName("plan_id") val planId: String?,
    @SerializedName("is_success") val isSuccess: Boolean?,
    @SerializedName("result_code") val resultCode: String?,
    @SerializedName("search_complete") val searchComplete: Boolean?,
    @SerializedName("current_folded_segments") val currentFoldedSegments: List<String>?,
    @SerializedName("occupied_seats") val occupiedSeats: List<String>?,
    @SerializedName("vehicle_data_version") val vehicleDataVersion: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("arrangements") val arrangements: List<ArrangementResponse>?,
)

data class ArrangementResponse(
    @SerializedName("arrangement_id") val arrangementId: String,
    @SerializedName("rank") val rank: Int?,
    @SerializedName("passenger_seats") val passengerSeats: List<String>?,
    @SerializedName("target_folded_segments") val targetFoldedSegments: List<String>?,
    @SerializedName("seat_change_count") val seatChangeCount: Int?,
    @SerializedName("packing_space") val packingSpace: PackingSpaceResponse?,
    @SerializedName("score") val score: Double?,
    @SerializedName("placements") val placements: List<PlacementResponse>?,
)

data class PackingSpaceResponse(
    @SerializedName("width_cm") val widthCm: Double?,
    @SerializedName("depth_cm") val depthCm: Double?,
    @SerializedName("height_cm") val heightCm: Double?,
)

data class PlacementResponse(
    @SerializedName("placement_id") val placementId: String,
    @SerializedName("item_id") val itemId: String?,
    @SerializedName("x_cm") val xCm: Double?,
    @SerializedName("y_cm") val yCm: Double?,
    @SerializedName("z_cm") val zCm: Double?,
    @SerializedName("orientation") val orientation: String?,
    @SerializedName("load_order") val loadOrder: Int?,
)
