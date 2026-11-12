package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 내 차. 대표 차량(is_main)은 하나만 둔다 */
@Entity(tableName = "vehicle")
data class VehicleEntity(
    /** plan.vehicle_id가 참조하는 키 (예: "avante_cn8") */
    @PrimaryKey
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,
    /** 대표 차량 여부 */
    @ColumnInfo(name = "is_main")
    val isMain: Boolean,
    /** 예: "아반떼" */
    val name: String,
    /** 예: "5인승 세단". SQL 예약어 DESC와 겹치지 않게 컬럼명은 description */
    @ColumnInfo(name = "description")
    val desc: String,
)
