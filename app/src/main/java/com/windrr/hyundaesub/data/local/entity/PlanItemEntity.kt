package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 계획에 담긴 짐. 크기 단위 cm */
@Entity(
    tableName = "plan_item",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["plan_id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plan_id")],
)
data class PlanItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "plan_id")
    val planId: String,
    val name: String,
    /** 가로 */
    @ColumnInfo(name = "width_cm")
    val widthCm: Int,
    /** 세로 */
    @ColumnInfo(name = "depth_cm")
    val depthCm: Int,
    /** 높이 */
    @ColumnInfo(name = "height_cm")
    val heightCm: Int,
    /** 같은 짐 개수 */
    val quantity: Int,
)
