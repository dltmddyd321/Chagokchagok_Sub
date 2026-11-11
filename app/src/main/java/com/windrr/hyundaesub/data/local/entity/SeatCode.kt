package com.windrr.hyundaesub.data.local.entity

/** 좌석 코드. Pleos SDK(SeatValue) 이름과 동일해서 차량 센서 값과 바로 비교할 수 있다. */
enum class SeatCode {
    ROW_1_LEFT, // 운전석
    ROW_1_CENTER,
    ROW_1_RIGHT, // 조수석
    ROW_2_LEFT,
    ROW_2_CENTER,
    ROW_2_RIGHT,
    ROW_3_LEFT,
    ROW_3_CENTER,
    ROW_3_RIGHT,
}
