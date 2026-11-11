package com.bolobhai.domain

/**
 * Audio retention (BoloBhai.md §13.6): `7d` / `30d` keep audio that many days; `forever` / `none`
 * (and anything unknown) never run the age-based cleanup. Returns the cutoff epoch-ms — audio
 * older than this should be deleted — or null when no cleanup applies.
 */
fun retentionCutoffMs(keepAudio: String, now: Long): Long? {
    val days = when (keepAudio) {
        "7d" -> 7
        "30d" -> 30
        else -> return null
    }
    return now - days * 24L * 60 * 60 * 1000
}
