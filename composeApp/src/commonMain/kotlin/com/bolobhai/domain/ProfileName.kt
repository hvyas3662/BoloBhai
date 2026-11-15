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
    "hinglish.md" to "Hinglish",
    "proper-english.md" to "Proper English",
    "ai-prompt.md" to "AI Prompt",
    "whatsapp-message.md" to "WhatsApp Message",
    "professional-email.md" to "Professional Email",
)
