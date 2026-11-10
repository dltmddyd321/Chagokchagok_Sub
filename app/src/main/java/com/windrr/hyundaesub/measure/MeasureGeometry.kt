package com.windrr.hyundaesub.measure

import kotlin.math.ceil
import kotlin.math.sqrt

/** ARCore 월드 좌표 (단위 m, y축이 위쪽) */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun length() = sqrt(this dot this)
    fun normalized(): Vec3 = length().let { if (it == 0f) this else this * (1f / it) }

    companion object {
        val UP = Vec3(0f, 1f, 0f)
    }
}

data class Ray(val origin: Vec3, val direction: Vec3)

/** 측정 결과 (cm) */
data class BoxSize(val widthCm: Float?, val depthCm: Float?, val heightCm: Float?)

object MeasureGeometry {

    /** 바닥 위 두 점의 수평 거리 (높이 차는 무시) */
    fun horizontalDistance(a: Vec3, b: Vec3): Float {
        val d = b - a
        return sqrt(d.x * d.x + d.z * d.z)
    }

    /**
     * base에서 위로 세운 수직선과 카메라 시선이 가장 가까워지는 지점의 높이(m).
     * 깊이 센서 없이 "시선 ↔ 수직선" 교차로 높이를 구한다.
     * 시선이 수직선과 거의 평행하거나, 카메라 뒤쪽·바닥 아래를 가리키면 null.
     */
    fun heightAlongVertical(base: Vec3, ray: Ray): Float? {
        val u = Vec3.UP
        val v = ray.direction.normalized()
        val w0 = base - ray.origin
        val b = u dot v
        val denom = 1f - b * b
        if (denom < 1e-4f) return null
        val d = u dot w0
        val e = v dot w0
        val t = (b * e - d) / denom // 수직선 위 위치 = 높이
        val s = (e - b * d) / denom // 시선 위 위치 (양수 = 카메라 앞)
        return if (t > 0f && s > 0f) t else null
    }

    /**
     * 화면 좌표(px) → 월드 시선.
     * invViewProj: (projection × view)의 역행렬, OpenGL 열 우선(column-major) 배열
     */
    fun rayFromScreen(x: Float, y: Float, width: Int, height: Int, invViewProj: FloatArray): Ray {
        val ndcX = 2f * x / width - 1f
        val ndcY = 1f - 2f * y / height
        val near = transformPoint(invViewProj, ndcX, ndcY, -1f)
        val far = transformPoint(invViewProj, ndcX, ndcY, 1f)
        return Ray(near, (far - near).normalized())
    }

    /** 월드 좌표 → 화면 좌표(px). 카메라 뒤쪽이면 null */
    fun projectToScreen(p: Vec3, viewProj: FloatArray, width: Int, height: Int): Pair<Float, Float>? {
        val clip = multiply(viewProj, floatArrayOf(p.x, p.y, p.z, 1f))
        if (clip[3] <= 0f) return null
        val ndcX = clip[0] / clip[3]
        val ndcY = clip[1] / clip[3]
        return (ndcX + 1f) / 2f * width to (1f - ndcY) / 2f * height
    }

    /**
     * 판정용 크기: 측정값 + 여유를 더해 step 단위로 올림.
     * 여유값은 실기기 오차 테스트(물건 5종 × 5회) 후 확정 예정.
     */
    fun roundUpForFit(valueCm: Float, marginCm: Float = DEFAULT_MARGIN_CM, stepCm: Float = 5f): Float =
        ceil((valueCm + marginCm) / stepCm) * stepCm

    const val DEFAULT_MARGIN_CM = 1f

    private fun transformPoint(m: FloatArray, x: Float, y: Float, z: Float): Vec3 {
        val r = multiply(m, floatArrayOf(x, y, z, 1f))
        return Vec3(r[0] / r[3], r[1] / r[3], r[2] / r[3])
    }

    private fun multiply(m: FloatArray, v: FloatArray): FloatArray =
        FloatArray(4) { row -> (0 until 4).sumOf { col -> (m[col * 4 + row] * v[col]).toDouble() }.toFloat() }
}
