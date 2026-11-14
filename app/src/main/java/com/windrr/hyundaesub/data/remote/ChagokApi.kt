package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.data.remote.model.CalculationRequest
import com.windrr.hyundaesub.data.remote.model.CalculationResponse
import com.windrr.hyundaesub.data.remote.model.DeviceRegistrationRequest
import com.windrr.hyundaesub.data.remote.model.DeviceResponse
import com.windrr.hyundaesub.data.remote.model.PlanCreateRequest
import com.windrr.hyundaesub.data.remote.model.PlanListResponse
import com.windrr.hyundaesub.data.remote.model.PlanResponse
import com.windrr.hyundaesub.data.remote.model.PlanSort
import com.windrr.hyundaesub.data.remote.model.PreviewRequest
import com.windrr.hyundaesub.data.remote.model.PreviewResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChagokApi {

    @POST("api/v1/plans")
    suspend fun createPlan(
        @Header(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @Body request: PlanCreateRequest,
    ): Response<ApiResponse<PlanResponse>>

    @POST("api/v1/plans/{plan_id}/calculations")
    suspend fun createCalculation(
        @Header(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @Path("plan_id") planId: String,
        @Body request: CalculationRequest,
    ): Response<ApiResponse<CalculationResponse>>

    /**
     * 계획 목록. 홈: 최근 등록 = sort=latest, 자주 찾는 내 계획 = favorite=true
     * (Swagger GET /api/v1/plans: limit 1~100, offset, sort latest|favorite, q 검색어, category, favorite)
     */
    @GET("api/v1/plans")
    suspend fun getPlans(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("sort") sort: String = PlanSort.LATEST.value,
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
        @Query("favorite") favorite: Boolean? = null,
    ): Response<ApiResponse<PlanListResponse>>

    /** 저장 전 배치 미리 계산 (새 계획). 결과의 preview_id로 POST /plans 저장 */
    @POST("api/v1/calculations/preview")
    suspend fun previewCalculation(
        @Header(IDEMPOTENCY_KEY_HEADER) idempotencyKey: String,
        @Body request: PreviewRequest,
    ): Response<ApiResponse<PreviewResponse>>

    /** 폰 FCM 토큰 등록·갱신. device_id는 앱이 만든 UUID (같은 기기면 계속 같은 값) */
    @PUT("api/v1/notifications/devices/{device_id}")
    suspend fun registerDevice(
        @Path("device_id") deviceId: String,
        @Header(NOTIFICATION_KEY_HEADER) notificationKey: String?,
        @Body request: DeviceRegistrationRequest,
    ): Response<ApiResponse<DeviceResponse>>

    /** 폰 FCM 토큰 해제 */
    @DELETE("api/v1/notifications/devices/{device_id}")
    suspend fun unregisterDevice(
        @Path("device_id") deviceId: String,
        @Header(NOTIFICATION_KEY_HEADER) notificationKey: String?,
    ): Response<ApiResponse<DeviceResponse>>

    companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

        /** 알림 API 선택 헤더. 값이 null이면 Retrofit이 헤더를 빼고 보낸다 */
        const val NOTIFICATION_KEY_HEADER = "X-Notification-Key"
    }
}
