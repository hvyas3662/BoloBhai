package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PillFormatTest {

    @Test
    fun timer_formatsAsMinuteColonSeconds() {
        assertEquals("0:00", formatTimer(0))
        assertEquals("0:05", formatTimer(5_000))
        assertEquals("0:59", formatTimer(59_000))
        assertEquals("1:05", formatTimer(65_000))
        assertEquals("10:00", formatTimer(600_000))
    }

    @Test
    fun shortName_knownDefaults() {
        assertEquals("Hinglish", shortProfileName("hinglish-clean.md"))
        assertEquals("List", shortProfileName("numbered-list.md"))
        assertEquals("Email", shortProfileName("email.md"))
        assertEquals("WhatsApp", shortProfileName("whatsapp-message.md"))
        assertEquals("Notes", shortProfileName("meeting-notes.md"))
    }

    @Test
    fun shortName_nullIsRaw() {
        assertEquals("Raw", shortProfileName(null))
    }

    @Test
    fun shortName_customFallsBackToDisplayName() {
        assertEquals("My Thing", shortProfileName("my-thing.md"))
    }
}
