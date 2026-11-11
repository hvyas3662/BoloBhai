package com.bolobhai.ui.theme

import androidx.compose.ui.graphics.Color

/** Theme selection; System resolves against the OS appearance (resolved once at startup in M1). */
enum class ThemeMode { Dark, Light, System }

fun resolveDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.Dark -> true
    ThemeMode.Light -> false
    ThemeMode.System -> systemDark
}

/** The full Bolo Bhai color palette (BoloBhai.md §12.1). */
data class BoloColors(
    val bg: Color,
    val side: Color,
    val panel: Color,
    val card: Color,
    val card2: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val input: Color,
    val blue: Color,
    val blueSoft: Color,
    val orange: Color,
    val orangeSoft: Color,
    val onAccent: Color,
    val red: Color,
    val redBorder: Color,
    val onRed: Color,
    val green: Color,
    val rec: Color,
    val shadow: Color,
    val overlay: Color,
)

fun boloDarkColors(): BoloColors = BoloColors(
    bg = Color(0xFF0B1020),
    side = Color(0xFF10172A),
    panel = Color(0xFF131B30),
    card = Color(0xFF151D33),
    card2 = Color(0xFF1B2440),
    border = Color(0xFF263152),
    text = Color(0xFFE9EEF8),
    muted = Color(0xFF9EAAC4),
    input = Color(0xFF0F1629),
    blue = Color(0xFF53C4F4),
    blueSoft = Color(0xFF53C4F4).copy(alpha = 0.14f),
    orange = Color(0xFFF6964F),
    orangeSoft = Color(0xFFF6964F).copy(alpha = 0.14f),
    onAccent = Color(0xFF06111F),
    red = Color(0xFFFF7E7E),
    redBorder = Color(0xFFFF7E7E).copy(alpha = 0.35f),
    onRed = Color(0xFF2A0707),
    green = Color(0xFF4ADE80),
    rec = Color(0xFFFF5F5F),
    shadow = Color(0xFF000000).copy(alpha = 0.5f),
    overlay = Color(0xFF04070F).copy(alpha = 0.6f),
)

fun boloLightColors(): BoloColors = BoloColors(
    bg = Color(0xFFF2F4F8),
    side = Color(0xFFFFFFFF),
    panel = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    card2 = Color(0xFFF4F6FA),
    border = Color(0xFFDCE2EC),
    text = Color(0xFF111827),
    muted = Color(0xFF525D74),
    input = Color(0xFFF7F9FC),
    blue = Color(0xFF0A6FAD),
    blueSoft = Color(0xFF0A6FAD).copy(alpha = 0.10f),
    orange = Color(0xFFA64B0A),
    orangeSoft = Color(0xFFA64B0A).copy(alpha = 0.10f),
    onAccent = Color(0xFFFFFFFF),
    red = Color(0xFFB42323),
    redBorder = Color(0xFFB42323).copy(alpha = 0.35f),
    onRed = Color(0xFFFFFFFF),
    green = Color(0xFF1F8A4C),
    rec = Color(0xFFD12C2C),
    shadow = Color(0xFF111827).copy(alpha = 0.16f),
    overlay = Color(0xFF111827).copy(alpha = 0.35f),
)

fun boloColors(mode: ThemeMode, systemDark: Boolean): BoloColors =
    if (resolveDark(mode, systemDark)) boloDarkColors() else boloLightColors()
