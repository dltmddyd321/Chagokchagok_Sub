package com.windrr.hyundaesub.measure

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
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
data class BoxSize(val widthCm: Float?, val depthCm: Float?, val heightCm: Float?) {
    val isComplete: Boolean get() = widthCm != null && depthCm != null && heightCm != null
}

/** 바닥 직사각형 네 꼭짓점 (A → B → C → D 순서로 한 바퀴) */
data class FloorRect(val a: Vec3, val b: Vec3, val c: Vec3, val d: Vec3) {
    val corners: List<Vec3> get() = listOf(a, b, c, d)

    /** A→B 수평 길이 (m) */
    val width: Float get() = MeasureGeometry.horizontalDistance(a, b)

    /** B→C 수평 길이 (m) */
    val depth: Float get() = MeasureGeometry.horizontalDistance(b, c)

    fun containsHorizontally(p: Vec3, margin: Float): Boolean {
        val ab = b - a
        val ad = d - a
        val abLength = ab.length()
        val adLength = ad.length()
        if (abLength < MeasureGeometry.MIN_EDGE_M || adLength < MeasureGeometry.MIN_EDGE_M) return false
        val rel = Vec3(p.x - a.x, 0f, p.z - a.z)
        val u = (rel dot ab) / abLength
        val v = (rel dot ad) / adLength
        return u in -margin..(abLength + margin) && v in -margin..(adLength + margin)
    }

    /** 같은 바닥 꼭짓점을 height(m)만큼 올린 윗면 */
    fun top(height: Float): List<Vec3> = corners.map { it + Vec3.UP * height }
}

/** 측정할 물건의 바닥 모양 */
enum class MeasureShape {
    /** 상자·캐리어처럼 각진 물건: 바닥 꼭짓점 3개 → 직사각형 */
    BOX,

    /** 원통·둥근 받침: 바닥 둘레 3점 → 원 (지름 × 지름 바운딩 박스로 싣는다) */
    CYLINDER,
}

/** 바닥 원. center는 바닥 높이(y)에 있다 */
data class FloorCircle(val center: Vec3, val radius: Float) {
    val diameter: Float get() = radius * 2f

    /** 둘레를 n등분한 점 (시작 각도 startAngle, 라디안) */
    fun rim(n: Int, startAngle: Float = 0f, height: Float = 0f): List<Vec3> = List(n) { i ->
        val t = startAngle + (2.0 * Math.PI * i / n).toFloat()
        Vec3(center.x + radius * cos(t), center.y + height, center.z + radius * sin(t))
    }

    /** p가 위에서 봤을 때 원 안(+여유)인지 */
    fun containsHorizontally(p: Vec3, margin: Float): Boolean =
        MeasureGeometry.horizontalDistance(center, p) <= radius + margin
}

object MeasureGeometry {

    /** 바닥 위 두 점의 수평 거리 (높이 차는 무시) */
    fun horizontalDistance(a: Vec3, b: Vec3): Float {
        val d = b - a
        return sqrt(d.x * d.x + d.z * d.z)
    }

    /**
     * 시선과 수평면(y = planeY)의 교점. 시선이 아래를 향하지 않거나 카메라 뒤쪽이면 null.
     * 물건 바닥 꼭짓점은 항상 바닥 위에 있으므로, 표면 인식 대신 바닥 평면과 교차시키면
     * 물건 옆면·특징점에 잘못 찍히는 일 없이 터치한 곳 바로 그 바닥 지점이 잡힌다.
     */
    fun intersectHorizontalPlane(ray: Ray, planeY: Float): Vec3? {
        val d = ray.direction.normalized()
        if (d.y > -1e-4f) return null
        val t = (planeY - ray.origin.y) / d.y
        if (t <= 0f) return null
        return ray.origin + d * t
    }

    /**
     * 바닥 꼭짓점 세 개를 찍은 순서와 상관없이 직사각형으로.
     * 세 점 중 두 변이 이루는 각이 직각에 가장 가까운 점을 모서리(B)로 보고 A·B·C를 다시 정한다.
     * (대각선 두 점 + 한 점처럼 어떤 순서로 찍어도 같은 상자가 나온다)
     */
    fun rectifyFloorAnyOrder(p0: Vec3, p1: Vec3, p2: Vec3): FloorRect {
        val pts = listOf(p0, p1, p2)
        fun flat(v: Vec3) = Vec3(v.x, 0f, v.z)
        val corner = pts.indices.minByOrNull { i ->
            val o = flat(pts[i])
            val u = (flat(pts[(i + 1) % 3]) - o).normalized()
            val v = (flat(pts[(i + 2) % 3]) - o).normalized()
            kotlin.math.abs(u dot v)
        } ?: 1
        val a = pts[(corner + 1) % 3]
        val c = pts[(corner + 2) % 3]
        return rectifyFloor(a, pts[corner], c)
    }

    /**
     * 바닥 둘레 위 세 점을 지나는 원 (위에서 본 x·z 평면의 외접원, 높이는 A 기준).
     * 세 점이 거의 한 줄이면 원을 정할 수 없어 null.
     * 지름 두 점을 정확히 맞히기 어려워서, 둘레 아무 데나 세 점을 찍게 하고 원을 역산한다.
     */
    fun circleThrough(a: Vec3, b: Vec3, c: Vec3): FloorCircle? {
        // A 기준 상대좌표로 계산해 큰 월드 좌표에서의 정밀도 손실을 줄인다
        val bx = b.x - a.x
        val bz = b.z - a.z
        val cx = c.x - a.x
        val cz = c.z - a.z
        val d = 2f * (bx * cz - bz * cx)
        val scale = maxOf(bx * bx + bz * bz, cx * cx + cz * cz)
        if (scale < MIN_EDGE_M * MIN_EDGE_M || kotlin.math.abs(d) < COLLINEAR_EPS * scale) return null
        val b2 = bx * bx + bz * bz
        val c2 = cx * cx + cz * cz
        val ux = (cz * b2 - bz * c2) / d
        val uz = (bx * c2 - cx * b2) / d
        val center = Vec3(a.x + ux, a.y, a.z + uz)
        return FloorCircle(center, sqrt(ux * ux + uz * uz))
    }

    /** 세 점이 한 줄로 판단되는 기준 (넓이 비율) */
    private const val COLLINEAR_EPS = 0.05f

    /**
     * 사용자가 찍은 바닥 세 점 A·B·C를 직사각형으로 바로잡는다.
     * - A→B를 가로 방향으로 고정
     * - C는 A→B에 수직인 성분만 남겨 C'로 (손 떨림으로 비스듬히 찍혀도 직각 유지)
     * - D' = A + (C' − B), 네 꼭짓점 모두 A의 바닥 높이(y)로 맞춘다
     * A·B가 거의 같은 점이면 직각을 정할 수 없어 C를 그대로 쓴다.
     */
    fun rectifyFloor(a: Vec3, b: Vec3, c: Vec3): FloorRect {
        val floorY = a.y
        val flatA = Vec3(a.x, floorY, a.z)
        val flatB = Vec3(b.x, floorY, b.z)
        val flatC = Vec3(c.x, floorY, c.z)
        val ab = flatB - flatA
        val bc = flatC - flatB
        val abLength = ab.length()
        val side = if (abLength < MIN_EDGE_M) {
            bc
        } else {
            val u = ab * (1f / abLength)
            bc - u * (bc dot u)
        }
        return FloorRect(flatA, flatB, flatB + side, flatA + side)
    }

    /** 바닥 직사각형을 정하기에 너무 짧은 변 (m) */
    const val MIN_EDGE_M = 0.005f

    /**
     * base에서 위로 세운 수직선과 카메라 시선이 가장 가까워지는 지점의 높이(m).
     * 깊이 센서 없이 "시선 ↔ 수직선" 교차로 높이를 구한다.
     * 시선이 수직선과 거의 평행하거나, 카메라 뒤쪽·바닥 아래를 가리키면 null.
     */
    fun heightAlongVertical(base: Vec3, ray: Ray): Float? = verticalApproach(base, ray)?.first

    fun topOnNearestVertical(bases: List<Vec3>, ray: Ray): Vec3? =
        bases.mapNotNull { base -> verticalApproach(base, ray)?.let { (height, gap) -> base + Vec3.UP * height to gap } }
            .minByOrNull { it.second }?.first

    private fun verticalApproach(base: Vec3, ray: Ray): Pair<Float, Float>? {
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
        if (t <= 0f || s <= 0f) return null
        val onVertical = base + u * t
        val onRay = ray.origin + v * s
        return t to (onVertical - onRay).length()
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
