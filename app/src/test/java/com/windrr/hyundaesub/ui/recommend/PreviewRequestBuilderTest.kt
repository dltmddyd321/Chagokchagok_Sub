package com.windrr.hyundaesub.ui.recommend

import com.windrr.hyundaesub.data.local.entity.SeatCode
import com.windrr.hyundaesub.ui.item.PickedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class PreviewRequestBuilderTest {

    private val input = RecommendInput(
        localVehicleId = "avante_cn8",
        occupiedSeats = setOf(SeatCode.ROW_2_LEFT),
        items = listOf(
            PickedItem(key = "a", name = "유모차", widthCm = 55, depthCm = 85, heightCm = 40, quantity = 1, required = true),
            PickedItem(key = "b", name = "캐리어", widthCm = 40, depthCm = 25, heightCm = 60, quantity = 2, savedItemId = "s1"),
            PickedItem(key = "c", name = "빼둔 짐", widthCm = 10, depthCm = 10, heightCm = 10, selected = false),
        ),
    )

    private val request = PreviewRequestBuilder.build(input, OffsetDateTime.parse("2026-11-14T12:00:00+09:00"))

    @Test
    fun passengers_includeDriver() {
        assertEquals("avante_demo", request.plan.vehicleId)
        assertEquals(2, request.plan.passengerCount)
        assertEquals(listOf("ROW_1_LEFT", "ROW_2_LEFT"), request.plan.passengerSeats)
    }

    @Test
    fun onlySelectedItems_withRequiredAndSource() {
        val items = request.plan.items
        assertEquals(listOf("유모차", "캐리어"), items.map { it.name })
        assertEquals(1, items[0].requiredQuantity)
        assertEquals("UNSPECIFIED", items[0].dimensionSource)
        assertNull(items[0].savedItemId)
        assertEquals(0, items[1].requiredQuantity)
        assertEquals("SAVED_ITEM", items[1].dimensionSource)
    }

    @Test
    fun vehicleState_marksUnselectedSeatsEmpty() {
        val seats = request.calculationOptions.vehicleState!!.seats.associate { it.seatCode to it.occupancy }
        assertEquals(
            mapOf(
                "ROW_1_LEFT" to "OCCUPIED",
                "ROW_1_RIGHT" to "EMPTY",
                "ROW_2_LEFT" to "OCCUPIED",
                "ROW_2_CENTER" to "EMPTY",
                "ROW_2_RIGHT" to "EMPTY",
            ),
            seats,
        )
        assertEquals("REPACK_ALL", request.calculationOptions.packingMode)
    }
}
