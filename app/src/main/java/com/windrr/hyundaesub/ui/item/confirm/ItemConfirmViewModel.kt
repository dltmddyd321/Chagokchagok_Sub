package com.windrr.hyundaesub.ui.item.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ItemConfirmViewModel : ViewModel() {

    private val _state = MutableStateFlow(ItemConfirmUiState())
    val state: StateFlow<ItemConfirmUiState> = _state.asStateFlow()

    private val _effect = Channel<ItemConfirmEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var started = false

    /** 화면 진입 시 한 번만: 측정값(없으면 직접 입력)으로 채운다 */
    fun start(measured: BoxSize?) {
        if (started) return
        started = true
        _state.value = ItemConfirmUiState.fromMeasure(measured)
    }

    fun onIntent(intent: ItemConfirmIntent) {
        when (intent) {
            is ItemConfirmIntent.NameChanged -> _state.update { it.copy(name = intent.name.take(MAX_NAME_LENGTH)) }
            is ItemConfirmIntent.DimensionChanged -> {
                // 숫자만, 최대 3자리
                val digits = intent.value.filter(Char::isDigit).take(3)
                _state.update {
                    when (intent.dimension) {
                        Dimension.WIDTH -> it.copy(width = digits)
                        Dimension.DEPTH -> it.copy(depth = digits)
                        Dimension.HEIGHT -> it.copy(height = digits)
                    }
                }
            }
            ItemConfirmIntent.Increase -> _state.update {
                it.copy(quantity = (it.quantity + 1).coerceAtMost(ItemConfirmUiState.MAX_QUANTITY))
            }
            ItemConfirmIntent.Decrease -> _state.update { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }
            ItemConfirmIntent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.canSave) return
        val item = PickedItem(
            key = "new-${UUID.randomUUID()}",
            name = s.name.trim(),
            widthCm = s.widthCm!!,
            depthCm = s.depthCm!!,
            heightCm = s.heightCm!!,
            quantity = s.quantity,
        )
        viewModelScope.launch { _effect.send(ItemConfirmEffect.Saved(item)) }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 20
    }
}
