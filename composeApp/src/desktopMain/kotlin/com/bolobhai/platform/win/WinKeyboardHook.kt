package com.bolobhai.platform.win

import com.bolobhai.domain.HotkeyInput
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

private const val WM_KEYDOWN = 0x0100
private const val WM_KEYUP = 0x0101
private const val WM_SYSKEYDOWN = 0x0104
private const val WM_SYSKEYUP = 0x0105

/**
 * System-wide WH_KEYBOARD_LL hook on a dedicated thread with a message loop (BoloBhai.md §6.5).
 * The callback stays cheap: it maps raw keys to [HotkeyInput]s, asks [onInput] whether to swallow,
 * and handles the Alt-menu-suppression / Alt+other pass-through rules.
 *
 * NOTE: a global keyboard hook can only be verified interactively on Windows — this compiles but
 * is not exercised by headless tests.
 */
class WinKeyboardHook(
    private val dictationKey: DictationKey,
    private val onInput: (HotkeyInput) -> Boolean,
    private val isRecording: () -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val vks = dictationVks(dictationKey)
    private var thread: Thread? = null
    private var hook: WinUser.HHOOK? = null

    // Strong ref: the proc must not be GC'd while installed.
    private var proc: WinUser.LowLevelKeyboardProc? = null

    @Volatile private var dictationDown = false
    @Volatile private var gestureActive = false
    @Volatile private var altCombined = false

    fun start() {
        if (thread != null) return
        thread = Thread {
            val hMod = Kernel32.INSTANCE.GetModuleHandle(null)
            val callback = WinUser.LowLevelKeyboardProc { nCode, wParam, info ->
                var swallow = false
                if (nCode >= 0) {
                    swallow = runCatching { handle(wParam.toInt(), info.vkCode) }.getOrDefault(false)
                }
                if (swallow) {
                    WinDef.LRESULT(1)
                } else {
                    User32.INSTANCE.CallNextHookEx(
                        hook, nCode, wParam,
                        WinDef.LPARAM(Pointer.nativeValue(info.pointer)),
                    )
                }
            }
            proc = callback
            hook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, callback, hMod, 0)

            val msg = WinUser.MSG()
            while (!Thread.currentThread().isInterrupted &&
                User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0
            ) {
                User32.INSTANCE.TranslateMessage(msg)
                User32.INSTANCE.DispatchMessage(msg)
            }
        }.apply {
            name = "bolobhai-win-hook"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        hook?.let { User32.INSTANCE.UnhookWindowsHookEx(it) }
        hook = null
        proc = null
        thread?.interrupt()
        thread = null
    }

    private fun handle(wParam: Int, vk: Int): Boolean {
        val down = wParam == WM_KEYDOWN || wParam == WM_SYSKEYDOWN
        val up = wParam == WM_KEYUP || wParam == WM_SYSKEYUP

        return when (val key = classifyVk(vk, vks)) {
            RawKey.Dictation -> onDictation(down, up)
            is RawKey.Digit -> {
                if (down) onInput(HotkeyInput.Digit(key.n)) else isRecording()
            }
            RawKey.Esc -> {
                if (down) onInput(HotkeyInput.Esc) else isRecording()
            }
            RawKey.Other -> {
                // Alt + another key (Alt+Tab, Alt+F4…) passes through and cancels the gesture.
                if (dictationDown && gestureActive) {
                    altCombined = true
                    onInput(HotkeyInput.Esc)
                }
                false
            }
        }
    }

    private fun onDictation(down: Boolean, up: Boolean): Boolean = when {
        down && !dictationDown -> {
            dictationDown = true
            gestureActive = true
            altCombined = false
            onInput(HotkeyInput.KeyDown)
            false // never swallow the dictation keydown
        }
        down -> false // auto-repeat
        up -> {
            dictationDown = false
            val swallow = onInput(HotkeyInput.KeyUp)
            gestureActive = false
            if (dictationKey == DictationKey.Alt && !altCombined) {
                // Inject a dummy key so Windows treats Alt as a combo (no menu bar), then let the
                // real Alt key-up PASS THROUGH — swallowing it would leave Alt logically stuck down.
                WinPaste.injectHarmlessKey()
                false
            } else {
                swallow
            }
        }
        else -> false
    }
}
