package com.windrr.hyundaesub.data.local.entity

/**
 * 짐 회전 코드. 세 글자는 회전 후 x(가로), y(앞뒤), z(높이) 방향에 놓이는 원래 치수(W/D/H).
 * 예: HWD → x = 원래 높이, y = 원래 가로, z = 원래 세로
 */
enum class Orientation {
    WDH,
    WHD,
    DWH,
    DHW,
    HWD,
    HDW,
    ;

    /** 원래 치수(가로·세로·높이)를 회전 후 (x, y, z) 길이로 바꾼다 */
    fun rotate(widthCm: Int, depthCm: Int, heightCm: Int): Triple<Int, Int, Int> {
        fun pick(c: Char) = when (c) {
            'W' -> widthCm
            'D' -> depthCm
            else -> heightCm
        }
        return Triple(pick(name[0]), pick(name[1]), pick(name[2]))
    }

    companion object {
        /** 허용 방향을 지정하지 않으면 6방향 모두 허용 (서버 정책과 동일) */
        val ALL: List<Orientation> = entries.toList()

        /**
         * 허용 방향 정책: 6개 중 중복 없는 1개 이상. 비어 있으면(생략) 6방향 기본값.
         * 순서는 enum 순서로 맞춰 같은 집합이면 항상 같은 값이 저장되게 한다.
         */
        fun normalizeAllowed(value: Collection<Orientation>?): List<Orientation> =
            value?.toSet()?.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal } ?: ALL
    }
}
