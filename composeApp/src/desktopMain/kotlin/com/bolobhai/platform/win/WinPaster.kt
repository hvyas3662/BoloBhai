package com.bolobhai.platform.win

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Paste text into the previously focused app (BoloBhai.md §10): stash clipboard, set the result,
 * simulate Ctrl+V, restore the old clipboard after a short delay.
 */
object WinPaster {
    fun paste(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val previous = runCatching { clipboard.getContents(null) }.getOrNull()
        clipboard.setContents(StringSelection(text), null)
        WinPaste.pasteCtrlV()
        Thread.sleep(250)
        if (previous != null) runCatching { clipboard.setContents(previous, null) }
    }
}
