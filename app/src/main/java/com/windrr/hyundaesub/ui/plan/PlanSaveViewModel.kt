package com.windrr.hyundaesub.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlanSaveViewModel : ViewModel() {

    private val _state = MutableStateFlow(PlanSaveUiState())
    val state: StateFlow<PlanSaveUiState> = _state.asStateFlow()

    private val _effect = Channel<PlanSaveEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: PlanSaveIntent) {
        when (intent) {
            is PlanSaveIntent.NameChanged -> _state.update {
                it.copy(name = intent.name.replace("\n", "").take(PlanSaveUiState.MAX_NAME_LENGTH))
            }
            is PlanSaveIntent.SelectCategory -> _state.update { it.copy(category = intent.category) }
            is PlanSaveIntent.MemoChanged -> _state.update {
                it.copy(memo = intent.memo.take(PlanSaveUiState.MAX_MEMO_LENGTH))
            }
            PlanSaveIntent.Save -> {
                val s = _state.value
                if (!s.canSave) return
                val meta = PlanMeta(
                    name = s.name.trim(),
                    category = s.category,
                    memo = s.memo.trim().ifEmpty { null },
                )
                viewModelScope.launch { _effect.send(PlanSaveEffect.Saved(meta)) }
            }
        }
    }
}
