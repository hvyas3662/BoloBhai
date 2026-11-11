package com.bolobhai.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.ClosedWatchServiceException
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchService
import kotlin.io.path.createDirectories

/**
 * Watches the profiles folder and fires [onChanged] after a 300 ms debounce (BoloBhai.md §2/§11).
 */
class ProfilesWatcher(
    private val dir: Path,
    private val scope: CoroutineScope,
    private val debounceMs: Long = 300,
) {
    private var watchService: WatchService? = null
    private var watchJob: Job? = null
    private var debounceJob: Job? = null

    fun start(onChanged: () -> Unit) {
        dir.createDirectories()
        val ws = dir.fileSystem.newWatchService()
        dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        watchService = ws
        watchJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val key = ws.take()
                    key.pollEvents()
                    key.reset()
                    debounceJob?.cancel()
                    debounceJob = launch {
                        delay(debounceMs)
                        onChanged()
                    }
                }
            } catch (_: ClosedWatchServiceException) {
                // stopped
            } catch (_: InterruptedException) {
                // stopped
            }
        }
    }

    fun stop() {
        runCatching { watchService?.close() }
        watchJob?.cancel()
    }
}
