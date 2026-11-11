package com.bolobhai.domain

import com.bolobhai.data.Dictation
import com.bolobhai.data.Version
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryViewTest {
    private val utc = ZoneId.of("UTC")

    private fun item(
        id: Long = 1,
        createdAt: Long = 0,
        status: String = "ok",
        profileId: String? = "email.md",
        profileName: String = "Email",
    ) = Dictation(
        id = id, createdAt = createdAt, durationMs = 0, rawTranscript = "raw",
        audioPath = null, status = status, currentVersion = 0, language = "hi",
        whisperModel = "w",
        versions = listOf(Version(id, 0, profileId, profileName, "out", null, null, createdAt)),
    )

    @Test
    fun `formatDuration is m colon ss`() {
        assertEquals("0:14", formatDuration(14_000))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("0:00", formatDuration(-5))
    }

    @Test
    fun `formatClock is 12-hour`() {
        val t = ZonedDateTime.of(2026, 11, 9, 22, 42, 0, 0, utc).toInstant().toEpochMilli()
        assertEquals("10:42 PM", formatClock(t, utc))
    }

    @Test
    fun `dayLabel today yesterday and date`() {
        val now = ZonedDateTime.of(2026, 11, 11, 12, 0, 0, 0, utc).toInstant().toEpochMilli()
        val today = ZonedDateTime.of(2026, 11, 11, 8, 0, 0, 0, utc).toInstant().toEpochMilli()
        val yest = ZonedDateTime.of(2026, 11, 10, 23, 0, 0, 0, utc).toInstant().toEpochMilli()
        val older = ZonedDateTime.of(2026, 11, 9, 9, 0, 0, 0, utc).toInstant().toEpochMilli()
        assertEquals("Today", dayLabel(today, now, utc))
        assertEquals("Yesterday", dayLabel(yest, now, utc))
        assertEquals("Mon, 9 Nov", dayLabel(older, now, utc))
    }

    @Test
    fun `filterMatches by profile raw failed and all`() {
        val email = item(profileId = "email.md", profileName = "Email")
        val raw = item(profileId = null, profileName = "Raw (no LLM)")
        val failed = item(status = "llm_failed")
        assertTrue(filterMatches(email, HistoryFilter.All))
        assertTrue(filterMatches(email, HistoryFilter.ByProfile("Email")))
        assertFalse(filterMatches(raw, HistoryFilter.ByProfile("Email")))
        assertTrue(filterMatches(raw, HistoryFilter.Raw))
        assertTrue(filterMatches(failed, HistoryFilter.Failed))
        assertFalse(filterMatches(failed, HistoryFilter.ByProfile("Email")))
    }

    @Test
    fun `counts split profile raw failed`() {
        val items = listOf(
            item(id = 1, profileName = "Email"),
            item(id = 2, profileName = "Email"),
            item(id = 3, profileId = null, profileName = "Raw (no LLM)"),
            item(id = 4, status = "llm_failed"),
        )
        assertEquals(mapOf("Email" to 2), profileCounts(items))
        assertEquals(1, rawCount(items))
        assertEquals(1, failedCount(items))
    }

    @Test
    fun `queryMatches needs every token, case-insensitive, across raw and outputs`() {
        val it = Dictation(
            1, 0, 0, "mera naam Kubernetes", null, "ok", 0, "hi", "w",
            listOf(Version(1, 0, "email.md", "Email", "Dear sir", null, null, 0)),
        )
        assertTrue(queryMatches(it, "kuber"))
        assertTrue(queryMatches(it, "dear SIR"))
        assertTrue(queryMatches(it, "naam dear"))
        assertFalse(queryMatches(it, "zzz"))
        assertTrue(queryMatches(it, ""))
    }

    @Test
    fun `groupByDay keeps newest-first order`() {
        val now = ZonedDateTime.of(2026, 11, 11, 12, 0, 0, 0, utc).toInstant().toEpochMilli()
        val todayMs = ZonedDateTime.of(2026, 11, 11, 8, 0, 0, 0, utc).toInstant().toEpochMilli()
        val yestMs = ZonedDateTime.of(2026, 11, 10, 8, 0, 0, 0, utc).toInstant().toEpochMilli()
        val groups = groupByDay(listOf(item(createdAt = todayMs), item(createdAt = yestMs)), now)
        assertEquals(listOf("Today", "Yesterday"), groups.map { it.first })
    }
}
