package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 배치안 (추천 순 리스트). calculation 1 : N arrangement */
@Entity(
    tableName = "arrangement",
    foreignKeys = [
        ForeignKey(
            entity = CalculationEntity::class,
            parentColumns = ["calculation_id"],
            childColumns = ["calculation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("calculation_id")],
)
data class ArrangementEntity(
    @PrimaryKey
    @ColumnInfo(name = "arrangement_id")
    val arrangementId: String,
    @ColumnInfo(name = "calculation_id")
    val calculationId: String,
    /** 1 = 1순위 */
    val rank: Int,
    /** 사람이 앉을 좌석 (json 배열) */
    @ColumnInfo(name = "passenger_seats")
    val passengerSeats: List<SeatCode>,
    /** 이 배치에 필요한 접힘 구간 (json 배열) — 폴딩 안내용 */
    @ColumnInfo(name = "target_folded_segments")
    val targetFoldedSegments: List<String>,
    /** 좌석 이동 수. 서버가 안 주면 null */
    @ColumnInfo(name = "seat_change_count")
    val seatChangeCount: Int?,
    /** 정렬 점수. 1차 서버는 null (rank로 정렬) */
    val score: Double?,
    /** 적재 프로필 (예: TRUNK_ONLY) */
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    /** 짐이 전부 트렁크 안에 들어갔는지 */
    @ColumnInfo(name = "all_in_trunk")
    val allInTrunk: Boolean,
    /** 못 실은 짐 개수 (부분 적재 시) */
    @ColumnInfo(name = "excluded_total_quantity")
    val excludedTotalQuantity: Int,
    /** 추천 이유 코드 (예: NO_SEAT_CHANGE, ALL_IN_TRUNK) — 배지 표시용 */
    @ColumnInfo(name = "reason_codes")
    val reasonCodes: List<String>,
)
