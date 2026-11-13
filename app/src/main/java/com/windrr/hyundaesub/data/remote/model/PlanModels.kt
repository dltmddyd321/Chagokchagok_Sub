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

data class PlanResponse(
    @SerializedName("plan_id") val planId: String,
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
