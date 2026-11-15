package com.windrr.hyundaesub.measure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssignDimensionsTest {

    @Test
    fun box_heightFirst_isStillHeight() {
        // 높이(수직 45)를 먼저, 그다음 짧은 바닥 40, 긴 바닥 60
        val size = MeasureGeometry.assignDimensions(listOf(45f, 40f, 60f), listOf(0.98f, 0.05f, 0.1f), MeasureShape.BOX)
        assertEquals(BoxSize(60f, 40f, 45f), size)
    }

    @Test
    fun box_noVerticalLine_shortestIsHeight() {
        val size = MeasureGeometry.assignDimensions(listOf(30f, 80f, 50f), listOf(0.1f, 0.2f, 0.3f), MeasureShape.BOX)
        assertEquals(BoxSize(80f, 50f, 30f), size)
    }

    @Test
    fun cylinder_verticalIsHeight() {
        val size = MeasureGeometry.assignDimensions(listOf(90f, 35f), listOf(0.95f, 0.02f), MeasureShape.CYLINDER)
        assertEquals(BoxSize(35f, 35f, 90f), size)
    }

    @Test
    fun notEnoughLines_isNull() {
        assertNull(MeasureGeometry.assignDimensions(listOf(30f, 80f), listOf(0.1f, 0.9f), MeasureShape.BOX))
    }
}
