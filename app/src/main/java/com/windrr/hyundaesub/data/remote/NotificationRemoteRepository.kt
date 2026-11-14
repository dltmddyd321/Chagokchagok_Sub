package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.BuildConfig
import com.windrr.hyundaesub.data.remote.model.DeviceRegistrationRequest
import com.windrr.hyundaesub.data.remote.model.DeviceResponse

/** 모바일 알림: 폰 FCM 토큰 등록·해제 */
class NotificationRemoteRepository(private val api: ChagokApi = ApiClient.chagokApi) {

    /** local.properties의 CHAGOK_NOTIFICATION_KEY. 비어 있으면 헤더 없이 보낸다 */
    private val notificationKey: String? = BuildConfig.CHAGOK_NOTIFICATION_KEY.takeIf { it.isNotBlank() }

    suspend fun registerDevice(deviceId: String, token: String): ApiResult<DeviceResponse> =
        apiCall { api.registerDevice(deviceId, notificationKey, DeviceRegistrationRequest(token = token)) }

    suspend fun unregisterDevice(deviceId: String): ApiResult<DeviceResponse> =
        apiCall { api.unregisterDevice(deviceId, notificationKey) }
}
