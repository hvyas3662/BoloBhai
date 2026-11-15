package com.bolobhai.domain

/** Recording elapsed time as m:ss (BoloBhai.md §8 timer). */
fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Short pill profile label (BoloBhai.md §8): Hinglish · English · Prompt · WhatsApp · Email · Raw. */
fun shortProfileName(fileName: String?): String {
    if (fileName.isNullOrBlank()) return "Raw"
    return shortNames[fileName] ?: profileDisplayName(fileName)
}

private val shortNames = mapOf(
    "hinglish.md" to "Hinglish",
    "proper-english.md" to "English",
    "ai-prompt.md" to "Prompt",
    "whatsapp-message.md" to "WhatsApp",
    "professional-email.md" to "Email",
)
