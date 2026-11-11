package com.bolobhai.platform.mac

import com.bolobhai.domain.HotkeyInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Drives the `bolobhai-mac-helper` CLI over its one-JSON-object-per-line protocol (BoloBhai.md
 * §6.4): reads fn/digit/Esc events, and sends capture/paste/activation/openPrefs commands.
 *
 * UNVERIFIED: the macOS path cannot run on Windows/headless — compiles here, needs a Mac + the
 * built helper binary. Absent binary => [start] returns false and dictation stays unavailable.
 */
class MacHelper(private val binary: Path) {
    private var process: Process? = null
    private var reader: Thread? = null

    fun start(onEvent: (HotkeyInput) -> Unit): Boolean {
        if (!Files.isExecutable(binary)) return false
        val p = runCatching {
            ProcessBuilder(binary.toString()).redirectErrorStream(false).start()
        }.getOrNull() ?: return false
        process = p
        reader = Thread {
            p.inputStream.bufferedReader().use { br -> readLoop(br, onEvent) }
        }.apply { name = "bolobhai-mac-helper"; isDaemon = true; start() }
        return true
    }

    private fun readLoop(br: BufferedReader, onEvent: (HotkeyInput) -> Unit) {
        while (true) {
            val line = br.readLine() ?: break
            val input = parseHelperEvent(line) ?: continue
            onEvent(input)
        }
    }

    fun setCapture(on: Boolean) = send("""{"cmd":"capture","on":$on}""")
    fun paste() = send("""{"cmd":"paste"}""")
    fun setActivationPolicy(regular: Boolean) =
        send("""{"cmd":"activationPolicy","policy":"${if (regular) "regular" else "accessory"}"}""")
    fun openPrefs(pane: String) = send("""{"cmd":"openPrefs","pane":"$pane"}""")

    fun stop() {
        process?.destroy()
        process = null
    }

    @Synchronized
    private fun send(json: String) {
        val os = process?.outputStream ?: return
        runCatching { os.write((json + "\n").toByteArray()); os.flush() }
    }
}

/** Map one helper line to a [HotkeyInput]; null for non-gesture lines (perm, error, …). */
internal fun parseHelperEvent(line: String): HotkeyInput? {
    val obj = runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull() ?: return null
    return when (obj["ev"]?.jsonPrimitive?.content) {
        "fn" -> if (obj["down"]?.jsonPrimitive?.content == "true") HotkeyInput.KeyDown else HotkeyInput.KeyUp
        "key" -> when (val k = obj["k"]?.jsonPrimitive?.content) {
            "esc" -> HotkeyInput.Esc
            in listOf("1", "2", "3", "4", "5") -> HotkeyInput.Digit(k!!.toInt())
            else -> null
        }
        else -> null
    }
}
