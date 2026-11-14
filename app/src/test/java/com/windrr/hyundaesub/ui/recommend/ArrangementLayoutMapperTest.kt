package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.data.remote.model.ArrangementResponse
import com.windrr.hyundaesub.data.remote.model.PackingSpaceResponse
import com.windrr.hyundaesub.data.remote.model.PlacementResponse
import com.windrr.hyundaesub.data.remote.model.SnapshotItem
import com.windrr.hyundaesub.data.remote.model.SpaceOriginResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 실서버 응답(아반떼 데모, 뒷좌석 왼쪽 접기 배치)을 줄여 옮긴 값으로 검증 */
class ArrangementLayoutMapperTest {

    private val trunk = PackingSpaceResponse("TRUNK", "TRUNK", 97.0, 110.0, 50.0, SpaceOriginResponse(0.0, 0.0, 0.0), emptyList())
    private val rearLeftFolded = PackingSpaceResponse(
        "REAR_LEFT_FOLDED", "FOLDED_SEAT", 61.0, 60.0, 50.0, SpaceOriginResponse(0.0, 110.0, 0.0), listOf("ROW_2_LEFT", "ROW_2_CENTER"),
    )
    private val rearRight = PackingSpaceResponse(
        "REAR_RIGHT_SEAT", "SEAT", 30.0, 40.0, 55.0, SpaceOriginResponse(62.0, 110.0, 0.0), listOf("ROW_2_RIGHT"),
    )
    private val items = listOf(
        SnapshotItem("big", "큰박스", 90.0, 100.0, 40.0, 1),
        SnapshotItem("long", "긴박스", 55.0, 55.0, 40.0, 1),
    )
    private val arrangement = ArrangementResponse(
        arrangementId = "arr1",
        rank = 1,
        passengerSeats = listOf("ROW_1_LEFT"),
        targetFoldedSegments = listOf("DEMO_REAR_LEFT"),
        packingSpaces = listOf(trunk, rearLeftFolded, rearRight),
        placements = listOf(
            // 순서 섞어서 보냄 → load_order대로 정렬돼야 함
            PlacementResponse("p2", "long", 1, "REAR_LEFT_FOLDED", 0.0, 0.0, 0.0, "WDH", 2),
            PlacementResponse("p1", "big", 1, "TRUNK", 0.0, 0.0, 0.0, "WDH", 1),
        ),
        allInTrunk = false,
        reasonCodes = emptyList(),
    )

    private val layout = ArrangementLayoutMapper.map(arrangement, items)

    @Test
    fun items_inLoadOrder() {
        assertEquals(listOf("큰박스", "긴박스"), layout.items.map { it.name })
        assertEquals(listOf(0, 1), layout.items.map { it.order })
    }

    @Test
    fun foldedSeats_haveNoSeatBox_otherSeatsKeepBox() {
        val marks = layout.seats.associate { it.seat to it.mark }
        assertEquals(
            mapOf(SeatCode.ROW_1_RIGHT to SeatMark.EMPTY_SEAT, SeatCode.ROW_2_RIGHT to SeatMark.EMPTY_SEAT),
            marks,
        )
    }

    @Test
    fun foldedGroups_andSpaceKind() {
        assertEquals(listOf(listOf(SeatCode.ROW_2_LEFT, SeatCode.ROW_2_CENTER)), layout.foldedGroups.map { it.seats })
        assertEquals(listOf("TRUNK", "FOLDED_SEAT"), layout.items.map { it.spaceKind })
    }

    @Test
    fun trunkItem_startsAtLeftRearCorner() {
        val rect = layout.items[0].rect
        val trunkRect = AvanteTopView.TRUNK
        assertEquals(trunkRect.left, rect.left, 0.01f)
        assertEquals(trunkRect.bottom, rect.bottom, 0.01f) // y=0(뒤) = 이미지 아래
        assertEquals(trunkRect.left + trunkRect.width * (90f / 97f), rect.right, 0.5f)
        assertEquals(trunkRect.bottom - trunkRect.height * (100f / 110f), rect.top, 0.5f)
    }

    @Test
    fun foldedSeatItem_staysInsideFoldedArea() {
        val area = ArrangementLayoutMapper.spaceRect(rearLeftFolded)!!
        val rect = layout.items[1].rect
        assertTrue(rect.left >= area.left && rect.right <= area.right)
        assertTrue(rect.top >= area.top && rect.bottom <= area.bottom)
    }

    @Test
    fun footprint_followsOrientation() {
        val item = SnapshotItem("i", "캐리어", 40.0, 25.0, 60.0, 1)
        assertEquals(40.0 to 25.0, ArrangementLayoutMapper.footprint(item, "WDH"))
        assertEquals(40.0 to 60.0, ArrangementLayoutMapper.footprint(item, "WHD"))
        assertEquals(60.0 to 25.0, ArrangementLayoutMapper.footprint(item, "HDW"))
    }
}
