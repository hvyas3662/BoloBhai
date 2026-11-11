package com.bolobhai.domain

import com.bolobhai.data.Dictation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.time.format.DateTimeFormatter

/** Presentation helpers for the History screen (BoloBhai.md §13.1). Pure, so they are unit-tested. */

private val CLOCK = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val DATE = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)

/** Duration as `m:ss` (BoloBhai.md §14). */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

/** Local 12-hour clock, e.g. `10:42 AM` (BoloBhai.md §14). */
fun formatClock(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    CLOCK.format(Instant.ofEpochMilli(epochMs).atZone(zone))

/** Day-group label: `Today`, `Yesterday`, else `Mon, 9 Nov` (BoloBhai.md §13.1). */
fun dayLabel(epochMs: Long, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
    val day = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DATE.format(day)
    }
}

/** Items grouped by day label, newest day first, preserving each group's incoming order. */
fun groupByDay(items: List<Dictation>, nowMs: Long = System.currentTimeMillis()): List<Pair<String, List<Dictation>>> =
    items.groupBy { dayLabel(it.createdAt, nowMs) }.toList()

/** The profile name a dictation is filed under by its current version; null when it's raw / stt-failed. */
fun currentProfileName(item: Dictation): String? =
    item.current?.takeIf { it.profileId != null }?.profileName

fun isRaw(item: Dictation): Boolean = item.status == "ok" && item.current?.profileId == null
fun isFailed(item: Dictation): Boolean = item.status != "ok"

/** A filter chip selection. */
sealed interface HistoryFilter {
    data object All : HistoryFilter
    data class ByProfile(val profileName: String) : HistoryFilter
    data object Raw : HistoryFilter
    data object Failed : HistoryFilter
}

fun filterMatches(item: Dictation, filter: HistoryFilter): Boolean = when (filter) {
    HistoryFilter.All -> true
    HistoryFilter.Raw -> isRaw(item)
    HistoryFilter.Failed -> isFailed(item)
    is HistoryFilter.ByProfile -> !isFailed(item) && currentProfileName(item) == filter.profileName
}

/** Ordered profile-name -> count for the chip row (ok, non-raw items by current version). */
fun profileCounts(items: List<Dictation>): Map<String, Int> {
    val out = LinkedHashMap<String, Int>()
    items.filter { !isFailed(it) }.forEach { item ->
        val name = currentProfileName(item) ?: return@forEach
        out[name] = (out[name] ?: 0) + 1
    }
    return out
}

/**
 * Live search over raw transcript + every version output: all whitespace-separated tokens must
 * appear (case-insensitive substring).
 * shortcut: in-memory substring, not the repo's FTS5 prefix index — fine for interactive typing;
 * switch to HistoryRepo.search if the list grows large.
 */
fun queryMatches(item: Dictation, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    val hay = (item.rawTranscript + "\n" + item.versions.joinToString("\n") { it.output }).lowercase()
    return q.split(Regex("\\s+")).all { hay.contains(it) }
}

fun rawCount(items: List<Dictation>): Int = items.count { isRaw(it) }
fun failedCount(items: List<Dictation>): Int = items.count { isFailed(it) }
