package com.windrr.hyundaesub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.windrr.hyundaesub.data.local.entity.PlanCarSeatEntity
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.relation.PlanWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlanDao {

    /**
     * 계획 하나를 짐·카시트까지 통째로 저장. 같은 plan_id가 있으면 짐·카시트 목록을 새 값으로 교체.
     * 입력이 바뀌면 예전 배치 결과는 맞지 않으므로 계산 결과도 함께 지운다.
     */
    @Transaction
    open suspend fun savePlan(
        plan: PlanEntity,
        items: List<PlanItemEntity>,
        carSeats: List<PlanCarSeatEntity>,
    ) {
        upsertPlan(plan)
        deleteCalculations(plan.planId)
        deleteItems(plan.planId)
        deleteCarSeats(plan.planId)
        insertItems(items)
        insertCarSeats(carSeats)
    }

    @Transaction
    @Query("SELECT * FROM `plan` WHERE plan_id = :planId")
    abstract suspend fun getPlan(planId: String): PlanWithDetails?

    @Transaction
    @Query("SELECT * FROM `plan` WHERE plan_id = :planId")
    abstract fun observePlan(planId: String): Flow<PlanWithDetails?>

    /** 최근 등록 순 */
    @Transaction
    @Query("SELECT * FROM `plan` ORDER BY created_at DESC")
    abstract fun observePlans(): Flow<List<PlanWithDetails>>

    /** 즐겨찾기한 계획만, 최근 등록 순 */
    @Transaction
    @Query("SELECT * FROM `plan` WHERE is_favorite = 1 ORDER BY created_at DESC")
    abstract fun observeFavoritePlans(): Flow<List<PlanWithDetails>>

    @Query("SELECT COUNT(*) FROM `plan`")
    abstract suspend fun countPlans(): Int

    @Query("UPDATE `plan` SET is_favorite = :favorite WHERE plan_id = :planId")
    abstract suspend fun setFavorite(planId: String, favorite: Boolean)

    /** 짐·카시트는 CASCADE로 함께 삭제 */
    @Query("DELETE FROM `plan` WHERE plan_id = :planId")
    abstract suspend fun deletePlan(planId: String)

    @Upsert
    protected abstract suspend fun upsertPlan(plan: PlanEntity)

    @Insert
    protected abstract suspend fun insertItems(items: List<PlanItemEntity>)

    @Insert
    protected abstract suspend fun insertCarSeats(carSeats: List<PlanCarSeatEntity>)

    @Query("DELETE FROM calculation WHERE plan_id = :planId")
    protected abstract suspend fun deleteCalculations(planId: String)

    @Query("DELETE FROM plan_item WHERE plan_id = :planId")
    protected abstract suspend fun deleteItems(planId: String)

    @Query("DELETE FROM plan_car_seat WHERE plan_id = :planId")
    protected abstract suspend fun deleteCarSeats(planId: String)
}
