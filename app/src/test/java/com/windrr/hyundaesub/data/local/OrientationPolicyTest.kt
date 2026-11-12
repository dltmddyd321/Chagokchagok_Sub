package com.windrr.hyundaesub.data.local

import com.windrr.hyundaesub.data.local.entity.Orientation
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationPolicyTest {

    @Test
    fun omittedOrEmpty_allowsAllSix() {
        assertEquals(6, Orientation.normalizeAllowed(null).size)
        assertEquals(Orientation.ALL, Orientation.normalizeAllowed(emptyList()))
    }

    @Test
    fun duplicates_removed_inEnumOrder() {
        val result = Orientation.normalizeAllowed(listOf(Orientation.HDW, Orientation.WDH, Orientation.HDW))
        assertEquals(listOf(Orientation.WDH, Orientation.HDW), result)
    }
}
