package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle")
data class VehicleEntity(
    @PrimaryKey
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: String,
    /** 예: "5인승 세단 · 아반떼" */
    val name: String,
)
