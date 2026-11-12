package com.windrr.hyundaesub.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.windrr.hyundaesub.data.local.entity.PlanCarSeatEntity
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.entity.SeatCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanDaoTest {

    private lateinit var db: AppDatabase

    private val plan = PlanEntity(planId = "plan-1", vehicleId = DefaultVehicles.AVANTE.vehicleId, passengerCount = 3)
    private val carrier = PlanItemEntity("item-1", "plan-1", "캐리어 24인치", widthCm = 42, depthCm = 26, heightCm = 65, quantity = 1)
    private val box = PlanItemEntity("item-2", "plan-1", "캠핑 박스", widthCm = 40, depthCm = 30, heightCm = 30, quantity = 2)

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
        db.vehicleDao().upsert(*DefaultVehicles.ALL.toTypedArray())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savePlan_readsBackWithVehicleItemsAndCarSeats() = runBlocking {
        db.planDao().savePlan(plan, listOf(carrier, box), listOf(PlanCarSeatEntity("plan-1", SeatCode.ROW_2_RIGHT)))

        val saved = db.planDao().getPlan("plan-1")
        assertNotNull(saved)
        assertEquals(DefaultVehicles.AVANTE.name, saved!!.vehicle?.name)
        assertEquals(3, saved.plan.passengerCount)
        assertEquals(listOf("캐리어 24인치", "캠핑 박스"), saved.items.map { it.name }.sorted().reversed())
        assertEquals(2, saved.items.first { it.itemId == "item-2" }.quantity)
        assertEquals(listOf(SeatCode.ROW_2_RIGHT), saved.carSeats.map { it.seatCode })
    }

    @Test
    fun savePlan_again_replacesItemsAndCarSeats() = runBlocking {
        db.planDao().savePlan(plan, listOf(carrier, box), listOf(PlanCarSeatEntity("plan-1", SeatCode.ROW_2_RIGHT)))
        db.planDao().savePlan(plan.copy(passengerCount = 4), listOf(carrier), emptyList())

        val saved = db.planDao().observePlan("plan-1").first()!!
        assertEquals(4, saved.plan.passengerCount)
        assertEquals(listOf("item-1"), saved.items.map { it.itemId })
        assertEquals(0, saved.carSeats.size)
    }

    @Test
    fun deletePlan_cascadesToItemsAndCarSeats() = runBlocking {
        db.planDao().savePlan(plan, listOf(carrier, box), listOf(PlanCarSeatEntity("plan-1", SeatCode.ROW_2_LEFT)))
        db.planDao().deletePlan("plan-1")

        assertNull(db.planDao().getPlan("plan-1"))
        assertEquals(0, count("plan_item"))
        assertEquals(0, count("plan_car_seat"))
    }

    @Test
    fun fileDatabase_seedsDefaultVehicle() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vehicle = AppDatabase.getInstance(context).vehicleDao().get("avante_cn8")
        assertEquals(DefaultVehicles.AVANTE.name, vehicle?.name)
    }

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }
}
