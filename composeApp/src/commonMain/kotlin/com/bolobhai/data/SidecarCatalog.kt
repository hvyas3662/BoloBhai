package com.bolobhai.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A downloadable sidecar backend build from `sidecars.json` (BoloBhai.md §16/§17). Each entry is one
 * engine ("whisper"/"llama") compiled for one backend ("cpu"/"cuda"/"vulkan"/"metal") on one OS,
 * shipped as a zip hosted on GitHub Releases. Picked at runtime from the detected GPU.
 */
@Serializable
data class SidecarEntry(
    val engine: String,   // "whisper" | "llama"
    val backend: String,  // "cpu" | "cuda" | "vulkan" | "metal"
    val os: String,       // "windows" | "macos-arm64" | "macos-x64"
    val url: String,      // direct-download zip (GitHub Release asset)
    val sizeBytes: Long,
    val sha256: String = "", // of the zip; empty = skip verification (shortcut: fill once assets are up)
)

@Serializable
data class SidecarCatalog(val sidecars: List<SidecarEntry>) {

    /** The whisper + llama builds for this OS and backend (empty if the catalog lacks them). */
    fun forBackend(os: String, backend: String): List<SidecarEntry> =
        sidecars.filter { it.os == os && it.backend == backend }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): SidecarCatalog = json.decodeFromString(serializer(), text)

        fun load(): SidecarCatalog {
            val stream = SidecarCatalog::class.java.getResourceAsStream("/sidecars.json")
                ?: error("sidecars.json missing from resources")
            return parse(stream.bufferedReader().use { it.readText() })
        }
    }
}
