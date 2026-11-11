package com.bolobhai.platform.win

/** Which physical key triggers dictation on Windows (BoloBhai.md §6.1). */
enum class DictationKey { Alt, RightCtrl, Fn }

fun dictationKeyFromSetting(value: String): DictationKey = when (value.lowercase().replace(" ", "")) {
    "rightctrl" -> DictationKey.RightCtrl
    "fn" -> DictationKey.Fn
    else -> DictationKey.Alt
}

/** Windows virtual-key codes we care about. */
object Vk {
    const val ESCAPE = 0x1B
    const val MENU = 0x12      // Alt
    const val LMENU = 0xA4
    const val RMENU = 0xA5
    const val CONTROL = 0x11
    const val LCONTROL = 0xA2
    const val RCONTROL = 0xA3
    const val TAB = 0x09
    const val V = 0x56
    const val HARMLESS = 0xFF  // unassigned — injected to swallow a lone Alt
    const val KEYEVENTF_KEYUP = 0x0002
}

fun dictationVks(key: DictationKey): Set<Int> = when (key) {
    DictationKey.Alt -> setOf(Vk.MENU, Vk.LMENU, Vk.RMENU)
    DictationKey.RightCtrl -> setOf(Vk.RCONTROL)
    DictationKey.Fn -> emptySet() // handled in firmware; onboarding falls back to Alt (§6.5)
}

/** Classification of a virtual-key while the low-level hook is active. */
sealed interface RawKey {
    data object Dictation : RawKey
    data class Digit(val n: Int) : RawKey
    data object Esc : RawKey
    data object Other : RawKey
}

fun classifyVk(vk: Int, dictationVks: Set<Int>): RawKey = when {
    vk in dictationVks -> RawKey.Dictation
    vk == Vk.ESCAPE -> RawKey.Esc
    vk in 0x31..0x35 -> RawKey.Digit(vk - 0x30)   // top-row 1..5
    vk in 0x61..0x65 -> RawKey.Digit(vk - 0x60)   // numpad 1..5
    else -> RawKey.Other
}
