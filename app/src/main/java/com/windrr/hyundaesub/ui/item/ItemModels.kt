package com.windrr.hyundaesub.ui.item

/** 짐을 어디서 가져올지 */
enum class ItemSource { NEW, SAVED }

/**
 * 이번 계획에 담은 짐 (화면용). 계획 저장 시 plan_item으로 바뀐다.
 * savedItemId: 등록된 물건에서 불러왔으면 그 id, 새로 추가했으면 null
 */
data class PickedItem(
    val key: String,
    val name: String,
    val widthCm: Int,
    val depthCm: Int,
    val heightCm: Int,
    val quantity: Int = 1,
    val savedItemId: String? = null,
) {
    val sizeLabel: String get() = "${widthCm}×${depthCm}×${heightCm}cm"
}
