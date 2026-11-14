package com.bolobhai.app

import com.bolobhai.data.SidecarCatalog
import com.bolobhai.data.SidecarEntry
import com.bolobhai.data.SidecarRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One engine row's install state for the onboarding "engine" step. */
data class SidecarUi(
    val entry: SidecarEntry,
    val installed: Boolean,
    val downloading: Boolean,
    val progress: SidecarRepo.Progress?,
)

/**
 * Downloads the GPU-matched whisper + llama backend builds and records the installed backend in
 * settings, so launchers pick the right args (BoloBhai.md §16/§17). Mirrors [ModelsController];
 * [backend] is what the platform detected (GpuDetect) and [os] the Sidecars OS folder name.
 */
class SidecarsController(
    private val appState: AppState,
    private val repo: SidecarRepo,
    catalog: SidecarCatalog,
    private val os: String,
    private val backend: String,
    private val scope: CoroutineScope,
) {
    private val entries = catalog.forBackend(os, backend)
    private val jobs = mutableMapOf<String, Job>()
    private val progress = mutableMapOf<String, SidecarRepo.Progress>()

    private val _rows = MutableStateFlow(emptyList<SidecarUi>())
    val rows: StateFlow<List<SidecarUi>> = _rows.asStateFlow()

    init { refresh() }

    private fun key(e: SidecarEntry) = "${e.os}-${e.engine}"

    private fun refresh() {
        _rows.value = entries.map { e ->
            SidecarUi(e, repo.isInstalled(e), jobs.containsKey(key(e)), progress[key(e)])
        }
    }

    /** Download every not-yet-installed engine for the detected backend. */
    fun installMissing() = entries.filterNot { repo.isInstalled(it) }.forEach { install(it) }

    private fun install(entry: SidecarEntry) {
        val k = key(entry)
        if (jobs.containsKey(k)) return
        jobs[k] = scope.launch(Dispatchers.IO) {
            try {
                repo.install(entry) { p -> progress[k] = p; refresh() }
                if (entry.engine == "whisper") appState.setSttBackend(entry.backend)
                else appState.setLlmBackend(entry.backend)
                appState.toast("${entry.engine} ($backend) installed")
            } catch (_: kotlinx.coroutines.CancellationException) {
            } catch (e: Exception) {
                appState.toast("${entry.engine} download failed: ${e.message ?: "network error"}")
            } finally {
                jobs.remove(k); progress.remove(k); refresh()
            }
        }
        refresh()
    }

    fun pause() {
        jobs.values.forEach { it.cancel() }
        jobs.clear(); progress.clear(); refresh()
    }
}
