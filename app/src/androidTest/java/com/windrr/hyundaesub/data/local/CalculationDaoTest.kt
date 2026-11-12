package com.windrr.hyundaesub.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.windrr.hyundaesub.data.local.entity.ArrangementEntity
import com.windrr.hyundaesub.data.local.entity.CalculationEntity
import com.windrr.hyundaesub.data.local.entity.Orientation
import com.windrr.hyundaesub.data.local.entity.PackingSpaceEntity
import com.windrr.hyundaesub.data.local.entity.PlacementEntity
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.entity.SeatCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 서버 계산 결과 예시(예시 상자 30×40×20 2개, 트렁크만) 저장·조회 */
@RunWith(AndroidJUnit4::class)
class CalculationDaoTest {

    private lateinit var db: AppDatabase

    private val plan = PlanEntity(planId = "plan-1", vehicleId = DefaultVehicles.AVANTE.vehicleId, passengerCount = 2)
    private val box = PlanItemEntity("item-1", "plan-1", "예시 상자", widthCm = 30, depthCm = 40, heightCm = 20, quantity = 2)

    private val calculation = CalculationEntity(
        calculationId = "calc-1",
        planId = "plan-1",
        isSuccess = true,
        resultCode = "LAYOUT_FOUND",
        currentFoldedSegments = emptyList(),
        createdAt = "2026-11-11T02:01:00Z",
    )
    private val arrangement = ArrangementEntity(
        arrangementId = "arr-1",
        calculationId = "calc-1",
        rank = 1,
        passengerSeats = listOf(SeatCode.ROW_1_LEFT, SeatCode.ROW_1_RIGHT),
        targetFoldedSegments = emptyList(),
        seatChangeCount = 0,
        score = null,
        profileId = "TRUNK_ONLY",
        allInTrunk = true,
        excludedTotalQuantity = 0,
        reasonCodes = listOf("NO_SEAT_CHANGE", "ALL_IN_TRUNK"),
    )
    private val trunk = PackingSpaceEntity(
        arrangementId = "arr-1",
        spaceId = "TRUNK",
        kind = "TRUNK",
        widthCm = 97,
        depthCm = 82,
        heightCm = 42,
        originXCm = 0,
        originYCm = 0,
        originZCm = 0,
        seatCodes = emptyList(),
    )
    private val placements = listOf(
        PlacementEntity("pl-1", "arr-1", "item-1", unitIndex = 1, spaceId = "TRUNK", xCm = 0, yCm = 0, zCm = 0, orientation = Orientation.WDH, loadOrder = 1),
        PlacementEntity("pl-2", "arr-1", "item-1", unitIndex = 2, spaceId = "TRUNK", xCm = 30, yCm = 0, zCm = 0, orientation = Orientation.WDH, loadOrder = 2),
    )

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
        ).build()
        db.vehicleDao().upsert(*DefaultVehicles.ALL.toTypedArray())
        db.planDao().savePlan(plan, listOf(box), emptyList())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun saveCalculation_readsBackArrangementsSpacesAndPlacements() = runBlocking {
        db.calculationDao().saveCalculation(calculation, listOf(arrangement), listOf(trunk), placements)

        val saved = db.calculationDao().observeLatest("plan-1").first()!!
        assertEquals("LAYOUT_FOUND", saved.calculation.resultCode)
        val first = saved.sortedArrangements.first()
        assertEquals(listOf(SeatCode.ROW_1_LEFT, SeatCode.ROW_1_RIGHT), first.arrangement.passengerSeats)
        assertEquals(listOf("NO_SEAT_CHANGE", "ALL_IN_TRUNK"), first.arrangement.reasonCodes)
        assertEquals(97, first.spaces.single().widthCm)
        assertEquals(listOf(1, 2), first.placements.map { it.loadOrder }.sorted())
    }

    @Test
    fun savePlan_again_clearsOldCalculations() = runBlocking {
        db.calculationDao().saveCalculation(calculation, listOf(arrangement), listOf(trunk), placements)
        db.planDao().savePlan(plan, listOf(box), emptyList())

        assertNull(db.calculationDao().getLatest("plan-1"))
    }

    @Test
    fun deletePlan_cascadesToCalculationTree() = runBlocking {
        db.calculationDao().saveCalculation(calculation, listOf(arrangement), listOf(trunk), placements)
        db.planDao().deletePlan("plan-1")

        listOf("calculation", "arrangement", "packing_space", "placement").forEach {
            assertEquals(it, 0, count(it))
        }
    }

    @Test
    fun orientation_rotatesOriginalDimensions() {
        // HWD → x = 높이, y = 가로, z = 세로
        assertEquals(Triple(20, 30, 40), Orientation.HWD.rotate(widthCm = 30, depthCm = 40, heightCm = 20))
    }

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            it.moveToFirst()
            it.getInt(0)
        }
}
