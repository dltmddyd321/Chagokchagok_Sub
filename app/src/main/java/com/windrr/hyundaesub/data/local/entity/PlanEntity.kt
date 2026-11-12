package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 적재 계획. plan_id는 서버·두 앱 공통 키 */
@Entity(
    tableName = "plan",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["vehicle_id"],
            childColumns = ["vehicle_id"],
        ),
    ],
    indices = [Index("vehicle_id")],
)
data class PlanEntity(
    @PrimaryKey
    @ColumnInfo(name = "plan_id")
    val planId: String,
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,
    /** 동승 인원 (운전자 포함) */
    @ColumnInfo(name = "passenger_count")
    val passengerCount: Int,
    /** 사용자 메모 (없으면 null) */
    val memo: String? = null,
)
