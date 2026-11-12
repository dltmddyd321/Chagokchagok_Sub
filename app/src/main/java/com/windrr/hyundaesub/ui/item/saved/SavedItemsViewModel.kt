package com.windrr.hyundaesub.ui.item.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.windrr.hyundaesub.data.local.AppDatabase
import androidx.lifecycle.viewModelScope
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedItemsViewModel(application: Application) : AndroidViewModel(application) {

    private val source: SavedItemsSource = RoomSavedItemsSource(AppDatabase.getInstance(application).savedItemDao())

    private val _state = MutableStateFlow(SavedItemsUiState())
    val state: StateFlow<SavedItemsUiState> = _state.asStateFlow()

    private val _effect = Channel<SavedItemsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            source.observeAll().collect { items ->
                _state.update { state ->
                    val ids = items.map { it.id }.toSet()
                    state.copy(
                        isLoading = false,
                        allItems = items,
                        // 목록에서 사라진 물건은 선택에서도 뺀다
                        selectedIds = state.selectedIds intersect ids,
                        quantities = state.quantities.filterKeys { it in ids },
                    )
                }
            }
        }
    }

    fun onIntent(intent: SavedItemsIntent) {
        when (intent) {
            is SavedItemsIntent.QueryChanged -> _state.update { it.copy(query = intent.query) }
            is SavedItemsIntent.ToggleItem -> _state.update {
                val ids = it.selectedIds
                it.copy(selectedIds = if (intent.id in ids) ids - intent.id else ids + intent.id)
            }
            is SavedItemsIntent.Increase -> _state.update {
                val next = (it.quantityOf(intent.id) + 1).coerceAtMost(MAX_QUANTITY)
                it.copy(
                    quantities = it.quantities + (intent.id to next),
                    selectedIds = it.selectedIds + intent.id,
                )
            }
            is SavedItemsIntent.Decrease -> _state.update {
                val next = (it.quantityOf(intent.id) - 1).coerceAtLeast(1)
                it.copy(quantities = it.quantities + (intent.id to next))
            }
            SavedItemsIntent.Save -> save()
        }
    }

    private fun save() {
        val state = _state.value
        if (!state.canSave) return
        val picked = state.allItems
            .filter { it.id in state.selectedIds }
            .map {
                PickedItem(
                    key = "saved-${it.id}",
                    name = it.name,
                    widthCm = it.widthCm,
                    depthCm = it.depthCm,
                    heightCm = it.heightCm,
                    quantity = state.quantityOf(it.id),
                    savedItemId = it.id,
                )
            }
        viewModelScope.launch { _effect.send(SavedItemsEffect.Done(picked)) }
    }

    private companion object {
        const val MAX_QUANTITY = 20
    }
}
