package com.bolobhai.platform

import java.nio.file.Files
import java.nio.file.Path

/**
 * Launches the bundled whisper-server sidecar (BoloBhai.md §9, §17). Returns null when the binary
 * or model is absent so the supervisor reports NotInstalled instead of crashing — the real binary
 * is bundled at packaging time and the model is downloaded in onboarding. Failures and the server's
 * own output are sent to [com.bolobhai.log.Logger] so they show up in the Logs panel.
 */
object WhisperLauncher {
    fun launch(port: Int, modelPath: Path): Process? = SidecarProcess.launch(
        name = "whisper",
        exe = Sidecars.whisperServer(),
        missingHint = "sidecars/<os>/whisper/whisper-server",
        modelPath = modelPath,
        args = listOf("--host", "127.0.0.1", "--port", port.toString(), "-m", modelPath.toString(), "--flash-attn"),
    )
}
