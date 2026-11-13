package com.windrr.hyundaesub.data.local.entity

/**
 * 계획 분류. 순서 = 계획 저장 화면 칩 순서 (시안: 여행 · 레저 및 취미 · 이사 및 운반이 먼저).
 * DB에는 이름(name)으로 저장되므로 순서를 바꿔도 기존 데이터에 영향 없음.
 */
enum class PlanCategory(val label: String) {
    TRAVEL("여행"),
    LEISURE("레저 및 취미"),
    MOVING("이사 및 운반"),
    OUTING("나들이"),
    SHOPPING("장보기 및 쇼핑"),
    ETC("기타"),
}
