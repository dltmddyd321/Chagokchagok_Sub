package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.data.remote.model.CalculationRequest
import com.windrr.hyundaesub.data.remote.model.CalculationResponse
import com.windrr.hyundaesub.data.remote.model.PlanCreateRequest
import com.windrr.hyundaesub.data.remote.model.PlanListResponse
import com.windrr.hyundaesub.data.remote.model.PlanResponse
import com.windrr.hyundaesub.data.remote.model.PlanSort
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

    companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
    }
}
