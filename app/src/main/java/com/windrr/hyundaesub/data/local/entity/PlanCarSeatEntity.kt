package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/** 카시트가 놓일 좌석 */
@Entity(
    tableName = "plan_car_seat",
    primaryKeys = ["plan_id", "seat_code"],
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["plan_id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlanCarSeatEntity(
    @ColumnInfo(name = "plan_id")
    val planId: String,
    @ColumnInfo(name = "seat_code")
    val seatCode: SeatCode,
)
