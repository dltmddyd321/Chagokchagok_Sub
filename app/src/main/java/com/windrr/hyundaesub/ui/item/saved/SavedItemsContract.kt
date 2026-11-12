package com.windrr.hyundaesub.ui.item.saved

import com.windrr.hyundaesub.data.local.dao.SavedItemDao
import com.windrr.hyundaesub.ui.component.sizeLabelOf
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 사용자가 저장해 둔 물건 ("자주 싣는 짐") */
data class SavedItem(
    val id: String,
    val name: String,
    val widthCm: Int,
    val depthCm: Int,
    val heightCm: Int,
    /** 불러올 때 기본 수량 */
    val defaultQuantity: Int = 1,
) {
    val sizeLabel: String get() = sizeLabelOf(widthCm, depthCm, heightCm)
}

/** 저장한 물건 목록 출처. DB 테이블 만들면 Room 구현으로 교체 */
fun interface SavedItemsSource {
    fun observeAll(): Flow<List<SavedItem>>
}

/** 내 짐 테이블(saved_item)에서 최근 등록 순으로 */
class RoomSavedItemsSource(private val dao: SavedItemDao) : SavedItemsSource {
    override fun observeAll(): Flow<List<SavedItem>> = dao.observeAll().map { list ->
        list.map {
            SavedItem(
                id = it.savedItemId,
                name = it.name,
                widthCm = it.widthCm,
                depthCm = it.depthCm,
                heightCm = it.heightCm,
                defaultQuantity = it.defaultQuantity,
            )
        }
    }
}

data class SavedItemsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val allItems: List<SavedItem> = emptyList(),
    /** 체크한 물건 id */
    val selectedIds: Set<String> = emptySet(),
    /** 물건별로 바꾼 수량 (없으면 그 물건의 기본 수량) */
    val quantities: Map<String, Int> = emptyMap(),
) {
    val visibleItems: List<SavedItem>
        get() = if (query.isBlank()) allItems else allItems.filter { it.name.contains(query.trim(), ignoreCase = true) }

    /** 저장한 물건이 하나도 없음 (검색 결과 없음과 구분) */
    val isEmpty: Boolean get() = !isLoading && allItems.isEmpty()

    val isSearchEmpty: Boolean get() = !isLoading && allItems.isNotEmpty() && visibleItems.isEmpty()

    val canSave: Boolean get() = selectedIds.isNotEmpty()

    fun quantityOf(id: String): Int =
        quantities[id] ?: allItems.firstOrNull { it.id == id }?.defaultQuantity ?: 1
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
