package com.bolobhai.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText

/**
 * Installs backend builds under `<root>/<os>/<engine>/` (BoloBhai.md §16/§17). Downloads the zip,
 * resuming via HTTP Range into a `.part`, verifies its SHA-256 (when the catalog gives one), then
 * replaces the engine folder with the fresh extraction and writes a `.installed` marker holding the
 * backend — so a later launch knows which backend is live and whether anything is installed.
 */
class SidecarRepo(private val dir: Path) {

    private fun engineDir(e: SidecarEntry): Path = dir.resolve(e.os).resolve(e.engine)
    private fun marker(e: SidecarEntry): Path = engineDir(e).resolve(".installed")
    private fun part(e: SidecarEntry): Path = dir.resolve("${e.os}-${e.engine}.zip.part")

    /** The backend currently installed for this engine, or null if none. */
    fun installedBackend(os: String, engine: String): String? {
        val m = dir.resolve(os).resolve(engine).resolve(".installed")
        return if (m.exists()) m.readText().trim().ifEmpty { null } else null
    }

    fun isInstalled(e: SidecarEntry): Boolean = installedBackend(e.os, e.engine) == e.backend

    data class Progress(val done: Long, val total: Long, val bytesPerSec: Long)

    /** Download + install [entry], resuming from any `.part`. Cancel the calling job to pause. */
    suspend fun install(entry: SidecarEntry, onProgress: (Progress) -> Unit) {
        dir.createDirectories()
        val part = part(entry)
        var have = if (part.exists()) part.fileSize() else 0L

        val conn = URI(entry.url).toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
        conn.connect()

        conn.inputStream.use { input ->
            Files.newOutputStream(
                part,
                java.nio.file.StandardOpenOption.CREATE,
                if (have > 0) java.nio.file.StandardOpenOption.APPEND else java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            ).use { out ->
                val buf = ByteArray(1 shl 16)
                var windowBytes = 0L
                var windowStart = System.currentTimeMillis()
                while (true) {
                    currentCoroutineContext().ensureActive() // cancellation = pause
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    have += n
                    windowBytes += n
                    val now = System.currentTimeMillis()
                    if (now - windowStart >= 400) {
                        onProgress(Progress(have, entry.sizeBytes, windowBytes * 1000 / (now - windowStart)))
                        windowBytes = 0; windowStart = now
                    }
                }
            }
        }
        conn.disconnect()

        if (entry.sha256.isNotEmpty()) {
            val actual = sha256HexOf(part)
            require(actual == entry.sha256) { "SHA-256 mismatch for ${entry.engine} ${entry.backend}" }
        }

        extract(part, engineDir(entry))
        Files.writeString(marker(entry), entry.backend)
        part.deleteIfExists()
        onProgress(Progress(have, entry.sizeBytes, 0))
    }

    /** SHA-256 of [file], streamed in chunks — the sidecar zips are ~700 MB, so never read whole. */
    internal fun sha256HexOf(file: Path): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Replace [target] with a clean extraction of the zip (stale DLLs from a prior backend gone). */
    internal fun extract(zip: Path, target: Path) {
        if (target.exists()) target.toFile().deleteRecursively()
        target.createDirectories()
        Files.newInputStream(zip).use { raw ->
            ZipInputStream(raw).use { zin ->
                var e = zin.nextEntry
                while (e != null) {
                    val out = target.resolve(e.name).normalize()
                    require(out.startsWith(target)) { "zip entry escapes target: ${e.name}" } // zip-slip guard
                    if (e.isDirectory) {
                        out.createDirectories()
                    } else {
                        out.parent.createDirectories()
                        Files.newOutputStream(out).use { zin.copyTo(it) }
                        out.toFile().setExecutable(true) // mac dylibs/exe need the bit; harmless on win
                    }
                    e = zin.nextEntry
                }
            }
        }
    }
}
