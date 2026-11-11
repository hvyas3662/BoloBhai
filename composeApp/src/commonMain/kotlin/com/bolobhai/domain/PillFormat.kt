package com.bolobhai.domain

/** Recording elapsed time as m:ss (BoloBhai.md §8 timer). */
fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** Short pill profile label (BoloBhai.md §8): Hinglish · List · Email · WhatsApp · Notes · Raw. */
fun shortProfileName(fileName: String?): String {
    if (fileName.isNullOrBlank()) return "Raw"
    return shortNames[fileName] ?: profileDisplayName(fileName)
}

private val shortNames = mapOf(
    "hinglish-clean.md" to "Hinglish",
    "numbered-list.md" to "List",
    "email.md" to "Email",
    "whatsapp-message.md" to "WhatsApp",
    "meeting-notes.md" to "Notes",
)
