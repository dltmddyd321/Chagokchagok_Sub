package com.windrr.hyundaesub.ui.recommend

data class RecommendUiState(
    val phase: RecommendPhase = RecommendPhase.LOADING,
    val previewId: String? = null,
    /** 탭으로 보여줄 배치안 (rank 순, 최대 [MAX_TABS]개) */
    val layouts: List<ArrangementLayoutUi> = emptyList(),
    val selectedIndex: Int = 0,
    /** 오류 안내 문구 */
    val errorMessage: String? = null,
) {
    val selected: ArrangementLayoutUi? get() = layouts.getOrNull(selectedIndex)

    companion object {
        const val MAX_TABS = 3
    }
}

enum class RecommendPhase { LOADING, READY, ERROR }

sealed interface RecommendIntent {
    data class SelectTab(val index: Int) : RecommendIntent

    /** 통신 오류 후 다시 시도 (같은 Idempotency-Key) */
    data object Retry : RecommendIntent

    /** 확인: 고른 배치로 저장 단계 */
    data object Confirm : RecommendIntent

    /** 가이드 보기 */
    data object OpenGuide : RecommendIntent
}

sealed interface RecommendEffect {
    /** 배치를 못 찾음 → 배치 실패 화면 */
    data class NoLayout(val resultCode: String) : RecommendEffect

    /** 배치 선택 완료 → 저장 단계 (preview_id + arrangement_id로 POST /plans) */
    data class Confirmed(val previewId: String, val arrangementId: String) : RecommendEffect

    data class Toast(val message: String) : RecommendEffect
}
