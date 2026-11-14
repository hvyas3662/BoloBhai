package com.bolobhai.data

import kotlinx.serialization.Serializable

@Serializable
data class HotkeySettings(
    val mac: String = "fn",
    val windows: String = "Alt",
)

@Serializable
data class WindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/** Persisted app settings; defaults mirror BoloBhai.md §15 settings.json. */
@Serializable
data class Settings(
    val onboardingDone: Boolean = false,
    val theme: String = "dark",
    val hotkey: HotkeySettings = HotkeySettings(),
    val doubleTapMs: Int = 400,
    val microphone: String? = null,
    val sounds: Boolean = true,
    val launchAtLogin: Boolean = true,
    val keepAudio: String = "30d",
    val language: String = "hi",
    val postProcessing: Boolean = true,
    val activeWhisperModel: String = "ggml-large-v3-turbo.bin",
    val activeLlmModel: String = "Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
    val quickSlots: Map<String, String?> = mapOf(
        "1" to "hinglish-clean.md",
        "2" to "numbered-list.md",
        "3" to "email.md",
        "4" to "whatsapp-message.md",
        "5" to null,
    ),
    val activeSlot: Int = 1,
    val windowBounds: WindowBounds? = null,
    val debugLogging: Boolean = false,
    // Installed sidecar backend per engine ("cpu"/"cuda"/"vulkan"/"metal"); "" = bundled/none.
    val sttBackend: String = "",
    val llmBackend: String = "",
)
