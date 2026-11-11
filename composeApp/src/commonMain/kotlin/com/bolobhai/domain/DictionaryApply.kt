package com.bolobhai.domain

import com.bolobhai.data.Dictionary

/**
 * Dictionary post-processing (BoloBhai.md §13.4): corrections run after transcription, before the
 * LLM. Each `wrong` is replaced by `right` as a whole, case-insensitive match; later corrections
 * see the output of earlier ones.
 */
fun applyCorrections(text: String, dict: Dictionary): String {
    var out = text
    for (c in dict.corrections) {
        if (c.wrong.isBlank()) continue
        val pattern = Regex("(?i)(?<![\\p{L}\\p{N}])" + Regex.escape(c.wrong) + "(?![\\p{L}\\p{N}])")
        out = pattern.replace(out, Regex.escapeReplacement(c.right))
    }
    return out
}

/** Custom words fed to Whisper as its initial prompt to bias spelling (BoloBhai.md §13.4). */
fun whisperPrompt(dict: Dictionary): String = dict.customWords.joinToString(", ")
