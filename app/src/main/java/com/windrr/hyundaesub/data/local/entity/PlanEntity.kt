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
    indices = [Index("vehicle_id"), Index("created_at")],
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
    /** 계획 이름 (예: "주말 나들이") */
    val name: String = "",
    /** 카테고리. 항상 있음, 기본 여행 */
    val category: PlanCategory = PlanCategory.DEFAULT,
    /** 출발 날짜 "yyyy-MM-dd" (D-day 표시용). 정하지 않았으면 null */
    @ColumnInfo(name = "trip_date")
    val tripDate: String? = null,
    /** 즐겨찾기 — 홈 "자주 찾는 내 계획"에 노출 */
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    /** 등록 시각 (epoch ms) — 최근 등록 순 정렬 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
