package com.bolobhai.domain

import java.security.MessageDigest
import java.util.Locale

/** On-disk state of a model file (BoloBhai.md §13.5). "Active" is settings-derived in the UI. */
enum class ModelState { NotDownloaded, Partial, Installed }

fun modelState(hasFile: Boolean, hasPart: Boolean): ModelState = when {
    hasFile -> ModelState.Installed
    hasPart -> ModelState.Partial
    else -> ModelState.NotDownloaded
}

/** `2.50 GB` / `550 MB` for a byte count. */
fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1.0) String.format(Locale.US, "%.2f GB", gb)
    else String.format(Locale.US, "%.0f MB", bytes / 1_000_000.0)
}

/** Download status line, e.g. `46% · 1.15 of 2.50 GB · 41.6 MB/s` (BoloBhai.md §13.5). */
fun formatProgress(done: Long, total: Long, bytesPerSec: Long): String {
    val pct = if (total > 0) (done * 100 / total).toInt() else 0
    val doneGb = String.format(Locale.US, "%.2f", done / 1_000_000_000.0)
    val totalStr = formatBytes(total)
    val speed = String.format(Locale.US, "%.1f MB/s", bytesPerSec / 1_000_000.0)
    return "$pct% · $doneGb of $totalStr · $speed"
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
