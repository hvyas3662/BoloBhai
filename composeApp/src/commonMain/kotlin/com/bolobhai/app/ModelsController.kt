package com.bolobhai.app

import com.bolobhai.data.ModelEntry
import com.bolobhai.data.ModelRepo
import com.bolobhai.domain.ModelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One model row's UI state (BoloBhai.md §13.5). */
data class ModelUi(
    val entry: ModelEntry,
    val state: ModelState,
    val active: Boolean,
    val downloading: Boolean,
    val progress: ModelRepo.Progress?,
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
) {
    private val jobs = mutableMapOf<String, Job>()
    private val progress = mutableMapOf<String, ModelRepo.Progress>()

    private val _models = MutableStateFlow(emptyList<ModelUi>())
    val models: StateFlow<List<ModelUi>> = _models.asStateFlow()

    init { refresh() }

    fun refresh() {
        val s = appState.settings.value
        _models.value = entries.map { e ->
            val active = e.file == (if (e.kind == "stt") s.activeWhisperModel else s.activeLlmModel)
            ModelUi(e, repo.stateOf(e), active, jobs.containsKey(e.id), progress[e.id])
        }
    }

    fun download(entry: ModelEntry) {
        if (jobs.containsKey(entry.id)) return
        jobs[entry.id] = scope.launch {
            try {
                repo.download(entry) { p -> progress[entry.id] = p; refresh() }
                appState.toast("${entry.name} downloaded · SHA-256 verified")
            } catch (_: Exception) {
                // cancellation (pause) or network error — the .part is kept for resume.
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
        appState.toast("${entry.name} is now active · server restarts on next launch")
        refresh()
    }
}
