package com.windrr.hyundaesub.ui.item

import com.windrr.hyundaesub.ui.component.sizeLabelOf

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
    /** 이번 배치 추천에 포함할지 (짐 선택 목록의 체크박스) */
    val selected: Boolean = true,
    /**
     * 꼭 가져가야 하는 짐인지 (배치 실패 후 "가져갈 짐 조정"에서 체크).
     * 서버 required_quantity = required면 quantity, 아니면 0 → 못 실으면 이 짐부터 뺀다
     */
    val required: Boolean = false,
) {
    val sizeLabel: String get() = sizeLabelOf(widthCm, depthCm, heightCm)

    companion object {
        const val MAX_QUANTITY = 20
    }
}
