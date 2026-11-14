package com.windrr.hyundaesub.ui.plan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.remote.ApiResult
import com.windrr.hyundaesub.data.remote.PlanRemoteRepository
import com.windrr.hyundaesub.data.remote.model.PlanMetadataRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 계획 저장 (서버):
 * 1) POST /plans {source_preview_id, selected_arrangement_id} → plan_id, revision
 * 2) PATCH /plans/{plan_id}/metadata {expected_revision, name, category, memo}
 * 2)에서 실패하면 다시 저장할 때 1)은 건너뛰고 2)만 다시 보낸다 (계획이 두 번 생기지 않게).
 * 각 요청의 Idempotency-Key는 그 요청이 끝날 때(성공)까지 재시도에 같은 값을 쓴다.
 */
class PlanSaveViewModel(
    private val remote: PlanRemoteRepository = PlanRemoteRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(PlanSaveUiState())
    val state: StateFlow<PlanSaveUiState> = _state.asStateFlow()

    private val _effect = Channel<PlanSaveEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var source: PlanSaveSource? = null

    private val createKey = UUID.randomUUID().toString()
    private var metadataKey = UUID.randomUUID().toString()

    /** 1) 성공 후 기억 (2) 재시도용) */
    private var createdPlanId: String? = null
    private var createdRevision: Int = 1

    /** 2)에 보낸 내용. 내용이 바뀌면 다른 요청이라 새 키 */
    private var lastMetadata: PlanMetadataRequest? = null

    fun start(source: PlanSaveSource) {
        if (this.source == null) this.source = source
    }

    fun onIntent(intent: PlanSaveIntent) {
        when (intent) {
            is PlanSaveIntent.NameChanged -> _state.update {
                it.copy(name = intent.name.replace("\n", "").take(PlanSaveUiState.MAX_NAME_LENGTH))
            }
            is PlanSaveIntent.SelectCategory -> _state.update { it.copy(category = intent.category) }
            is PlanSaveIntent.MemoChanged -> _state.update {
                it.copy(memo = intent.memo.take(PlanSaveUiState.MAX_MEMO_LENGTH))
            }
            PlanSaveIntent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        val from = source
        if (!s.canSave || from == null) return
        val meta = PlanMeta(name = s.name.trim(), category = s.category, memo = s.memo.trim().ifEmpty { null })
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val planId = createdPlanId ?: when (val created = remote.createPlanFromResult(from.previewId, from.arrangementId, createKey)) {
                is ApiResult.Success -> created.data.also {
                    createdPlanId = it.planId
                    createdRevision = it.revision ?: 1
                }.planId
                else -> return@launch fail("계획 저장 실패", created)
            }
            val request = PlanMetadataRequest(
                expectedRevision = createdRevision,
                name = meta.name,
                category = meta.category.name,
                memo = meta.memo,
            )
            if (request != lastMetadata) {
                metadataKey = UUID.randomUUID().toString()
                lastMetadata = request
            }
            when (val updated = remote.updatePlanMetadata(planId, request, metadataKey)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _effect.send(PlanSaveEffect.Saved(planId))
                }
                else -> fail("계획 이름 저장 실패", updated)
            }
        }
    }

    private suspend fun fail(what: String, result: ApiResult<*>) {
        Log.w(TAG, "$what: $result")
        _state.update { it.copy(isSaving = false) }
        val message = when (result) {
            is ApiResult.NetworkError -> "네트워크 연결을 확인하고 다시 저장해 주세요."
            is ApiResult.Failure -> result.message ?: "저장하지 못했어요. 다시 시도해 주세요."
            else -> "저장하지 못했어요. 다시 시도해 주세요."
        }
        _effect.send(PlanSaveEffect.Failed(message))
    }

    private companion object {
        const val TAG = "PlanSave"
    }
}
