package com.windrr.hyundaesub.ui.vehicle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.local.AppDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehicleSelectViewModel(application: Application) : AndroidViewModel(application) {

    private val vehicleDao = AppDatabase.getInstance(application).vehicleDao()

    private val _state = MutableStateFlow(VehicleSelectUiState())
    val state: StateFlow<VehicleSelectUiState> = _state.asStateFlow()

    private val _effect = Channel<VehicleSelectEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            vehicleDao.observeMain().collect { car -> _state.update { it.copy(myCar = car) } }
        }
    }

    fun onIntent(intent: VehicleSelectIntent) {
        when (intent) {
            VehicleSelectIntent.SelectMyCar -> _state.update { it.copy(choice = VehicleChoice.MY_CAR) }
            // TODO: 다른 차량 검색 화면 (1차 범위 밖)
            VehicleSelectIntent.SelectOther -> Unit
            VehicleSelectIntent.Next -> {
                val car = _state.value.myCar ?: return
                if (!_state.value.canProceed) return
                viewModelScope.launch { _effect.send(VehicleSelectEffect.GoNext(car.vehicleId)) }
            }
        }
    }
}
