package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 서버 빈패킹 계산 1회 결과. plan 1 : N calculation */
@Entity(
    tableName = "calculation",
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
data class CalculationEntity(
    @PrimaryKey
    @ColumnInfo(name = "calculation_id")
    val calculationId: String,
    @ColumnInfo(name = "plan_id")
    val planId: String,
    /** 배치안이 하나라도 성립했는지 */
    @ColumnInfo(name = "is_success")
    val isSuccess: Boolean,
    /** LAYOUT_FOUND / NO_LAYOUT_FOUND / TIME_LIMIT / NO_VALID_SEAT_CONFIGURATION — 실패 안내 문구용 */
    @ColumnInfo(name = "result_code")
    val resultCode: String,
    /** 차량에서 확인한 현재 접힌 구간 (json 배열) */
    @ColumnInfo(name = "current_folded_segments")
    val currentFoldedSegments: List<String>,
    /** 서버 생성 시각, ISO-8601 UTC 문자열 (문자열 정렬 = 시간순) */
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)
