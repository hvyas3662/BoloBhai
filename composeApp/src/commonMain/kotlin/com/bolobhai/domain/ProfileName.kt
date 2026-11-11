package com.bolobhai.domain

/**
 * A short display name for a profile file. M1 has no frontmatter parsing yet (that lands with the
 * profiles milestone), so known defaults map to their spec names (§11) and anything else is
 * title-cased from its slug. Null/blank => "Empty".
 */
fun profileDisplayName(fileName: String?): String {
    if (fileName.isNullOrBlank()) return "Empty"
    knownDefaults[fileName]?.let { return it }
    return fileName.removeSuffix(".md")
        .split('-', '_')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

private val knownDefaults = mapOf(
    "hinglish-clean.md" to "Hinglish Clean",
    "numbered-list.md" to "Numbered List",
    "email.md" to "Email",
    "whatsapp-message.md" to "WhatsApp Message",
    "meeting-notes.md" to "Meeting Notes",
)
