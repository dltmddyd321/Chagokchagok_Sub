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
            is RecommendIntent.SelectTab -> _state.update {
                if (intent.index in it.layouts.indices) it.copy(selectedIndex = intent.index) else it
            }
            RecommendIntent.Retry -> load()
            RecommendIntent.Confirm -> {
                val state = _state.value
                val previewId = state.previewId ?: return
                val arrangement = state.selected ?: return
                send(RecommendEffect.Confirmed(previewId, arrangement.arrangementId))
            }
            // TODO: POST /ai/loading-guides (선택한 배치의 적재 순서 안내) 연결
            RecommendIntent.OpenGuide -> send(RecommendEffect.Toast("적재 가이드는 준비 중이에요."))
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
                            it.copy(phase = RecommendPhase.READY, previewId = result.data.previewId, layouts = layouts, selectedIndex = 0)
                        }
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
