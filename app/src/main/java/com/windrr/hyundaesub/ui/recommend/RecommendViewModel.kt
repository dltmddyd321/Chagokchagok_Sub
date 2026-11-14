package com.windrr.hyundaesub.ui.recommend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.remote.ApiResult
import com.windrr.hyundaesub.data.remote.PlanRemoteRepository
import com.windrr.hyundaesub.data.remote.model.PreviewRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 배치 추천: 화면에 들어올 때 한 번 POST /calculations/preview.
 * 조건이 바뀌면 상위 흐름이 새 ViewModel(새 키)을 만들므로, 여기서 만든 Idempotency-Key는 이 요청 하나에만 쓴다.
 */
class RecommendViewModel(
    private val remote: PlanRemoteRepository = PlanRemoteRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(RecommendUiState())
    val state: StateFlow<RecommendUiState> = _state.asStateFlow()

    private val _effect = Channel<RecommendEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var request: PreviewRequest? = null

    /** 이 계산 요청의 키. 처리 결과를 받거나 재시도를 마칠 때까지만 같은 값을 쓴다 */
    private var idempotencyKey: String = UUID.randomUUID().toString()

    /** 화면 진입 시 한 번. 같은 ViewModel에 다시 불려도 무시 */
    fun start(input: RecommendInput) {
        if (request != null) return
        request = PreviewRequestBuilder.build(input)
        load()
    }

    fun onIntent(intent: RecommendIntent) {
        when (intent) {
            is RecommendIntent.SelectTab -> _state.update { current ->
                if (intent.index !in current.layouts.indices) {
                    current
                } else {
                    val next = current.copy(selectedIndex = intent.index)
                    // 가이드를 보던 중 문구가 아직 안 온 배치로 바꾸면 기다리는 화면으로
                    if (next.mode == RecommendMode.GUIDE && next.selectedGuide !is GuideStatus.Ready) {
                        next.copy(mode = RecommendMode.GUIDE_LOADING)
                    } else {
                        next
                    }
                }
            }
            RecommendIntent.Retry -> load()
            RecommendIntent.Confirm -> {
                val state = _state.value
                val previewId = state.previewId ?: return
                val arrangement = state.selected ?: return
                send(RecommendEffect.Confirmed(previewId, arrangement.arrangementId))
            }
            // 문구가 이미 와 있어도 기다리는 화면을 거친다 (진행 표시가 빨리 끝나고 가이드로)
            RecommendIntent.OpenGuide -> _state.update {
                if (it.phase == RecommendPhase.READY) it.copy(mode = RecommendMode.GUIDE_LOADING) else it
            }
            RecommendIntent.GuideLoadingFinished -> _state.update {
                if (it.mode == RecommendMode.GUIDE_LOADING && it.selectedGuide is GuideStatus.Ready) {
                    it.copy(mode = RecommendMode.GUIDE)
                } else {
                    it
                }
            }
            RecommendIntent.BackToMap -> _state.update { it.copy(mode = RecommendMode.MAP) }
            // TODO: 새 계획 만들기 동작은 추후 정의
            RecommendIntent.NewPlan -> Unit
        }
    }

    private fun load() {
        val body = request ?: return
        _state.update { it.copy(phase = RecommendPhase.LOADING, errorMessage = null) }
        viewModelScope.launch {
            when (val result = remote.previewCalculation(body, idempotencyKey)) {
                is ApiResult.Success -> {
                    val calculation = result.data.calculation
                    val snapshotItems = calculation.inputSnapshot?.items.orEmpty()
                    val layouts = calculation.arrangements.orEmpty()
                        .sortedBy { it.rank }
                        .take(RecommendUiState.MAX_TABS)
                        .map { ArrangementLayoutMapper.map(it, snapshotItems) }
                    if (calculation.resultCode == RESULT_LAYOUT_FOUND && layouts.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                phase = RecommendPhase.READY,
                                previewId = result.data.previewId,
                                layouts = layouts,
                                selectedIndex = 0,
                                guides = layouts.associate { layout -> layout.arrangementId to GuideStatus.Loading },
                            )
                        }
                        // 배치가 나오면 곧장 배치안마다 적재 안내 요청 (계산보다 오래 걸릴 수 있음)
                        layouts.forEach { layout -> loadGuide(result.data.previewId, layout) }
                    } else {
                        Log.i(TAG, "배치 없음: ${calculation.resultCode} required=${calculation.requiredInputs}")
                        send(RecommendEffect.NoLayout(calculation.resultCode))
                    }
                }
                is ApiResult.Failure -> {
                    Log.w(TAG, "배치 계산 실패: $result")
                    // 서버가 요청을 거절(4xx)했으면 같은 키로 다시 보내도 결과가 같다 → 새 키
                    if (result.httpCode in 400..499) idempotencyKey = UUID.randomUUID().toString()
                    showError(result.message ?: "배치를 계산하지 못했어요.")
                }
                is ApiResult.NetworkError -> {
                    Log.w(TAG, "배치 계산 통신 오류", result.cause)
                    // 서버가 처리했는지 모르므로 같은 키로 재시도
                    showError("네트워크 연결을 확인하고 다시 시도해 주세요.")
                }
            }
        }
    }

    /** 적재 안내 요청 (배치마다 새 Idempotency-Key). 서버가 단계를 비워 주거나 실패하면 앱이 싣는 순서로 기본 문구를 만든다 */
    private fun loadGuide(previewId: String, layout: ArrangementLayoutUi) {
        viewModelScope.launch {
            val guide = when (val result = remote.loadingGuide(previewId, layout.arrangementId)) {
                is ApiResult.Success -> result.data.also {
                    Log.d(TAG, "적재 안내 ${layout.arrangementId}: source=${it.source} steps=${it.steps?.size}")
                }
                else -> {
                    Log.w(TAG, "적재 안내 실패 ${layout.arrangementId}: $result")
                    null
                }
            }
            val steps = LoadingGuideBuilder.build(layout, guide)
            _state.update { it.copy(guides = it.guides + (layout.arrangementId to GuideStatus.Ready(steps))) }
        }
    }

    private fun showError(message: String) {
        _state.update { it.copy(phase = RecommendPhase.ERROR, errorMessage = message) }
    }

    private fun send(effect: RecommendEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private companion object {
        const val TAG = "Recommend"
        const val RESULT_LAYOUT_FOUND = "LAYOUT_FOUND"
    }
}
