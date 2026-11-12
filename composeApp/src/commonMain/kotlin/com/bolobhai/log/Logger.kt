package com.bolobhai.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tiny app-wide logger (BoloBhai.md §16 debug logs). Keeps the last [MAX] entries in memory for the
 * Logs panel and appends every line to `<app-data>/bolobhai.log` so a crashing sidecar's output
 * survives for a bug report. Thread-safe (sidecar reader threads log here).
 */
object Logger {
    enum class Level { INFO, WARN, ERROR }

    data class Entry(val ts: Long, val level: Level, val msg: String)

    private const val MAX = 2000
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.US).withZone(ZoneId.systemDefault())

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    @Volatile private var file: Path? = null

    fun init(logFile: Path) {
        file = logFile
        info("--- Bolo Bhai log started ---")
    }

    fun info(msg: String) = log(Level.INFO, msg)
    fun warn(msg: String) = log(Level.WARN, msg)
    fun error(msg: String) = log(Level.ERROR, msg)

    @Synchronized
    fun log(level: Level, msg: String) {
        val entry = Entry(System.currentTimeMillis(), level, msg)
        _entries.value = (_entries.value + entry).takeLast(MAX)
        file?.let { f ->
            runCatching {
                f.parent?.let { Files.createDirectories(it) }
                Files.writeString(f, format(entry) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            }
        }
    }

    /** Full report text for sharing: the whole log file if present, else the in-memory entries. */
    fun report(): String {
        val f = file
        if (f != null && Files.exists(f)) return runCatching { Files.readString(f) }.getOrDefault(memory())
        return memory()
    }

    @Synchronized
    fun clear() {
        _entries.value = emptyList()
        file?.let { runCatching { Files.writeString(it, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) } }
    }

    private fun memory() = _entries.value.joinToString("\n") { format(it) }

    private fun format(e: Entry) = "${clock.format(Instant.ofEpochMilli(e.ts))} ${e.level.name.padEnd(5)} ${e.msg}"
}
