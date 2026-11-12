package com.windrr.hyundaesub.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun onIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            MainIntent.OpenMeasure -> _state.update { it.copy(isMeasuring = true) }
            MainIntent.CloseMeasure -> _state.update { it.copy(isMeasuring = false) }
            MainIntent.OpenFrequentItems -> _state.update { it.copy(selectedTab = MainTab.MY_ITEMS) }
        }
    }
}
