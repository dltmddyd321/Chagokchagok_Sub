package com.windrr.hyundaesub.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PlanCardMapperTest {

    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun dDay_futureToday_andPast() {
        assertEquals("D-3", PlanCardMapper.dDay("2026-11-15", today))
        assertEquals("D-DAY", PlanCardMapper.dDay("2026-11-12", today))
        assertNull(PlanCardMapper.dDay("2026-11-11", today))
        assertNull(PlanCardMapper.dDay(null, today))
        assertNull(PlanCardMapper.dDay("잘못된 날짜", today))
    }

    @Test
    fun itemsShort_threeOrLess_listsAll() {
        assertEquals("캐리어 2, 보드 2", PlanCardMapper.itemsShort(listOf("캐리어" to 2, "보드" to 2)))
    }

    @Test
    fun itemsShort_moreThanThree_collapsesRest() {
        val items = listOf("캐리어" to 1, "캠핑의자" to 2, "배낭" to 1, "아이스박스" to 1, "텐트" to 1)
        assertEquals("캐리어 1, 캠핑의자 2, 배낭 외 2", PlanCardMapper.itemsShort(items))
    }
}
