package com.windrr.hyundaesub.ui.plan

import com.windrr.hyundaesub.data.local.entity.PlanCategory

data class PlanSaveUiState(
    val name: String = "",
    /** 항상 하나 선택, 기본 여행 */
    val category: PlanCategory = PlanCategory.DEFAULT,
    val memo: String = "",
) {
    /** 이름만 필수 (카테고리는 항상 선택돼 있고, 메모는 선택) */
    val canSave: Boolean get() = name.isNotBlank()

    companion object {
        const val MAX_NAME_LENGTH = 20
        const val MAX_MEMO_LENGTH = 200
    }
}

sealed interface PlanSaveIntent {
    data class NameChanged(val name: String) : PlanSaveIntent

    /** 하나만 선택 (다시 눌러도 해제되지 않음) */
    data class SelectCategory(val category: PlanCategory) : PlanSaveIntent

    data class MemoChanged(val memo: String) : PlanSaveIntent

    data object Save : PlanSaveIntent
}

/** 저장할 계획 정보 (짐·좌석·차량은 상위 흐름이 들고 있다) */
data class PlanMeta(
    val name: String,
    val category: PlanCategory,
    /** 비어 있으면 null */
    val memo: String?,
)

sealed interface PlanSaveEffect {
    data class Saved(val meta: PlanMeta) : PlanSaveEffect
}
