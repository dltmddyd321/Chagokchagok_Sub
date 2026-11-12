package com.windrr.hyundaesub.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.dummy.DummyPlans
import com.windrr.hyundaesub.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            DummyPlans.seedIfNeeded(application, db)
        }
        viewModelScope.launch {
            combine(db.planDao().observePlans(), db.planDao().observeFavoritePlans()) { recent, favorites ->
                val today = LocalDate.now()
                HomeUiState(
                    isLoading = false,
                    recentPlans = recent.map { PlanCardMapper.map(it, today) },
                    favoritePlans = favorites.map { PlanCardMapper.map(it, today) },
                )
            }.collect { next -> _state.update { next } }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ToggleFavorite -> viewModelScope.launch {
                val current = _state.value.recentPlans.firstOrNull { it.planId == intent.planId } ?: return@launch
                db.planDao().setFavorite(intent.planId, !current.isFavorite)
            }
        }
    }
}
