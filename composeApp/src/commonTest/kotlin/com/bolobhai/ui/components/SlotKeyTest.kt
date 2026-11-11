package com.bolobhai.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SlotKeyTest {

    @Test
    fun assignedAndActive_isActive() {
        assertEquals(SlotKeyVisual.Active, slotKeyVisual(slot = 2, activeSlot = 2, assigned = true))
    }

    @Test
    fun assignedNotActive_isAssigned() {
        assertEquals(SlotKeyVisual.Assigned, slotKeyVisual(slot = 3, activeSlot = 2, assigned = true))
    }

    @Test
    fun notAssigned_isEmpty_evenIfActive() {
        assertEquals(SlotKeyVisual.Empty, slotKeyVisual(slot = 5, activeSlot = 5, assigned = false))
        assertEquals(SlotKeyVisual.Empty, slotKeyVisual(slot = 5, activeSlot = 1, assigned = false))
    }
}
