package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TextCleanupTest {

    @Test
    fun capitalizesFirstLetter() {
        assertEquals("Hello bhai", capitalizeFirst("hello bhai"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("Kal meeting hai", capitalizeFirst("  kal meeting hai  "))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", capitalizeFirst("   "))
    }

    @Test
    fun alreadyCapital_unchanged() {
        assertEquals("OK done", capitalizeFirst("OK done"))
    }
}
