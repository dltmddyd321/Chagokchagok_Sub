package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

/** POST /api/v1/ai/loading-guides 요청. 새 계획은 미리보기 id + 배치 id만 보낸다 */
data class LoadingGuideRequest(
    @SerializedName("preview_id") val previewId: String,
    @SerializedName("arrangement_id") val arrangementId: String,
)

data class LoadingGuideResponse(
    @SerializedName("arrangement_id") val arrangementId: String,
    /** AI / FALLBACK (AI를 못 쓰면 기본 문구, steps가 비어 올 수 있음) */
    @SerializedName("source") val source: String?,
    @SerializedName("reply") val reply: String?,
    @SerializedName("steps") val steps: List<LoadingGuideStepResponse>?,
)

/** 안내 한 단계. placement_id로 배치의 짐과 이어진다 */
data class LoadingGuideStepResponse(
    @SerializedName("order") val order: Int,
    @SerializedName("placement_id") val placementId: String,
    @SerializedName("text") val text: String,
)
