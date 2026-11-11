package com.bolobhai.ui.pill

import com.bolobhai.ui.components.SlotKeyVisual

enum class PillPhase { Hold, HandsFree, Processing, Pasted, Failed }

/** Everything the recording pill needs to render (BoloBhai.md §8). */
data class PillUiState(
    val phase: PillPhase,
    val elapsedMs: Long = 0,
    val levels: List<Float> = List(14) { 0f }, // 14 bars, 0..1
    val slots: List<SlotKeyVisual> = List(5) { SlotKeyVisual.Empty },
    val profileShort: String = "Raw",
    val hotkeyLabel: String = "fn",
    val pasteShortcut: String = "Ctrl+V",
    val failedMessage: String? = null,
)
