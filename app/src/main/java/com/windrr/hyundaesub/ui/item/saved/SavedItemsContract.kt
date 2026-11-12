package com.windrr.hyundaesub.ui.item.saved

import com.windrr.hyundaesub.ui.component.sizeLabelOf
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 사용자가 저장해 둔 물건 ("자주 싣는 짐") */
data class SavedItem(
    val id: String,
    val name: String,
    val widthCm: Int,
    val depthCm: Int,
    val heightCm: Int,
) {
    val sizeLabel: String get() = sizeLabelOf(widthCm, depthCm, heightCm)
}

/** 저장한 물건 목록 출처. DB 테이블 만들면 Room 구현으로 교체 */
fun interface SavedItemsSource {
    fun observeAll(): Flow<List<SavedItem>>
}

/** TODO: saved_item 테이블 생기기 전까지 빈 목록 */
object EmptySavedItemsSource : SavedItemsSource {
    override fun observeAll(): Flow<List<SavedItem>> = flowOf(emptyList())
}

data class SavedItemsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val allItems: List<SavedItem> = emptyList(),
    /** 체크한 물건 id */
    val selectedIds: Set<String> = emptySet(),
    /** 물건별 수량 (없으면 1) */
    val quantities: Map<String, Int> = emptyMap(),
) {
    val visibleItems: List<SavedItem>
        get() = if (query.isBlank()) allItems else allItems.filter { it.name.contains(query.trim(), ignoreCase = true) }

    /** 저장한 물건이 하나도 없음 (검색 결과 없음과 구분) */
    val isEmpty: Boolean get() = !isLoading && allItems.isEmpty()

    val isSearchEmpty: Boolean get() = !isLoading && allItems.isNotEmpty() && visibleItems.isEmpty()

    val canSave: Boolean get() = selectedIds.isNotEmpty()

    fun quantityOf(id: String): Int = quantities[id] ?: 1
}

sealed interface SavedItemsIntent {
    data class QueryChanged(val query: String) : SavedItemsIntent

    data class ToggleItem(val id: String) : SavedItemsIntent

    /** + 버튼. 체크 안 된 물건이면 체크도 같이 */
    data class Increase(val id: String) : SavedItemsIntent

    /** − 버튼. 최소 1 */
    data class Decrease(val id: String) : SavedItemsIntent

    data object Save : SavedItemsIntent
}

sealed interface SavedItemsEffect {
    data class Done(val items: List<PickedItem>) : SavedItemsEffect
}
