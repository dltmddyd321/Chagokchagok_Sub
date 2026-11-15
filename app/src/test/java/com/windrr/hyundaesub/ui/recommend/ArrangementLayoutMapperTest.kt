package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.data.remote.model.ArrangementResponse
import com.windrr.hyundaesub.data.remote.model.PackingSpaceResponse
import com.windrr.hyundaesub.data.remote.model.PlacementResponse
import com.windrr.hyundaesub.data.remote.model.SnapshotItem
import com.windrr.hyundaesub.data.remote.model.SpaceOriginResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 실서버 응답(투싼 하이브리드, 2열 모두 접기: 책상을 옆으로 눕혀 트렁크~접은 2열에 걸침)을 옮긴 값으로 검증 */
class ArrangementLayoutMapperTest {

    private val trunk = PackingSpaceResponse("TRUNK", "TRUNK", 123.0, 100.0, 80.0, SpaceOriginResponse(0.0, 0.0, 0.0), emptyList())
    private val rearFolded = PackingSpaceResponse(
        "REAR_FOLDED", "FOLDED_SEAT", 116.0, 60.0, 70.0, SpaceOriginResponse(3.5, 100.0, 0.0),
        listOf("ROW_2_LEFT", "ROW_2_CENTER", "ROW_2_RIGHT"),
    )
    private val items = listOf(
        SnapshotItem("desk", "책상", 120.0, 60.0, 73.0, 1),
        SnapshotItem("box", "짐 2", 60.0, 40.0, 35.0, 1),
    )
    private val arrangement = ArrangementResponse(
        arrangementId = "arr1",
        rank = 1,
        passengerSeats = listOf("ROW_1_LEFT", "ROW_1_RIGHT"),
        targetFoldedSegments = listOf("REAR_ALL"),
        packingSpaces = listOf(trunk, rearFolded),
        placements = listOf(
            // 순서 섞어서 보냄 → load_order대로 정렬돼야 함
            PlacementResponse("p2", "box", 1, "REAR_FOLDED", 0.0, 20.0, 0.0, "WDH", 2),
            PlacementResponse("p1", "desk", 1, "TRUNK", 3.5, 0.0, 0.0, "HWD", 1),
        ),
        allInTrunk = false,
        reasonCodes = emptyList(),
    )

    private val layout = ArrangementLayoutMapper.map(arrangement, items)
    private val s = VehicleTopView.PX_PER_CM.toFloat()

    @Test
    fun items_inLoadOrder() {
        assertEquals(listOf("책상", "짐 2"), layout.items.map { it.name })
        assertEquals(listOf(0, 1), layout.items.map { it.order })
    }

    @Test
    fun foldedSeats_haveNoSeatBox_otherSeatsKeepBox() {
        val marks = layout.seats.associate { it.seat to it.mark }
        assertEquals(mapOf(SeatCode.ROW_1_RIGHT to SeatMark.BOARDING), marks)
    }

    @Test
    fun foldedGroups_andSpaceKind() {
        assertEquals(listOf(listOf(SeatCode.ROW_2_LEFT, SeatCode.ROW_2_CENTER, SeatCode.ROW_2_RIGHT)), layout.foldedGroups.map { it.seats })
        assertEquals(listOf("TRUNK", "FOLDED_SEAT"), layout.items.map { it.spaceKind })
    }

    @Test
    fun trunkItem_drawnAtVehicleScale_andSpansIntoFoldedSeats() {
        val rect = layout.items[0].rect // 옆으로 눕힌 책상: 바닥 73 × 120 cm, 왼쪽 벽(x 3.5)에서 트렁크 입구부터
        val trunkRect = VehicleTopView.TRUNK
        assertEquals(73f * s, rect.width, 0.5f)
        assertEquals(120f * s, rect.height, 0.5f)
        assertEquals(trunkRect.left + 3.5f * s, rect.left, 0.5f)
        assertEquals(trunkRect.bottom, rect.bottom, 0.5f)
        // 트렁크 깊이(100cm)를 넘는 20cm는 접은 2열 위로
        assertTrue(rect.top < trunkRect.top)
    }

    @Test
    fun foldedSeatItem_continuesFromTrunkAtSameScale() {
        val desk = layout.items[0].rect
        val box = layout.items[1].rect // 접은 2열 왼쪽, 책상 바로 앞 (차량 좌표 y 120~160)
        assertEquals(60f * s, box.width, 0.5f)
        assertEquals(40f * s, box.height, 0.5f)
        assertEquals(desk.top, box.bottom, 0.5f)
        assertEquals(desk.left, box.left, 0.5f)
    }

    @Test
    fun unfoldedSeat_fallsBackToSeatArea() {
        val seat = PackingSpaceResponse("REAR_RIGHT_SEAT", "SEAT", 30.0, 40.0, 55.0, SpaceOriginResponse(80.0, 100.0, 0.0), listOf("ROW_2_RIGHT"))
        assertNull(ArrangementLayoutMapper.cargoRect(seat, 0.0, 0.0, 30.0, 40.0))
        val area = ArrangementLayoutMapper.spaceRect(seat)!!
        val rect = ArrangementLayoutMapper.toImageRect(area, seat, 0.0, 0.0, 30.0, 40.0)
        assertTrue(rect.left >= area.left - 0.5f && rect.right <= area.right + 0.5f)
        assertTrue(rect.top >= area.top - 0.5f && rect.bottom <= area.bottom + 0.5f)
    }

    @Test
    fun footprint_followsOrientation() {
        val item = SnapshotItem("i", "캐리어", 40.0, 25.0, 60.0, 1)
        assertEquals(40.0 to 25.0, ArrangementLayoutMapper.footprint(item, "WDH"))
        assertEquals(40.0 to 60.0, ArrangementLayoutMapper.footprint(item, "WHD"))
        assertEquals(60.0 to 25.0, ArrangementLayoutMapper.footprint(item, "HDW"))
    }
}
