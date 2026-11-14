package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 실제 서버 호출 확인 (GET /api/v1/plans).
 * local.properties에 CHAGOK_API_USERNAME / CHAGOK_API_PASSWORD가 있을 때만 돈다 (없으면 건너뜀).
 * ./gradlew :app:testDebugUnitTest --tests "*PlanApiLiveTest*"
 */
class PlanApiLiveTest {

    @Test
    fun getRecentPlans_fromRealServer() = runBlocking {
        assumeTrue("서버 계정이 local.properties에 없어 건너뜀", BuildConfig.CHAGOK_API_USERNAME.isNotBlank())

        val result = PlanRemoteRepository().recentPlans(limit = 5)
        println("GET ${ApiClient.baseUrl(BuildConfig.CHAGOK_API_BASE_URL)}api/v1/plans?limit=5&sort=latest → $result")
        assertTrue("서버 응답이 성공이 아님: $result", result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        println("total=${data.total}, plans=${data.plans?.map { "${it.name}(${it.category}, 짐 ${it.itemTotalQuantity}개)" }}")
    }
}
