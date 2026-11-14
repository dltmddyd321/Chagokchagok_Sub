package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.BuildConfig
import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.ui.item.PickedItem
import com.windrr.hyundaesub.ui.recommend.ArrangementLayoutMapper
import com.windrr.hyundaesub.ui.recommend.PreviewRequestBuilder
import com.windrr.hyundaesub.ui.recommend.RecommendInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/**
 * 실서버 배치 미리 계산 (POST /api/v1/calculations/preview) → 화면 좌표 변환까지.
 * ./gradlew :app:testDebugUnitTest --tests "*PreviewApiLiveTest*"
 */
class PreviewApiLiveTest {

    @Test
    fun preview_findsLayout_andMapsToImage() = runBlocking {
        assumeTrue("서버 계정이 local.properties에 없어 건너뜀", BuildConfig.CHAGOK_API_USERNAME.isNotBlank())
        val input = RecommendInput(
            localVehicleId = "avante_cn8",
            occupiedSeats = emptySet(),
            items = listOf(
                // 여유분 포함 92.4 × 105 × 40 (트렁크 입구 높이 41.5 이하)
                PickedItem(key = "a", name = "큰박스", widthCm = 88, depthCm = 100, heightCm = 38),
                PickedItem(key = "b", name = "긴박스", widthCm = 55, depthCm = 55, heightCm = 40),
            ),
        )
        val result = PlanRemoteRepository().previewCalculation(PreviewRequestBuilder.build(input), UUID.randomUUID().toString())
        println("preview → $result")
        assertTrue("미리 계산 실패: $result", result is ApiResult.Success)
        val calculation = (result as ApiResult.Success).data.calculation
        assertEquals("LAYOUT_FOUND", calculation.resultCode)
        val layout = ArrangementLayoutMapper.map(calculation.arrangements!!.minBy { it.rank }, calculation.inputSnapshot?.items.orEmpty())
        println("seats=${layout.seats} items=${layout.items}")
        assertEquals(2, layout.items.size)
    }
}
