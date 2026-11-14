package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.data.remote.model.CalculationRequest
import com.windrr.hyundaesub.data.remote.model.CalculationResponse
import com.windrr.hyundaesub.data.remote.model.PlanCreateRequest
import com.windrr.hyundaesub.data.remote.model.PlanListResponse
import com.windrr.hyundaesub.data.remote.model.PlanResponse
import com.windrr.hyundaesub.data.remote.model.PlanSort
import com.windrr.hyundaesub.data.remote.model.PreviewRequest
import com.windrr.hyundaesub.data.remote.model.PreviewResponse
import java.util.UUID

class PlanRemoteRepository(private val api: ChagokApi = ApiClient.chagokApi) {

    /** 최근 등록 순 계획 */
    suspend fun recentPlans(limit: Int = 20): ApiResult<PlanListResponse> =
        apiCall { api.getPlans(limit = limit, sort = PlanSort.LATEST.value) }

    /** 즐겨찾기한 계획만, 최근 등록 순 */
    suspend fun favoritePlans(limit: Int = 20): ApiResult<PlanListResponse> =
        apiCall { api.getPlans(limit = limit, sort = PlanSort.LATEST.value, favorite = true) }

    suspend fun createPlan(
        request: PlanCreateRequest,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): ApiResult<PlanResponse> = apiCall { api.createPlan(idempotencyKey, request) }

    suspend fun createCalculation(
        planId: String,
        request: CalculationRequest,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): ApiResult<CalculationResponse> = apiCall { api.createCalculation(idempotencyKey, planId, request) }

    /**
     * 배치 미리 계산. [idempotencyKey]는 같은 요청을 재시도할 때만 다시 쓰고,
     * 조건(짐·좌석)을 바꿔 다시 계산할 때는 새로 만든다.
     */
    suspend fun previewCalculation(
        request: PreviewRequest,
        idempotencyKey: String,
    ): ApiResult<PreviewResponse> = apiCall { api.previewCalculation(idempotencyKey, request) }
}
