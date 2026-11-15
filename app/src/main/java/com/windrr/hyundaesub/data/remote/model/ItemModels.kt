package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

/** POST /api/v1/items: 내 짐 등록 */
data class ItemCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("default_quantity") val defaultQuantity: Int = 1,
    /** WDH 등. null이면 서버 기본(6방향) */
    @SerializedName("allowed_orientations") val allowedOrientations: List<String>? = null,
    /** AR / MANUAL / PRODUCT_SOURCE / UNSPECIFIED */
    @SerializedName("dimension_source") val dimensionSource: String = "UNSPECIFIED",
)

data class ItemResponse(
    @SerializedName("saved_item_id") val savedItemId: String,
    @SerializedName("name") val name: String,
    @SerializedName("width_cm") val widthCm: Double,
    @SerializedName("depth_cm") val depthCm: Double,
    @SerializedName("height_cm") val heightCm: Double,
    @SerializedName("default_quantity") val defaultQuantity: Int? = null,
    @SerializedName("allowed_orientations") val allowedOrientations: List<String>? = null,
    @SerializedName("dimension_source") val dimensionSource: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    /** 사진 API 경로 (예: /api/v1/items/{id}/photo). 서버 주소 뒤에 붙여 팀 인증으로 연다. 없으면 null */
    @SerializedName("photo_url") val photoUrl: String? = null,
)

data class ItemListResponse(
    @SerializedName("items") val items: List<ItemResponse>?,
    @SerializedName("total") val total: Int? = null,
)

data class ItemPhotoResponse(
    @SerializedName("saved_item_id") val savedItemId: String,
    @SerializedName("photo_url") val photoUrl: String,
)
