package com.bolobhai.domain

/** Post-processing-off output (BoloBhai.md §9): trim and capitalize the first letter. */
fun capitalizeFirst(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.replaceFirstChar { it.uppercase() }
}
