package com.bolobhai.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokensTest {

    // --- Theme resolution logic ---

    @Test
    fun darkMode_isAlwaysDark() {
        assertTrue(resolveDark(ThemeMode.Dark, systemDark = false))
        assertTrue(resolveDark(ThemeMode.Dark, systemDark = true))
    }

    @Test
    fun lightMode_isAlwaysLight() {
        assertTrue(!resolveDark(ThemeMode.Light, systemDark = false))
        assertTrue(!resolveDark(ThemeMode.Light, systemDark = true))
    }

    @Test
    fun systemMode_followsSystem() {
        assertTrue(resolveDark(ThemeMode.System, systemDark = true))
        assertTrue(!resolveDark(ThemeMode.System, systemDark = false))
    }

    @Test
    fun boloColors_picksSetByResolvedMode() {
        assertEquals(boloDarkColors(), boloColors(ThemeMode.Dark, systemDark = false))
        assertEquals(boloLightColors(), boloColors(ThemeMode.Light, systemDark = true))
        assertEquals(boloDarkColors(), boloColors(ThemeMode.System, systemDark = true))
        assertEquals(boloLightColors(), boloColors(ThemeMode.System, systemDark = false))
    }

    // --- Exact token values from BoloBhai.md §12.1 ---

    @Test
    fun darkSolidTokens_matchSpec() {
        val c = boloDarkColors()
        assertEquals(Color(0xFF0B1020), c.bg)
        assertEquals(Color(0xFF10172A), c.side)
        assertEquals(Color(0xFF131B30), c.panel)
        assertEquals(Color(0xFF151D33), c.card)
        assertEquals(Color(0xFF1B2440), c.card2)
        assertEquals(Color(0xFF263152), c.border)
        assertEquals(Color(0xFFE9EEF8), c.text)
        assertEquals(Color(0xFF9EAAC4), c.muted)
        assertEquals(Color(0xFF0F1629), c.input)
        assertEquals(Color(0xFF53C4F4), c.blue)
        assertEquals(Color(0xFFF6964F), c.orange)
        assertEquals(Color(0xFF06111F), c.onAccent)
        assertEquals(Color(0xFFFF7E7E), c.red)
        assertEquals(Color(0xFF2A0707), c.onRed)
        assertEquals(Color(0xFF4ADE80), c.green)
        assertEquals(Color(0xFFFF5F5F), c.rec)
    }

    @Test
    fun lightSolidTokens_matchSpec() {
        val c = boloLightColors()
        assertEquals(Color(0xFFF2F4F8), c.bg)
        assertEquals(Color(0xFFFFFFFF), c.side)
        assertEquals(Color(0xFF111827), c.text)
        assertEquals(Color(0xFF525D74), c.muted)
        assertEquals(Color(0xFF0A6FAD), c.blue)
        assertEquals(Color(0xFFA64B0A), c.orange)
        assertEquals(Color(0xFFFFFFFF), c.onAccent)
        assertEquals(Color(0xFFB42323), c.red)
        assertEquals(Color(0xFF1F8A4C), c.green)
        assertEquals(Color(0xFFD12C2C), c.rec)
    }

    @Test
    fun softTokens_areTranslucent() {
        val d = boloDarkColors()
        // blueSoft = rgba(83,196,244,.14)
        assertEquals(0.14f, d.blueSoft.alpha, 0.01f)
        assertEquals(Color(0xFF53C4F4).red, d.blueSoft.red, 0.01f)
        // orangeSoft = rgba(246,150,79,.14)
        assertEquals(0.14f, d.orangeSoft.alpha, 0.01f)
        // redBorder = rgba(255,126,126,.35)
        assertEquals(0.35f, d.redBorder.alpha, 0.01f)
        // overlay = rgba(4,7,15,.6)
        assertEquals(0.6f, d.overlay.alpha, 0.01f)
    }
}
