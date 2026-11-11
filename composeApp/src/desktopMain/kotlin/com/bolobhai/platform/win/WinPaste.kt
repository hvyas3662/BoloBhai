package com.bolobhai.platform.win

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

/**
 * Windows paste + key injection via SendInput (BoloBhai.md §6.5, §10). Must be called with no
 * dictation modifier still logically held.
 */
object WinPaste {

    /** Simulate Ctrl+V into the currently focused window. */
    fun pasteCtrlV() {
        sendKeys(
            KeyStroke(Vk.CONTROL, down = true),
            KeyStroke(Vk.V, down = true),
            KeyStroke(Vk.V, down = false),
            KeyStroke(Vk.CONTROL, down = false),
        )
    }

    /** Inject a harmless key so Windows does not open the menu bar on a lone Alt release. */
    fun injectHarmlessKey() {
        sendKeys(
            KeyStroke(Vk.HARMLESS, down = true),
            KeyStroke(Vk.HARMLESS, down = false),
        )
    }

    private data class KeyStroke(val vk: Int, val down: Boolean)

    private fun sendKeys(vararg strokes: KeyStroke) {
        val inputs = WinUser.INPUT().toArray(strokes.size) as Array<WinUser.INPUT>
        strokes.forEachIndexed { i, stroke ->
            val input = inputs[i]
            input.type = WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD.toLong())
            input.input.setType("ki")
            input.input.ki.wVk = WinDef.WORD(stroke.vk.toLong())
            input.input.ki.dwFlags = WinDef.DWORD(if (stroke.down) 0 else Vk.KEYEVENTF_KEYUP.toLong())
        }
        User32.INSTANCE.SendInput(WinDef.DWORD(strokes.size.toLong()), inputs, inputs[0].size())
    }
}
