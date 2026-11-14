package com.windrr.hyundaesub.data.remote.model

import com.google.gson.annotations.SerializedName

/** PUT /api/v1/notifications/devices/{device_id} 요청. platform은 ANDROID 고정 */
data class DeviceRegistrationRequest(
    @SerializedName("token") val token: String,
    @SerializedName("platform") val platform: String = PLATFORM_ANDROID,
)

/** 기기 등록·해제 응답 */
data class DeviceResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("platform") val platform: String?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("updated_at") val updatedAt: String?,
)

const val PLATFORM_ANDROID = "ANDROID"
