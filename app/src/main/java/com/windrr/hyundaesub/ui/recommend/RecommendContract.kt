package com.windrr.hyundaesub.ui.recommend

data class RecommendUiState(
    val phase: RecommendPhase = RecommendPhase.LOADING,
    /** 같은 화면 안에서 보여줄 단계: 배치 이미지 / 가이드 기다리는 중 / 가이드 */
    val mode: RecommendMode = RecommendMode.MAP,
    val previewId: String? = null,
    /** 탭으로 보여줄 배치안 (rank 순, 최대 [MAX_TABS]개) */
    val layouts: List<ArrangementLayoutUi> = emptyList(),
    val selectedIndex: Int = 0,
    /** 배치안별 적재 안내 (arrangement_id → 상태). 배치가 나오면 바로 모두 요청한다 */
    val guides: Map<String, GuideStatus> = emptyMap(),
    /** 오류 안내 문구 */
    val errorMessage: String? = null,
) {
    val selected: ArrangementLayoutUi? get() = layouts.getOrNull(selectedIndex)

    val selectedGuide: GuideStatus? get() = selected?.let { guides[it.arrangementId] }

    companion object {
        const val MAX_TABS = 3
    }
}

enum class RecommendPhase { LOADING, READY, ERROR }

enum class RecommendMode { MAP, GUIDE_LOADING, GUIDE }

sealed interface GuideStatus {
    data object Loading : GuideStatus

    /** 서버 AI 문구, 또는 서버가 비워 주거나 실패하면 앱이 만든 기본 문구 */
    data class Ready(val steps: List<GuideStepUi>) : GuideStatus
}

sealed interface RecommendIntent {
    data class SelectTab(val index: Int) : RecommendIntent

    /** 통신 오류 후 다시 시도 (같은 Idempotency-Key) */
    data object Retry : RecommendIntent

    /** 확인 / 이 배치 선택: 고른 배치로 저장 단계 */
    data object Confirm : RecommendIntent

    /** 가이드 보기: 기다리는 화면 → 문구가 오면 가이드 */
    data object OpenGuide : RecommendIntent

    /** 기다리는 화면의 진행 표시가 끝남 (문구 도착 후) → 가이드 */
    data object GuideLoadingFinished : RecommendIntent

    /** 기다리는 화면 나가기 / 가이드에서 뒤로 → 배치 이미지 */
    data object BackToMap : RecommendIntent

    /** 새 계획 만들기 (동작은 추후 정의) */
    data object NewPlan : RecommendIntent
}

sealed interface RecommendEffect {
    /** 배치를 못 찾음 → 배치 실패 화면 */
    data class NoLayout(val resultCode: String) : RecommendEffect

    /** 배치 선택 완료 → 저장 단계 (preview_id + arrangement_id로 POST /plans) */
    data class Confirmed(val previewId: String, val arrangementId: String) : RecommendEffect

    data class Toast(val message: String) : RecommendEffect
}
