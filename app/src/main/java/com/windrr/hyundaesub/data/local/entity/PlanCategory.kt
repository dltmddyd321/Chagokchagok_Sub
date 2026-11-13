package com.windrr.hyundaesub.data.local.entity

/**
 * 계획 카테고리 (서버와 같은 코드 3종). 계획은 항상 카테고리가 있고 기본값은 여행.
 * DB에는 이름(name)으로 저장된다.
 */
enum class PlanCategory(val label: String) {
    TRAVEL("여행"),
    LEISURE("레저 및 취미"),
    MOVING("이사 및 운반"),
    ;

    companion object {
        val DEFAULT = TRAVEL

        /** 서버·DB 코드 → 카테고리. null이거나 모르는 값이면 무조건 여행 */
        fun fromCode(code: String?): PlanCategory = entries.firstOrNull { it.name == code } ?: DEFAULT
    }
}
