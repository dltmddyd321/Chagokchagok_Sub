package com.windrr.hyundaesub.data.dummy

import android.content.Context
import android.content.pm.ApplicationInfo
import com.windrr.hyundaesub.data.local.AppDatabase
import com.windrr.hyundaesub.data.local.DefaultVehicles
import com.windrr.hyundaesub.data.local.entity.PlanCarSeatEntity
import com.windrr.hyundaesub.data.local.entity.PlanCategory
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.entity.SeatCode
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 홈 화면 확인용 더미 계획 (디버그 빌드에서만, 앱 설치 후 최초 1회).
 * 계획 저장 흐름이 연결되면 제거하거나 꺼도 된다.
 */
object DummyPlans {

    private const val PREFS = "dummy_data"
    private const val KEY_SEEDED = "plans_seeded_v1"

    suspend fun seedIfNeeded(context: Context, db: AppDatabase) {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        // 이미 사용자가 만든 계획이 있으면 건드리지 않는다
        if (db.planDao().countPlans() == 0) {
            db.vehicleDao().upsert(*DefaultVehicles.ALL.toTypedArray())
            plans().forEach { db.planDao().savePlan(it.plan, it.items, it.carSeats) }
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private class Dummy(
        val plan: PlanEntity,
        val items: List<PlanItemEntity>,
        val carSeats: List<PlanCarSeatEntity> = emptyList(),
    )

    private fun plans(): List<Dummy> {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        fun daysAgo(days: Long) = now - TimeUnit.DAYS.toMillis(days)
        fun item(planId: String, index: Int, name: String, w: Int, d: Int, h: Int, qty: Int) =
            PlanItemEntity("$planId-item-$index", planId, name, widthCm = w, depthCm = d, heightCm = h, quantity = qty)

        val vehicleId = DefaultVehicles.AVANTE.vehicleId
        return listOf(
            Dummy(
                plan = PlanEntity(
                    planId = "dummy-plan-outing",
                    vehicleId = vehicleId,
                    passengerCount = 3,
                    name = "주말 나들이",
                    category = PlanCategory.OUTING,
                    tripDate = today.plusDays(3).toString(),
                    createdAt = daysAgo(0),
                ),
                items = listOf(
                    item("dummy-plan-outing", 1, "유모차", 42, 58, 88, 1),
                    item("dummy-plan-outing", 2, "캐리어", 40, 25, 60, 1),
                    item("dummy-plan-outing", 3, "캠핑의자", 15, 15, 85, 2),
                ),
                carSeats = listOf(PlanCarSeatEntity("dummy-plan-outing", SeatCode.ROW_2_RIGHT)),
            ),
            Dummy(
                plan = PlanEntity(
                    planId = "dummy-plan-ikea",
                    vehicleId = vehicleId,
                    passengerCount = 2,
                    name = "이케아",
                    category = PlanCategory.SHOPPING,
                    tripDate = today.plusDays(6).toString(),
                    createdAt = daysAgo(1),
                ),
                items = listOf(item("dummy-plan-ikea", 1, "수납장", 80, 40, 30, 1)),
            ),
            Dummy(
                plan = PlanEntity(
                    planId = "dummy-plan-camping",
                    vehicleId = vehicleId,
                    passengerCount = 4,
                    name = "가족 캠핑",
                    category = PlanCategory.TRAVEL,
                    isFavorite = true,
                    createdAt = daysAgo(5),
                ),
                items = listOf(
                    item("dummy-plan-camping", 1, "캐리어", 40, 25, 60, 1),
                    item("dummy-plan-camping", 2, "캠핑의자", 15, 15, 85, 2),
                    item("dummy-plan-camping", 3, "배낭", 30, 20, 50, 1),
                    item("dummy-plan-camping", 4, "아이스박스", 50, 35, 40, 1),
                    item("dummy-plan-camping", 5, "텐트", 60, 20, 20, 1),
                ),
            ),
            Dummy(
                plan = PlanEntity(
                    planId = "dummy-plan-board",
                    vehicleId = vehicleId,
                    passengerCount = 2,
                    name = "보드여행",
                    category = PlanCategory.LEISURE,
                    isFavorite = true,
                    createdAt = daysAgo(9),
                ),
                items = listOf(
                    item("dummy-plan-board", 1, "캐리어", 40, 25, 60, 2),
                    item("dummy-plan-board", 2, "보드", 30, 15, 155, 2),
                ),
            ),
        )
    }
}
