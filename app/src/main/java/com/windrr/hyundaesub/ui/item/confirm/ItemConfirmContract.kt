package com.windrr.hyundaesub.ui.item.confirm

import com.windrr.hyundaesub.measure.BoxSize
import com.windrr.hyundaesub.ui.item.PickedItem
import kotlin.math.ceil

/**
 * 측정값 확인 / 직접 입력 화면 상태.
 * 치수는 입력 중 빈 칸도 허용해야 해서 문자열로 들고, 저장할 때 정수 cm로 바꾼다.
 */
data class ItemConfirmUiState(
    val name: String = "",
    val width: String = "",
    val depth: String = "",
    val height: String = "",
    val quantity: Int = 1,
    /** AR 측정에서 왔는지 (false = 직접 입력) */
    val fromMeasure: Boolean = false,
) {
    val widthCm: Int? get() = width.toCmOrNull()
    val depthCm: Int? get() = depth.toCmOrNull()
    val heightCm: Int? get() = height.toCmOrNull()

    val canSave: Boolean
        get() = name.isNotBlank() && widthCm != null && depthCm != null && heightCm != null

    companion object {
        /** 차에 실을 수 있는 현실적인 한 변 최대 길이 */
        const val MAX_CM = 300
        const val MAX_QUANTITY = 20

        /** AR 측정값(소수 cm) → 화면 값. 작게 재면 안 들어갈 수 있어서 올림 */
        fun fromMeasure(size: BoxSize?): ItemConfirmUiState = ItemConfirmUiState(
            width = size?.widthCm.toCeilText(),
            depth = size?.depthCm.toCeilText(),
            height = size?.heightCm.toCeilText(),
            fromMeasure = size != null,
        )

        private fun Float?.toCeilText(): String = this?.let { ceil(it).toInt().coerceIn(1, MAX_CM).toString() }.orEmpty()
    }
}

private fun String.toCmOrNull(): Int? = toIntOrNull()?.takeIf { it in 1..ItemConfirmUiState.MAX_CM }

enum class Dimension { WIDTH, DEPTH, HEIGHT }

sealed interface ItemConfirmIntent {
    data class NameChanged(val name: String) : ItemConfirmIntent

    data class DimensionChanged(val dimension: Dimension, val value: String) : ItemConfirmIntent

    data object Increase : ItemConfirmIntent

    data object Decrease : ItemConfirmIntent

    data object Save : ItemConfirmIntent
}

sealed interface ItemConfirmEffect {
    data class Saved(val item: PickedItem) : ItemConfirmEffect
}
