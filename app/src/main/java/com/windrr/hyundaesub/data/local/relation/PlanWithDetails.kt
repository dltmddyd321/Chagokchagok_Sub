package com.windrr.hyundaesub.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.windrr.hyundaesub.data.local.entity.PlanCarSeatEntity
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.entity.VehicleEntity

/** 계획 + 차량 + 짐 목록 + 카시트 위치 목록 */
data class PlanWithDetails(
    @Embedded
    val plan: PlanEntity,
    @Relation(parentColumn = "vehicle_id", entityColumn = "vehicle_id")
    val vehicle: VehicleEntity?,
    @Relation(parentColumn = "plan_id", entityColumn = "plan_id")
    val items: List<PlanItemEntity>,
    @Relation(parentColumn = "plan_id", entityColumn = "plan_id")
    val carSeats: List<PlanCarSeatEntity>,
)
