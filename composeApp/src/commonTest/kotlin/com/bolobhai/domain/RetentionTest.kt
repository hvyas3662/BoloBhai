package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RetentionTest {
    private val now = 1_000_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test fun `7d and 30d subtract the window`() {
        assertEquals(now - 7 * day, retentionCutoffMs("7d", now))
        assertEquals(now - 30 * day, retentionCutoffMs("30d", now))
    }

    @Test fun `forever none and unknown never clean`() {
        assertNull(retentionCutoffMs("forever", now))
        assertNull(retentionCutoffMs("none", now))
        assertNull(retentionCutoffMs("whatever", now))
    }
}
