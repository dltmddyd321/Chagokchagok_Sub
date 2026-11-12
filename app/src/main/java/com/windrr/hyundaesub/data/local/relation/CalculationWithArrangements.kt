package com.windrr.hyundaesub.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.windrr.hyundaesub.data.local.entity.ArrangementEntity
import com.windrr.hyundaesub.data.local.entity.CalculationEntity
import com.windrr.hyundaesub.data.local.entity.PackingSpaceEntity
import com.windrr.hyundaesub.data.local.entity.PlacementEntity

/** 배치안 1개 + 적재 공간 + 짐 배치 목록 */
data class ArrangementWithPlacements(
    @Embedded
    val arrangement: ArrangementEntity,
    @Relation(parentColumn = "arrangement_id", entityColumn = "arrangement_id")
    val spaces: List<PackingSpaceEntity>,
    @Relation(parentColumn = "arrangement_id", entityColumn = "arrangement_id")
    val placements: List<PlacementEntity>,
)

/** 계산 결과 1회 + 배치안 목록 (rank 순 정렬은 [sortedArrangements]) */
data class CalculationWithArrangements(
    @Embedded
    val calculation: CalculationEntity,
    @Relation(
        entity = ArrangementEntity::class,
        parentColumn = "calculation_id",
        entityColumn = "calculation_id",
    )
    val arrangements: List<ArrangementWithPlacements>,
) {
    val sortedArrangements: List<ArrangementWithPlacements>
        get() = arrangements.sortedBy { it.arrangement.rank }
}
