package com.windrr.hyundaesub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.windrr.hyundaesub.data.local.entity.ArrangementEntity
import com.windrr.hyundaesub.data.local.entity.CalculationEntity
import com.windrr.hyundaesub.data.local.entity.PackingSpaceEntity
import com.windrr.hyundaesub.data.local.entity.PlacementEntity
import com.windrr.hyundaesub.data.local.relation.CalculationWithArrangements
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CalculationDao {

    /**
     * 서버 계산 결과를 통째로 저장. 같은 calculation_id가 있으면 배치안 이하를 새 값으로 교체.
     * plan과 plan_item이 먼저 저장돼 있어야 한다 (FK).
     */
    @Transaction
    open suspend fun saveCalculation(
        calculation: CalculationEntity,
        arrangements: List<ArrangementEntity>,
        spaces: List<PackingSpaceEntity>,
        placements: List<PlacementEntity>,
    ) {
        upsertCalculation(calculation)
        deleteArrangements(calculation.calculationId) // CASCADE로 공간·배치도 삭제
        insertArrangements(arrangements)
        insertSpaces(spaces)
        insertPlacements(placements)
    }

    @Transaction
    @Query("SELECT * FROM calculation WHERE calculation_id = :calculationId")
    abstract suspend fun getCalculation(calculationId: String): CalculationWithArrangements?

    /** 계획의 가장 최근 계산 결과 */
    @Transaction
    @Query("SELECT * FROM calculation WHERE plan_id = :planId ORDER BY created_at DESC LIMIT 1")
    abstract fun observeLatest(planId: String): Flow<CalculationWithArrangements?>

    @Transaction
    @Query("SELECT * FROM calculation WHERE plan_id = :planId ORDER BY created_at DESC LIMIT 1")
    abstract suspend fun getLatest(planId: String): CalculationWithArrangements?

    @Query("DELETE FROM calculation WHERE calculation_id = :calculationId")
    abstract suspend fun deleteCalculation(calculationId: String)

    @Upsert
    protected abstract suspend fun upsertCalculation(calculation: CalculationEntity)

    @Insert
    protected abstract suspend fun insertArrangements(arrangements: List<ArrangementEntity>)

    @Insert
    protected abstract suspend fun insertSpaces(spaces: List<PackingSpaceEntity>)

    @Insert
    protected abstract suspend fun insertPlacements(placements: List<PlacementEntity>)

    @Query("DELETE FROM arrangement WHERE calculation_id = :calculationId")
    protected abstract suspend fun deleteArrangements(calculationId: String)
}
