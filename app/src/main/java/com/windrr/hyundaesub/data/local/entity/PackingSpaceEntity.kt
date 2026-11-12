package com.windrr.hyundaesub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * 배치안이 쓰는 적재 공간 (트렁크, 접은 2열 등). arrangement 1 : N packing_space
 * 2D 배치도에서 공간 테두리를 그리고 배율을 정할 때 쓴다.
 */
@Entity(
    tableName = "packing_space",
    primaryKeys = ["arrangement_id", "space_id"],
    foreignKeys = [
        ForeignKey(
            entity = ArrangementEntity::class,
            parentColumns = ["arrangement_id"],
            childColumns = ["arrangement_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PackingSpaceEntity(
    @ColumnInfo(name = "arrangement_id")
    val arrangementId: String,
    /** 예: TRUNK */
    @ColumnInfo(name = "space_id")
    val spaceId: String,
    /** 예: TRUNK */
    val kind: String,
    @ColumnInfo(name = "width_cm")
    val widthCm: Int,
    @ColumnInfo(name = "depth_cm")
    val depthCm: Int,
    @ColumnInfo(name = "height_cm")
    val heightCm: Int,
    /** 전체 좌표계에서 이 공간의 원점 (뒤쪽 왼쪽 바닥 모서리) */
    @ColumnInfo(name = "origin_x_cm")
    val originXCm: Int,
    @ColumnInfo(name = "origin_y_cm")
    val originYCm: Int,
    @ColumnInfo(name = "origin_z_cm")
    val originZCm: Int,
    /** 이 공간에 포함된 좌석 (접은 시트 공간일 때) */
    @ColumnInfo(name = "seat_codes")
    val seatCodes: List<SeatCode>,
)
