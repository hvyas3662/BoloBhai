package com.bolobhai.data

import com.bolobhai.domain.ModelState
import com.bolobhai.domain.modelState
import com.bolobhai.domain.sha256HexOf
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize

/**
 * Installs/removes model files under `<root>/models` (BoloBhai.md §2.1, §13.5). Downloads resume
 * via HTTP Range into `<file>.part`, then rename and write a `<file>.sha256` sidecar.
 *
 * shortcut: expected SHA-256 is computed from the finished file, not fetched from HF LFS metadata
 * (the spec's `x-linked-etag` HEAD) — upgrade when we need to detect a corrupt/altered upstream.
 */
class ModelRepo(private val dir: Path) {

    fun file(entry: ModelEntry): Path = dir.resolve(entry.file)
    private fun part(entry: ModelEntry): Path = dir.resolve(entry.file + ".part")

    fun stateOf(entry: ModelEntry): ModelState =
        modelState(hasFile = file(entry).exists(), hasPart = part(entry).exists())

    fun delete(entry: ModelEntry) {
        file(entry).deleteIfExists()
        dir.resolve(entry.file + ".sha256").deleteIfExists()
        part(entry).deleteIfExists()
    }

    data class Progress(val done: Long, val total: Long, val bytesPerSec: Long)

    /**
     * Download [entry], resuming from any `.part`. Suspends until done; cancel the calling job to
     * pause (the `.part` survives). Returns the SHA-256 of the finished file.
     */
    suspend fun download(entry: ModelEntry, onProgress: (Progress) -> Unit): String {
        dir.createDirectories()
        val part = part(entry)
        var have = if (part.exists()) part.fileSize() else 0L

        val conn = URI(entry.url).toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        if (have > 0) conn.setRequestProperty("Range", "bytes=$have-")
        conn.connect()

        val total = entry.sizeBytes
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
                        val bps = windowBytes * 1000 / (now - windowStart)
                        onProgress(Progress(have, total, bps))
                        windowBytes = 0; windowStart = now
                    }
                }
            }
        }
        conn.disconnect()

        Files.move(part, file(entry), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        val sha = sha256HexOf(file(entry))
        Files.writeString(dir.resolve(entry.file + ".sha256"), sha)
        onProgress(Progress(have, total, 0))
        return sha
    }
}
