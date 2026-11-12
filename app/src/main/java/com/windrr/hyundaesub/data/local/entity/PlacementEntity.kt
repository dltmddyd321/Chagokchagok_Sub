package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 짐 배치 (짐 1개당 1행 — 수량 2개면 2행). arrangement 1 : N placement
 * 좌표는 회전 후 상자의 원점쪽(뒤·왼·바닥) 모서리, cm.
 */
@Entity(
    tableName = "placement",
    foreignKeys = [
        ForeignKey(
            entity = ArrangementEntity::class,
            parentColumns = ["arrangement_id"],
            childColumns = ["arrangement_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlanItemEntity::class,
            parentColumns = ["item_id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("arrangement_id"), Index("item_id")],
)
data class PlacementEntity(
    @PrimaryKey
    @ColumnInfo(name = "placement_id")
    val placementId: String,
    @ColumnInfo(name = "arrangement_id")
    val arrangementId: String,
    /** plan_item 참조 — 원래 치수·이름은 여기서 가져온다 */
    @ColumnInfo(name = "item_id")
    val itemId: String,
    /** 같은 짐 중 몇 번째인지 (1부터) */
    @ColumnInfo(name = "unit_index")
    val unitIndex: Int,
    /** 어느 적재 공간에 놓였는지 (packing_space.space_id) */
    @ColumnInfo(name = "space_id")
    val spaceId: String,
    @ColumnInfo(name = "x_cm")
    val xCm: Int,
    @ColumnInfo(name = "y_cm")
    val yCm: Int,
    /** 1차는 무적층이라 항상 0 */
    @ColumnInfo(name = "z_cm")
    val zCm: Int,
    val orientation: Orientation,
    /** 싣는 순서 (1부터) */
    @ColumnInfo(name = "load_order")
    val loadOrder: Int,
)
