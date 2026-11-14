package com.windrr.hyundaesub.ui.plan

import com.windrr.hyundaesub.data.local.entity.PlanCategory

data class PlanSaveUiState(
    val name: String = "",
    /** 항상 하나 선택, 기본 여행 */
    val category: PlanCategory = PlanCategory.DEFAULT,
    val memo: String = "",
    /** 서버 저장 중 (버튼 잠금) */
    val isSaving: Boolean = false,
) {
    /** 이름만 필수 (카테고리는 항상 선택돼 있고, 메모는 선택) */
    val canSave: Boolean get() = name.isNotBlank() && !isSaving

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

/** 저장할 계획 정보 */
data class PlanMeta(
    val name: String,
    val category: PlanCategory,
    /** 비어 있으면 null */
    val memo: String?,
)

/** 배치 추천에서 고른 결과 (새 계획은 이걸로 서버에 저장) */
data class PlanSaveSource(
    val previewId: String,
    val arrangementId: String,
)

sealed interface PlanSaveEffect {
    /** 서버 저장 완료 */
    data class Saved(val planId: String) : PlanSaveEffect

    data class Failed(val message: String) : PlanSaveEffect
}
