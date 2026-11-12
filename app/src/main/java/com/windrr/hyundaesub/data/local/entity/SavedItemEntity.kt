package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 내 짐 ("자주 싣는 짐"). 계획과 무관하게 저장해 두고 짐 선택 때 불러온다.
 * 서버 응답 중 transport_state, material은 1차에서 쓰지 않아 저장하지 않는다.
 */
@Entity(
    tableName = "saved_item",
    indices = [Index("created_at")],
)
data class SavedItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "saved_item_id")
    val savedItemId: String,
    val name: String,
    /** 가로 (cm) */
    @ColumnInfo(name = "width_cm")
    val widthCm: Int,
    /** 세로 (cm) */
    @ColumnInfo(name = "depth_cm")
    val depthCm: Int,
    /** 높이 (cm) */
    @ColumnInfo(name = "height_cm")
    val heightCm: Int,
    /** 불러올 때 기본 수량 */
    @ColumnInfo(name = "default_quantity")
    val defaultQuantity: Int = 1,
    /**
     * 실을 때 허용하는 회전 (json 배열). 계획 짐으로 담을 때 서버에 같이 보낸다.
     * 6개 중 중복 없는 1개 이상, 지정하지 않으면 6방향 모두 ([Orientation.normalizeAllowed]로 저장·조회 시 보정)
     */
    @ColumnInfo(name = "allowed_orientations")
    val allowedOrientations: List<Orientation> = Orientation.ALL,
    @ColumnInfo(name = "dimension_source")
    val dimensionSource: DimensionSource,
    /** 등록 시각 (epoch ms) — 최근 등록 순 정렬 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /** 세운 채 바닥면만 돌리기 (높이 방향 고정) — 의자·유모차처럼 눕히면 안 되는 짐 */
        val UPRIGHT_ORIENTATIONS = listOf(Orientation.WDH, Orientation.DWH)
    }
}
