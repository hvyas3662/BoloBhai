package com.bolobhai.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsJsonTest {

    // The §15 settings.json literal IS the default document.
    private val specJson = """
        {
          "onboardingDone": false,
          "theme": "dark",
          "hotkey": { "mac": "fn", "windows": "Alt" },
          "doubleTapMs": 400,
          "microphone": null,
          "sounds": true,
          "launchAtLogin": true,
          "keepAudio": "30d",
          "language": "hi",
          "postProcessing": true,
          "activeWhisperModel": "ggml-large-v3-turbo.bin",
          "activeLlmModel": "Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
          "quickSlots": { "1": "hinglish-clean.md", "2": "numbered-list.md", "3": "email.md", "4": "whatsapp-message.md", "5": null },
          "activeSlot": 1,
          "windowBounds": null,
          "debugLogging": false
        }
    """.trimIndent()

    @Test
    fun specJson_decodesToDefaults() {
        assertEquals(Settings(), SettingsJson.decode(specJson))
    }

    @Test
    fun emptyObject_decodesToDefaults() {
        assertEquals(Settings(), SettingsJson.decode("{}"))
    }

    @Test
    fun roundTrip_preservesValues() {
        val custom = Settings(
            onboardingDone = true,
            theme = "light",
            doubleTapMs = 300,
            activeSlot = 3,
            quickSlots = mapOf("1" to "a.md", "2" to null, "3" to "c.md", "4" to null, "5" to null),
        )
        assertEquals(custom, SettingsJson.decode(SettingsJson.encode(custom)))
    }

    @Test
    fun unknownKeys_areIgnored() {
        assertEquals(Settings(), SettingsJson.decode("""{ "futureFlag": true }"""))
    }

    @Test
    fun emptySlot_staysNull() {
        assertNull(SettingsJson.decode(specJson).quickSlots["5"])
        assertTrue(SettingsJson.decode(specJson).quickSlots["1"] == "hinglish-clean.md")
    }
}
