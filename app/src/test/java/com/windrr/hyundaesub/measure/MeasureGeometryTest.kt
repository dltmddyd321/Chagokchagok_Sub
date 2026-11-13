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

    @Test
    fun rectifyFloor_alreadyRightAngle_keepsPoints() {
        val rect = MeasureGeometry.rectifyFloor(Vec3(0f, 0f, 0f), Vec3(0.4f, 0f, 0f), Vec3(0.4f, 0f, -0.3f))
        assertEquals(0.4f, rect.width, 1e-5f)
        assertEquals(0.3f, rect.depth, 1e-5f)
        assertEquals(0f, rect.d.x, 1e-5f)
        assertEquals(-0.3f, rect.d.z, 1e-5f)
    }

    @Test
    fun rectifyFloor_slantedC_projectsToRightAngle() {
        // C가 가로 방향으로 10cm 비껴 찍힘 → 세로는 수직 성분 30cm만
        val rect = MeasureGeometry.rectifyFloor(Vec3(0f, 0f, 0f), Vec3(0.4f, 0f, 0f), Vec3(0.5f, 0f, -0.3f))
        assertEquals(0.4f, rect.width, 1e-5f)
        assertEquals(0.3f, rect.depth, 1e-5f)
        assertEquals(0.4f, rect.c.x, 1e-5f)
        // 네 변이 직각: AB · BC = 0
        assertEquals(0f, (rect.b - rect.a) dot (rect.c - rect.b), 1e-6f)
    }

    @Test
    fun rectifyFloor_flattensToFloorOfA() {
        val rect = MeasureGeometry.rectifyFloor(Vec3(0f, 0.01f, 0f), Vec3(0.4f, -0.02f, 0f), Vec3(0.4f, 0.03f, -0.3f))
        rect.corners.forEach { assertEquals(0.01f, it.y, 1e-6f) }
    }

    @Test
    fun floorRect_topRaisesAllCorners() {
        val rect = MeasureGeometry.rectifyFloor(Vec3(0f, 0f, 0f), Vec3(0.4f, 0f, 0f), Vec3(0.4f, 0f, -0.3f))
        val top = rect.top(0.25f)
        assertEquals(4, top.size)
        top.forEach { assertEquals(0.25f, it.y, 1e-6f) }
    }

    @Test
    fun circleThrough_threeRimPoints_findsCenterAndRadius() {
        // 중심 (1, 0, 2), 반지름 0.25m 원 위의 세 점
        val c = Vec3(1f, 0f, 2f)
        val r = 0.25f
        fun rim(deg: Double) = Vec3(c.x + r * Math.cos(Math.toRadians(deg)).toFloat(), 0f, c.z + r * Math.sin(Math.toRadians(deg)).toFloat())
        val circle = MeasureGeometry.circleThrough(rim(10.0), rim(130.0), rim(250.0))
        assertNotNull(circle)
        assertEquals(1f, circle!!.center.x, 1e-4f)
        assertEquals(2f, circle.center.z, 1e-4f)
        assertEquals(0.5f, circle.diameter, 1e-4f)
    }

    @Test
    fun circleThrough_closeRimPoints_stillExact() {
        // 둘레 한쪽(90도 안)에 몰려 찍어도 원은 같다
        val r = 0.2f
        fun rim(deg: Double) = Vec3(r * Math.cos(Math.toRadians(deg)).toFloat(), 0f, r * Math.sin(Math.toRadians(deg)).toFloat())
        assertEquals(0.4f, MeasureGeometry.circleThrough(rim(0.0), rim(45.0), rim(90.0))!!.diameter, 1e-4f)
    }

    @Test
    fun circleThrough_collinear_isNull() {
        assertNull(MeasureGeometry.circleThrough(Vec3(0f, 0f, 0f), Vec3(0.2f, 0f, 0f), Vec3(0.4f, 0f, 0.001f)))
    }

    @Test
    fun circleThrough_usesFloorOfA() {
        val circle = MeasureGeometry.circleThrough(Vec3(0.2f, 0.03f, 0f), Vec3(0f, -0.01f, 0.2f), Vec3(-0.2f, 0f, 0f))!!
        assertEquals(0.03f, circle.center.y, 1e-6f)
        assertEquals(0.2f, circle.radius, 1e-4f)
    }
}
