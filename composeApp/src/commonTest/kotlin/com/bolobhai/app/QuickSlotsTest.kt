package com.bolobhai.app

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickSlotsTest {

    @Test
    fun existingFiles_keptAndNoneRemoved() {
        val slots = mapOf("1" to "a.md", "2" to "b.md", "3" to null, "4" to null, "5" to null)
        val r = reconcileQuickSlots(slots, available = setOf("a.md", "b.md"))
        assertEquals(slots, r.slots)
        assertEquals(emptyList(), r.removed)
    }

    @Test
    fun missingFile_isClearedAndReported() {
        val slots = mapOf("1" to "a.md", "2" to "gone.md", "3" to null, "4" to null, "5" to null)
        val r = reconcileQuickSlots(slots, available = setOf("a.md"))
        assertEquals("a.md", r.slots["1"])
        assertEquals(null, r.slots["2"])
        assertEquals(listOf(2), r.removed)
    }

    @Test
    fun multipleMissing_allReported() {
        val slots = mapOf("1" to "x.md", "2" to "y.md", "3" to "z.md", "4" to null, "5" to null)
        val r = reconcileQuickSlots(slots, available = emptySet())
        assertEquals(listOf(1, 2, 3), r.removed)
        assertEquals(List(5) { null }, (1..5).map { r.slots[it.toString()] })
    }
}
