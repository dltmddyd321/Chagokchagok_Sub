package com.windrr.hyundaesub.ui.seat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
