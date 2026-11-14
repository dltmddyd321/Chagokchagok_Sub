package com.windrr.hyundaesub.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.local.AppDatabase
import com.windrr.hyundaesub.data.local.DummyDataCleanup
import com.windrr.hyundaesub.data.remote.ApiResult
import com.windrr.hyundaesub.data.remote.PlanRemoteRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "HomeViewModel"
    }

    private val db = AppDatabase.getInstance(application)
    private val remote = PlanRemoteRepository()

    /** 서버에서 받은 목록을 보여주는 중이면 기기 DB 변화로 덮어쓰지 않는다 */
    @Volatile
    private var showingServer = false

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            DummyDataCleanup.run(db)
        }
        viewModelScope.launch {
            combine(db.planDao().observePlans(), db.planDao().observeFavoritePlans()) { recent, favorites ->
                val today = LocalDate.now()
                HomeUiState(
                    isLoading = false,
                    recentPlans = recent.map { PlanCardMapper.map(it, today) },
                    favoritePlans = favorites.map { PlanCardMapper.map(it, today) },
                )
            }.collect { next -> if (!showingServer) _state.update { next } }
        }
        refreshFromServer()
    }

    /**
     * 서버 조회: 최근 등록(sort=latest) + 즐겨찾기(favorite=true)를 같이 받는다.
     * 둘 다 성공해야 서버 목록으로 바꾸고, 하나라도 실패하면 기기 DB 목록을 그대로 둔다.
     */
    private fun refreshFromServer() {
        viewModelScope.launch {
            val recentCall = async { remote.recentPlans() }
            val favoriteCall = async { remote.favoritePlans() }
            val recent = recentCall.await()
            val favorite = favoriteCall.await()
            if (recent is ApiResult.Success && favorite is ApiResult.Success) {
                showingServer = true
                _state.update {
                    HomeUiState(
                        isLoading = false,
                        recentPlans = recent.data.plans.orEmpty().map(PlanCardMapper::fromServer),
                        favoritePlans = favorite.data.plans.orEmpty().map(PlanCardMapper::fromServer),
                        source = PlanSource.SERVER,
                    )
                }
            } else {
                Log.w(TAG, "서버 계획 조회 실패 → 기기 DB 표시. recent=$recent favorite=$favorite")
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refreshFromServer()
            // TODO: 서버 목록일 때는 PATCH /plans/{id}/metadata (expected_revision 필요)로 바꾸기
            is HomeIntent.ToggleFavorite -> if (!showingServer) viewModelScope.launch {
                val current = _state.value.recentPlans.firstOrNull { it.planId == intent.planId } ?: return@launch
                db.planDao().setFavorite(intent.planId, !current.isFavorite)
            }
        }
    }
}
