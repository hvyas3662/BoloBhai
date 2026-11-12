package com.bolobhai.platform

import java.nio.file.Files
import java.nio.file.Path

/**
 * Launches the bundled llama.cpp server sidecar (BoloBhai.md §9/§16). Returns null when the binary
 * or model is absent so the supervisor reports NotInstalled (graceful: post-processing treated off).
 */
object LlamaLauncher {
    fun launch(port: Int, modelPath: Path): Process? {
        val exe = Sidecars.binary("llama-server") ?: return null
        if (!Files.exists(modelPath)) return null
        return ProcessBuilder(
            exe.toString(),
            "--host", "127.0.0.1",
            "--port", port.toString(),
            "-m", modelPath.toString(),
            "-c", "4096",
            "-ngl", "99",
            "--parallel", "1",
        ).redirectErrorStream(true).start()
    }
}
