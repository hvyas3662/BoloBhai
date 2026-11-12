package com.bolobhai.platform

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Resolves a bundled sidecar binary (whisper-server, llama-server, bolobhai-mac-helper).
 *
 * When packaged, Compose copies `sidecars/<os>/` into the app and exposes it via the
 * `compose.application.resources.dir` system property, so the binaries live inside the install
 * folder next to the app (BoloBhai.md §17). In dev we fall back to the repo's `sidecars/<os>/`.
 */
object Sidecars {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    /** Platform binary name for [base] (adds `.exe` on Windows), resolved path or null if absent. */
    fun binary(base: String): Path? {
        val name = if (isWindows) "$base.exe" else base
        return candidates(name).firstOrNull { Files.exists(it) }
    }

    private fun candidates(name: String): List<Path> = buildList {
        System.getProperty("compose.application.resources.dir")?.let { add(Path(it, name)) }
        add(Path("sidecars", osDir(), name))          // repo root when running from there
        add(Path("..", "sidecars", osDir(), name))    // or from the module dir
    }

    private fun osDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.contains("win") -> "windows"
            os.contains("mac") || os.contains("darwin") -> if (arch.contains("aarch64")) "macos-arm64" else "macos-x64"
            else -> "linux-x64"
        }
    }
}
