package com.bolobhai.platform

import com.bolobhai.log.Logger
import java.nio.file.Files
import java.nio.file.Path

/** Shared sidecar launch + logging for whisper/llama (BoloBhai.md §9). */
internal object SidecarProcess {

    private val SIDECAR_EXES = setOf("whisper-server.exe", "llama-server.exe", "whisper-server", "llama-server")

    /**
     * Kill sidecar servers orphaned by a previous crash. Windows doesn't kill children when the
     * parent dies, so a crashed session leaves whisper/llama holding RAM + VRAM — which then makes
     * the next session's model load fail with CUDA out-of-memory. Call once at startup.
     */
    fun killStale() {
        ProcessHandle.allProcesses()
            .filter { p -> p.info().command().map { it.replace('\\', '/').substringAfterLast('/') in SIDECAR_EXES }.orElse(false) }
            .forEach { p ->
                Logger.warn("killing stale sidecar from a previous session: pid=${p.pid()}")
                p.destroyForcibly()
            }
    }

    fun launch(name: String, exe: Path?, missingHint: String, modelPath: Path, args: List<String>): Process? {
        if (exe == null) {
            Logger.warn("$name: server binary not found (expected $missingHint). Speech/LLM disabled.")
            return null
        }
        if (!Files.exists(modelPath)) {
            Logger.warn("$name: model not found at $modelPath — download it in Models.")
            return null
        }
        return try {
            Logger.info("$name: starting ${exe.fileName} ${args.joinToString(" ")}")
            val process = ProcessBuilder(listOf(exe.toString()) + args)
                .redirectErrorStream(true)
                .start()
            pipeOutput(name, process)
            watchExit(name, process)
            process
        } catch (e: Exception) {
            Logger.error("$name: launch failed — ${e.message}")
            null
        }
    }

    /** Stream the server's stdout/stderr into the log so a crash reason is visible. */
    private fun pipeOutput(name: String, process: Process) {
        Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { Logger.info("[$name] $it") }
            }
        }.apply { isDaemon = true; this.name = "sidecar-log-$name"; start() }
    }

    private fun watchExit(name: String, process: Process) {
        Thread {
            val code = process.waitFor()
            if (code != 0) Logger.error("$name: server exited with code $code")
            else Logger.info("$name: server stopped")
        }.apply { isDaemon = true; this.name = "sidecar-exit-$name"; start() }
    }
}
