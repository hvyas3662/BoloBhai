package com.bolobhai.app

import com.bolobhai.data.ModelEntry
import com.bolobhai.data.ModelRepo
import com.bolobhai.domain.ModelState
import com.bolobhai.domain.SystemHardware
import com.bolobhai.domain.capacityExceeded
import com.bolobhai.domain.parseRamRequirement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One model row's UI state (BoloBhai.md §13.5). [exceedsCapacity]: too big for this machine (#8). */
data class ModelUi(
    val entry: ModelEntry,
    val state: ModelState,
    val active: Boolean,
    val downloading: Boolean,
    val progress: ModelRepo.Progress?,
    val exceedsCapacity: Boolean = false,
)

/**
 * Drives model download/pause/resume/delete/use for the Models screen (BoloBhai.md §13.5).
 * Pausing cancels the job; the `.part` survives so a later Download resumes.
 */
class ModelsController(
    private val appState: AppState,
    private val repo: ModelRepo,
    private val entries: List<ModelEntry>,
    private val scope: CoroutineScope,
    val hardware: SystemHardware,
) {
    private val jobs = mutableMapOf<String, Job>()
    private val progress = mutableMapOf<String, ModelRepo.Progress>()

    private val _models = MutableStateFlow(emptyList<ModelUi>())
    val models: StateFlow<List<ModelUi>> = _models.asStateFlow()

    init { refresh() }

    fun refresh() {
        val s = appState.settings.value
        val limit = hardware.limitBytes
        // The other kind's active model stays resident alongside this one (unless swap mode).
        fun otherActiveRam(kind: String): Long? {
            val otherFile = if (kind == "stt") s.activeLlmModel else s.activeWhisperModel
            return entries.firstOrNull { it.file == otherFile }?.let { parseRamRequirement(it.ram) }
        }
        _models.value = entries.map { e ->
            val active = e.file == (if (e.kind == "stt") s.activeWhisperModel else s.activeLlmModel)
            val exceeds = capacityExceeded(
                modelRamBytes = parseRamRequirement(e.ram),
                otherActiveRamBytes = otherActiveRam(e.kind),
                swap = s.lowVramSwap,
                limitBytes = limit,
            )
            ModelUi(e, repo.stateOf(e), active, jobs.containsKey(e.id), progress[e.id], exceeds)
        }
    }

    fun download(entry: ModelEntry) {
        if (jobs.containsKey(entry.id)) return
        // Blocking HTTP download must run off the UI thread or progress never repaints.
        jobs[entry.id] = scope.launch(Dispatchers.IO) {
            try {
                repo.download(entry) { p -> progress[entry.id] = p; refresh() }
                appState.toast("${entry.name} downloaded · SHA-256 verified")
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Pause — the .part is kept for resume.
            } catch (e: Exception) {
                appState.toast("Download failed: ${e.message ?: "network error"}")
            } finally {
                jobs.remove(entry.id); progress.remove(entry.id); refresh()
            }
        }
        refresh()
    }

    fun pause(entry: ModelEntry) {
        jobs.remove(entry.id)?.cancel()
        progress.remove(entry.id)
        refresh()
    }

    fun delete(entry: ModelEntry) {
        pause(entry)
        repo.delete(entry)
        appState.toast("Model deleted")
        refresh()
    }

    /** Make [entry] the active model for its kind (BoloBhai.md §13.5). */
    fun use(entry: ModelEntry) {
        if (entry.kind == "stt") appState.setActiveWhisperModel(entry.file) else appState.setActiveLlmModel(entry.file)
        appState.toast("${entry.name} is now active · reloading…")
        refresh()
    }
}
