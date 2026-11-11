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

    /** 계획 하나를 짐·카시트까지 통째로 저장. 같은 plan_id가 있으면 짐·카시트 목록을 새 값으로 교체 */
    @Transaction
    open suspend fun savePlan(
        plan: PlanEntity,
        items: List<PlanItemEntity>,
        carSeats: List<PlanCarSeatEntity>,
    ) {
        upsertPlan(plan)
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

    @Transaction
    @Query("SELECT * FROM `plan`")
    abstract fun observePlans(): Flow<List<PlanWithDetails>>

    /** 짐·카시트는 CASCADE로 함께 삭제 */
    @Query("DELETE FROM `plan` WHERE plan_id = :planId")
    abstract suspend fun deletePlan(planId: String)

    @Upsert
    protected abstract suspend fun upsertPlan(plan: PlanEntity)

    @Insert
    protected abstract suspend fun insertItems(items: List<PlanItemEntity>)

    @Insert
    protected abstract suspend fun insertCarSeats(carSeats: List<PlanCarSeatEntity>)

    @Query("DELETE FROM plan_item WHERE plan_id = :planId")
    protected abstract suspend fun deleteItems(planId: String)

    @Query("DELETE FROM plan_car_seat WHERE plan_id = :planId")
    protected abstract suspend fun deleteCarSeats(planId: String)
}
