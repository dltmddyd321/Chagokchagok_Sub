package com.windrr.hyundaesub.measure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MeasureGeometryTest {

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    )

    @Test
    fun horizontalDistance_ignoresHeight() {
        val d = MeasureGeometry.horizontalDistance(Vec3(0f, 0f, 0f), Vec3(0.3f, 0.5f, 0.4f))
        assertEquals(0.5f, d, 1e-5f)
    }

    @Test
    fun height_rayCrossingVerticalLine() {
        // 카메라 (0, 0.5, 1)에서 수직선 위 높이 0.3m 지점을 바라봄
        val ray = Ray(Vec3(0f, 0.5f, 1f), Vec3(0f, -0.2f, -1f))
        val h = MeasureGeometry.heightAlongVertical(Vec3(0f, 0f, 0f), ray)
        assertNotNull(h)
        assertEquals(0.3f, h!!, 1e-4f)
    }

    @Test
    fun height_rayMissingLine_returnsClosestHeight() {
        // 수직선 옆 10cm를 지나가는 시선 → 가장 가까운 높이 0.3m
        val ray = Ray(Vec3(0.1f, 0.5f, 1f), Vec3(0f, -0.2f, -1f))
        assertEquals(0.3f, MeasureGeometry.heightAlongVertical(Vec3(0f, 0f, 0f), ray)!!, 1e-4f)
    }

    @Test
    fun height_rayPointingBelowFloor_isNull() {
        val ray = Ray(Vec3(0f, 0.5f, 1f), Vec3(0f, -1f, -0.5f))
        assertNull(MeasureGeometry.heightAlongVertical(Vec3(0f, 0f, 0f), ray))
    }

    @Test
    fun height_verticalRay_isNull() {
        val ray = Ray(Vec3(0f, 1f, 0f), Vec3(0f, -1f, 0f))
        assertNull(MeasureGeometry.heightAlongVertical(Vec3(0f, 0f, 0f), ray))
    }

    @Test
    fun height_lineBehindCamera_isNull() {
        val ray = Ray(Vec3(0f, 0.5f, 1f), Vec3(0f, 0.2f, 1f))
        assertNull(MeasureGeometry.heightAlongVertical(Vec3(0f, 0f, 0f), ray))
    }

    @Test
    fun screenRay_centerOfScreen_identity() {
        val ray = MeasureGeometry.rayFromScreen(50f, 50f, 100, 100, identity)
        assertEquals(0f, ray.origin.x, 1e-5f)
        assertEquals(0f, ray.origin.y, 1e-5f)
        assertEquals(-1f, ray.origin.z, 1e-5f)
        assertEquals(1f, ray.direction.z, 1e-5f)
    }

    @Test
    fun project_thenUnproject_roundTrip() {
        val p = Vec3(0.25f, -0.5f, 0f)
        val (sx, sy) = MeasureGeometry.projectToScreen(p, identity, 200, 100)!!
        assertEquals(125f, sx, 1e-3f)
        assertEquals(75f, sy, 1e-3f)
        val ray = MeasureGeometry.rayFromScreen(sx, sy, 200, 100, identity)
        assertEquals(p.x, ray.origin.x, 1e-4f)
        assertEquals(p.y, ray.origin.y, 1e-4f)
    }

    @Test
    fun roundUpForFit_addsMarginAndRoundsUp() {
        assertEquals(20f, MeasureGeometry.roundUpForFit(18.4f, marginCm = 1f), 0f)
        assertEquals(25f, MeasureGeometry.roundUpForFit(19.5f, marginCm = 1f), 0f)
        assertEquals(60f, MeasureGeometry.roundUpForFit(55.3f, marginCm = 1f), 0f)
    }
}
