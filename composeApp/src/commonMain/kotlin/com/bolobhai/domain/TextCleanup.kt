package com.bolobhai.domain

/** Post-processing-off output (BoloBhai.md §9): trim and capitalize the first letter. */
fun capitalizeFirst(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.replaceFirstChar { it.uppercase() }
}

private val thinkBlock = Regex("<think>.*?</think>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val herePreamble = Regex("^here('s| is)\\b.*:\\s*", RegexOption.IGNORE_CASE)

/**
 * Defensively clean an LLM response (BoloBhai.md §9): drop <think> blocks, a leading
 * "Here is …:" preamble, and surrounding quotes.
 */
fun cleanupLlmOutput(raw: String): String {
    var text = thinkBlock.replace(raw, "").trim()
    text = herePreamble.replace(text, "").trim()
    text = stripSurroundingQuotes(text).trim()
    return text
}

private fun stripSurroundingQuotes(text: String): String {
    if (text.length < 2) return text
    val first = text.first()
    val last = text.last()
    val pairs = mapOf('"' to '"', '\'' to '\'', '“' to '”')
    if (pairs[first] == last) return text.substring(1, text.length - 1)
    return text
}
