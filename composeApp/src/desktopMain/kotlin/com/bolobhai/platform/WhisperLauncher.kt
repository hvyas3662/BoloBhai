package com.bolobhai.platform

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Launches the bundled whisper-server sidecar (BoloBhai.md §9, §17). Returns null when the binary
 * or model is absent so the supervisor reports NotInstalled instead of crashing — the real binary
 * is bundled at packaging time and the model is downloaded in onboarding.
 */
object WhisperLauncher {
    fun launch(port: Int, modelPath: Path): Process? {
        val exe = sidecarBinary() ?: return null
        if (!Files.exists(modelPath)) return null
        return ProcessBuilder(
            exe.toString(),
            "--host", "127.0.0.1",
            "--port", port.toString(),
            "-m", modelPath.toString(),
            "--flash-attn",
        ).redirectErrorStream(true).start()
    }

    private fun sidecarBinary(): Path? {
        val candidate = Path("sidecars", "windows-x64", "whisper-server.exe")
        return candidate.takeIf { Files.exists(it) }
    }
}
