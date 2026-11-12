package com.windrr.hyundaesub.ui.seat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.local.entity.SeatCode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SeatSelectViewModel : ViewModel() {

    private val _state = MutableStateFlow(SeatSelectUiState())
    val state: StateFlow<SeatSelectUiState> = _state.asStateFlow()

    private val _effect = Channel<SeatSelectEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var started = false

    /** 화면 진입 시 한 번만: 다시 선택할 때 기존 좌석을 미리 체크 */
    fun start(initialSeats: Set<SeatCode>) {
        if (started) return
        started = true
        _state.update { it.copy(selectedSeats = initialSeats intersect AvanteSeats.SELECTABLE.toSet()) }
    }

    fun onIntent(intent: SeatSelectIntent) {
        when (intent) {
            is SeatSelectIntent.ToggleSeat -> {
                if (intent.seat !in AvanteSeats.SELECTABLE) return
                _state.update {
                    val seats = it.selectedSeats
                    it.copy(selectedSeats = if (intent.seat in seats) seats - intent.seat else seats + intent.seat)
                }
            }
            SeatSelectIntent.Next -> viewModelScope.launch {
                _effect.send(SeatSelectEffect.GoNext(_state.value.selectedSeats))
            }
        }
    }
}
