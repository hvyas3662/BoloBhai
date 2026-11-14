package com.bolobhai.sidecar

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Starts and health-checks a sidecar process, restarting on failure with back-off (BoloBhai.md §9).
 * Dependencies are injected so the logic is testable without a real binary; [launch] returning
 * null means the binary is not installed (graceful: status -> NotInstalled, no crash).
 */
class ProcessSupervisor(
    private val launch: () -> Process?,
    private val healthCheck: suspend () -> Boolean,
    // Used from the first retry on, when the primary (GPU) launch never gets healthy — e.g. a
    // low-VRAM iGPU can't fit the model with -ngl 99 and the server crashes. Falls back to CPU args.
    private val fallbackLaunch: (() -> Process?)? = null,
    private val backoff: BackoffPolicy = BackoffPolicy(),
    private val healthRetryMs: Long = 500,
    // Large models load slowly on CPU (whisper ~10 s, llama ~5 s), and the server only answers
    // /health once loaded — so wait up to ~2 min before giving up and restarting.
    private val maxHealthChecks: Int = 240,
    private val delayer: suspend (Long) -> Unit = { delay(it) },
) {
    private val _status = MutableStateFlow(SidecarStatus.Stopped)
    val status: StateFlow<SidecarStatus> = _status.asStateFlow()

    private var process: Process? = null

    suspend fun start(): SidecarStatus {
        var attempt = 0
        while (true) {
            _status.value = SidecarStatus.Starting
            val p = (if (attempt > 0 && fallbackLaunch != null) fallbackLaunch else launch)()
            if (p == null) {
                _status.value = SidecarStatus.NotInstalled
                return SidecarStatus.NotInstalled
            }
            process = p

            repeat(maxHealthChecks) {
                if (healthCheck()) {
                    _status.value = SidecarStatus.Ready
                    return SidecarStatus.Ready
                }
                delayer(healthRetryMs)
            }

            p.destroy()
            val wait = backoff.delayFor(attempt)
            if (wait == null) {
                _status.value = SidecarStatus.Failed
                return SidecarStatus.Failed
            }
            delayer(wait)
            attempt++
        }
    }

    fun stop() {
        process?.destroy()
        process = null
        _status.value = SidecarStatus.Stopped
    }
}
