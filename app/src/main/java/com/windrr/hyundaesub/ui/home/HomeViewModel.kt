package com.windrr.hyundaesub.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.data.local.AppDatabase
import com.windrr.hyundaesub.data.local.DummyDataCleanup
import com.windrr.hyundaesub.data.remote.ApiResult
import com.windrr.hyundaesub.data.remote.PlanRemoteRepository
import com.windrr.hyundaesub.data.remote.model.PlanMetadataRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "HomeViewModel"
    }

    private val db = AppDatabase.getInstance(application)
    private val remote = PlanRemoteRepository()

    /** 서버에서 받은 목록을 보여주는 중이면 기기 DB 변화로 덮어쓰지 않는다 */
    @Volatile
    private var showingServer = false

    /** 서버 계획별 revision (즐겨찾기 PATCH의 expected_revision) */
    private val revisions = ConcurrentHashMap<String, Int>()

    /** 즐겨찾기 요청 중인 계획 (연타 방지) */
    private val favoriteInFlight = ConcurrentHashMap.newKeySet<String>()

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
                (recent.data.plans.orEmpty() + favorite.data.plans.orEmpty()).forEach { plan ->
                    plan.revision?.let { revisions[plan.planId] = it }
                }
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

    /**
     * 서버 계획 즐겨찾기: PATCH /plans/{id}/metadata {expected_revision, is_favorite}.
     * 화면은 먼저 바꾸고(최근 카드 별 + 즐겨찾기 목록), 끝나면 서버 목록을 다시 받아 순서·상태를 맞춘다.
     * 실패(다른 기기에서 먼저 수정해 revision이 다름 등)해도 서버 목록으로 되돌린다.
     */
    private fun toggleServerFavorite(planId: String) {
        val state = _state.value
        val card = state.recentPlans.firstOrNull { it.planId == planId }
            ?: state.favoritePlans.firstOrNull { it.planId == planId }
            ?: return
        val revision = revisions[planId] ?: return refreshFromServer()
        if (!favoriteInFlight.add(planId)) return
        val favorite = !card.isFavorite
        val updated = card.copy(isFavorite = favorite)
        _state.update { s ->
            s.copy(
                recentPlans = s.recentPlans.map { if (it.planId == planId) updated else it },
                favoritePlans = if (favorite) listOf(updated) + s.favoritePlans.filterNot { it.planId == planId } else s.favoritePlans.filterNot { it.planId == planId },
            )
        }
        viewModelScope.launch {
            val result = remote.updatePlanMetadata(
                planId = planId,
                // 이름·카테고리·메모는 null → 요청에서 빠져 그대로 유지
                request = PlanMetadataRequest(expectedRevision = revision, name = null, category = null, memo = null, isFavorite = favorite),
                idempotencyKey = UUID.randomUUID().toString(),
            )
            when (result) {
                is ApiResult.Success -> result.data.revision?.let { revisions[planId] = it }
                else -> Log.w(TAG, "즐겨찾기 변경 실패 $planId → 서버 목록으로 되돌림: $result")
            }
            favoriteInFlight.remove(planId)
            refreshFromServer()
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refreshFromServer()
            is HomeIntent.ToggleFavorite -> if (showingServer) {
                toggleServerFavorite(intent.planId)
            } else {
                viewModelScope.launch {
                    val current = _state.value.recentPlans.firstOrNull { it.planId == intent.planId } ?: return@launch
                    db.planDao().setFavorite(intent.planId, !current.isFavorite)
                }
            }
        }
    }
}
