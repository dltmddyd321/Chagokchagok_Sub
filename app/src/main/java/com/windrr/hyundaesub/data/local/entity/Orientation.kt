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
}
