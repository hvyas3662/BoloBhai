package com.bolobhai.platform

import java.nio.file.Path

/**
 * Launches the bundled llama.cpp server sidecar (BoloBhai.md §9/§16). Returns null when the binary
 * or model is absent so the supervisor reports NotInstalled (graceful: post-processing treated off).
 * Failures + server output go to [com.bolobhai.log.Logger].
 */
object LlamaLauncher {
    /** [backend] "cuda"/"vulkan"/"metal" offload all layers to the GPU; "cpu"/"" stay on CPU. */
    fun launch(port: Int, modelPath: Path, backend: String = ""): Process? {
        val ngl = if (backend in GPU_BACKENDS) "99" else "0"
        return SidecarProcess.launch(
            name = "llama",
            exe = Sidecars.llamaServer(),
            missingHint = "sidecars/<os>/llama/llama-server",
            modelPath = modelPath,
            args = listOf(
                "--host", "127.0.0.1", "--port", port.toString(), "-m", modelPath.toString(),
                "-c", "4096", "-ngl", ngl, "--parallel", "1",
            ),
        )
    }
}

internal val GPU_BACKENDS = setOf("cuda", "vulkan", "metal")
