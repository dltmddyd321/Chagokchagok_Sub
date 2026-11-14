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

// 배치안·공간·짐 위치 모델은 PreviewModels.kt (미리보기와 저장된 계획 계산이 같은 형식)
