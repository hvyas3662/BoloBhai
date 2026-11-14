package com.bolobhai.platform

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Resolves a bundled sidecar binary (whisper-server, llama-server, bolobhai-mac-helper).
 *
 * Each engine lives in its own subfolder (`whisper/`, `llama/`) with its own DLLs/dylibs, because
 * whisper.cpp and llama.cpp ship different ggml versions that must not collide in one folder.
 *
 * When packaged, Compose copies `sidecars/<os>/` into the app and exposes it via the
 * `compose.application.resources.dir` system property, so the binaries live inside the install
 * folder next to the app (BoloBhai.md §17). In dev we fall back to the repo's `sidecars/<os>/`.
 */
object Sidecars {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    /** Set at startup to `<app-data>/sidecars`; backends downloaded at runtime land here and win. */
    var downloadedRoot: Path? = null

    fun whisperServer(): Path? = resolve("whisper", "whisper-server")
    fun llamaServer(): Path? = resolve("llama", "llama-server")
    fun macHelper(): Path? = resolve(null, "bolobhai-mac-helper")

    private fun resolve(subdir: String?, base: String): Path? {
        val name = if (isWindows) "$base.exe" else base
        return candidates(subdir, name).firstOrNull { Files.exists(it) }
    }

    private fun candidates(subdir: String?, name: String): List<Path> {
        val rel = if (subdir != null) arrayOf(subdir, name) else arrayOf(name)
        return buildList {
            downloadedRoot?.let { add(Path(it.toString(), osDir(), *rel)) }
            System.getProperty("compose.application.resources.dir")?.let { add(Path(it, *rel)) }
            add(Path("sidecars", osDir(), *rel))       // repo root when running from there
            add(Path("..", "sidecars", osDir(), *rel))  // or from the module dir
        }
    }

    fun osDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.contains("win") -> "windows"
            os.contains("mac") || os.contains("darwin") -> if (arch.contains("aarch64")) "macos-arm64" else "macos-x64"
            else -> "linux-x64"
        }
    }
}
