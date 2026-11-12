package com.windrr.hyundaesub.data.local.entity

/** 계획 분류 (홈 "자주 찾는 내 계획" 카드 상단 표시) */
enum class PlanCategory(val label: String) {
    TRAVEL("여행"),
    LEISURE("레저 및 취미"),
    OUTING("나들이"),
    SHOPPING("장보기·쇼핑"),
    MOVING("이사·운반"),
    ETC("기타"),
}
