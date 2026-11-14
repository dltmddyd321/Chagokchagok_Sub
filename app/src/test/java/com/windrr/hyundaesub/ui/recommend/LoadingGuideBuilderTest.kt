package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.data.remote.model.LoadingGuideResponse
import com.windrr.hyundaesub.data.remote.model.LoadingGuideStepResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoadingGuideBuilderTest {

    private val folded = FoldedSeatGroup(listOf(SeatCode.ROW_2_LEFT, SeatCode.ROW_2_CENTER))
    private val layout = ArrangementLayoutUi(
        arrangementId = "a1",
        seats = emptyList(),
        items = listOf(
            ItemBox("p1", "유모차", 0, PxRect(0f, 0f, 1f, 1f), "TRUNK"),
            ItemBox("p2", "캠핑박스", 1, PxRect(0f, 0f, 1f, 1f), "FOLDED_SEAT"),
        ),
        foldedGroups = listOf(folded),
    )

    @Test
    fun foldStepFirst_thenServerStepsByOrder() {
        val guide = LoadingGuideResponse(
            arrangementId = "a1",
            source = "AI",
            reply = "",
            steps = listOf(
                LoadingGuideStepResponse(2, "p2", "캠핑 박스 넣기\n남은 공간에 놓아 주세요."),
                LoadingGuideStepResponse(1, "p1", "유모차를 좌측으로 먼저 넣어주세요\n공간에 맞춰 눕혀주세요."),
            ),
        )
        val steps = LoadingGuideBuilder.build(layout, guide)
        assertEquals(listOf(1, 2, 3), steps.map { it.number })
        assertEquals("왼쪽 뒷좌석을 접어요", steps[0].title)
        assertEquals(folded, steps[0].foldedGroup)
        assertEquals("유모차를 좌측으로 먼저 넣어주세요", steps[1].title)
        assertEquals("공간에 맞춰 눕혀주세요.", steps[1].body)
        assertEquals("p1", steps[1].placementId)
        assertEquals("p2", steps[2].placementId)
    }

    @Test
    fun emptyServerSteps_fallBackToLoadOrder() {
        val guide = LoadingGuideResponse("a1", "FALLBACK", "위 그림을 보고 배치해 주세요.", emptyList())
        val steps = LoadingGuideBuilder.build(layout, guide)
        assertEquals(listOf("왼쪽 뒷좌석을 접어요", "유모차 넣기", "캠핑박스 넣기"), steps.map { it.title })
        assertEquals(listOf(null, "p1", "p2"), steps.map { it.placementId })
    }

    @Test
    fun failedGuide_alsoFallsBack() {
        assertEquals(3, LoadingGuideBuilder.build(layout, null).size)
    }

    @Test
    fun splitText_singleLine_hasNoBody() {
        val (title, body) = LoadingGuideBuilder.splitText("  유모차 넣기  ")
        assertEquals("유모차 넣기", title)
        assertNull(body)
    }
}
