package com.bolobhai.sidecar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SidecarTest {

    @Test
    fun backoff_followsSpecSchedule() {
        val b = BackoffPolicy() // 1s, 2s, 5s, then give up (max 3 tries)
        assertEquals(1000L, b.delayFor(0))
        assertEquals(2000L, b.delayFor(1))
        assertEquals(5000L, b.delayFor(2))
        assertNull(b.delayFor(3))
    }

    @Test
    fun parseText_extractsAndTrims() {
        assertEquals("hello bhai", parseInferenceText("""{"text":" hello bhai "}"""))
    }

    @Test
    fun parseText_ignoresUnknownFields() {
        assertEquals("ok", parseInferenceText("""{"text":"ok","language":"hi","segments":[]}"""))
    }

    @Test
    fun parseText_emptyOnGarbage() {
        assertEquals("", parseInferenceText("not json"))
    }
}
