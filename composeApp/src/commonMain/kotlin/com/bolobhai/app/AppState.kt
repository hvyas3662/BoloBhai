package com.bolobhai.app

import com.bolobhai.data.Dictation
import com.bolobhai.data.Dictionary
import com.bolobhai.data.DictionaryRepo
import com.bolobhai.data.HistoryRepo
import com.bolobhai.data.NewVersion
import com.bolobhai.data.ProfilesRepo
import com.bolobhai.data.Settings
import com.bolobhai.data.SettingsRepo
import com.bolobhai.domain.Profile
import com.bolobhai.domain.retentionCutoffMs
import com.bolobhai.ui.theme.ThemeMode
import com.bolobhai.ui.theme.asSettingValue
import com.bolobhai.ui.theme.themeModeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Resolves the active Quick Profile slot (BoloBhai.md §7): keep [requested] if it holds a
 * profile, else fall back to the first non-empty slot, else slot 1.
 */
fun resolveActiveSlot(quickSlots: Map<String, String?>, requested: Int): Int {
    if (quickSlots[requested.toString()] != null) return requested
    for (n in 1..5) if (quickSlots[n.toString()] != null) return n
    return 1
}

/** Single source of truth for all windows (BoloBhai.md §4). */
class AppState(
    private val settingsRepo: SettingsRepo,
    val systemDark: Boolean,
    private val profilesRepo: ProfilesRepo? = null,
    private val historyRepo: HistoryRepo? = null,
    private val dictionaryRepo: DictionaryRepo? = null,
) {
    private val _settings = MutableStateFlow(settingsRepo.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _history = MutableStateFlow<List<Dictation>>(emptyList())
    val history: StateFlow<List<Dictation>> = _history.asStateFlow()

    private val _dictionary = MutableStateFlow(dictionaryRepo?.load() ?: Dictionary())
    val dictionary: StateFlow<Dictionary> = _dictionary.asStateFlow()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    /** Slot numbers cleared by the last reload because their file vanished (for a toast). */
    private val _clearedSlots = MutableStateFlow<List<Int>>(emptyList())
    val clearedSlots: StateFlow<List<Int>> = _clearedSlots.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.History)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    /** Transient toast message for the main window (BoloBhai.md §13.7). */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun toast(message: String) { _toast.value = message }
    fun clearToast() { _toast.value = null }

    /** LLM/whisper History actions, wired by the desktop controller which owns the sidecars. */
    var reprocessOp: (suspend (Dictation, Profile) -> Unit)? = null
    var retranscribeOp: (suspend (Dictation) -> Unit)? = null
    var retryOp: (suspend (Dictation) -> Unit)? = null

    private val _theme = MutableStateFlow(themeModeOf(_settings.value.theme))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _activeSlot =
        MutableStateFlow(resolveActiveSlot(_settings.value.quickSlots, _settings.value.activeSlot))
    val activeSlot: StateFlow<Int> = _activeSlot.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    /** Copy shipped defaults if the folder is empty, then load (call once at startup). */
    fun initProfiles() {
        profilesRepo?.ensureDefaults()
        reloadProfiles()
    }

    /** (Re)load profiles from disk and clear any slot whose file disappeared (§11). */
    fun reloadProfiles() {
        val repo = profilesRepo ?: return
        val list = repo.list()
        _profiles.value = list
        val reconcile = reconcileQuickSlots(_settings.value.quickSlots, list.map { it.fileName }.toSet())
        _clearedSlots.value = reconcile.removed
        if (reconcile.removed.isNotEmpty()) {
            val resolved = resolveActiveSlot(reconcile.slots, _settings.value.activeSlot)
            _activeSlot.value = resolved
            persist { it.copy(quickSlots = reconcile.slots, activeSlot = resolved) }
        }
    }

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        persist { it.copy(theme = mode.asSettingValue()) }
    }

    fun setActiveSlot(slot: Int) {
        val resolved = resolveActiveSlot(_settings.value.quickSlots, slot)
        _activeSlot.value = resolved
        persist { it.copy(activeSlot = resolved) }
    }

    /** Assign a profile to a slot (null = clear), keeping a profile in at most one slot (§7). */
    fun assignSlot(slot: Int, fileName: String?) {
        val nextSlots = assignSlotMap(_settings.value.quickSlots, slot, fileName)
        val resolved = resolveActiveSlot(nextSlots, _settings.value.activeSlot)
        _activeSlot.value = resolved
        persist { it.copy(quickSlots = nextSlots, activeSlot = resolved) }
    }

    /** Persist an edited/created profile body and reload. */
    fun saveProfile(profile: Profile) {
        val repo = profilesRepo ?: return
        repo.write(profile)
        reloadProfiles()
    }

    /** Rename a profile's display name (file renamed to slug(name).md), remapping any slot (§13.3). */
    fun renameProfile(oldFile: String, newName: String) {
        val repo = profilesRepo ?: return
        val updated = repo.rename(oldFile, newName)
        if (updated.fileName != oldFile) {
            val slots = _settings.value.quickSlots.mapValues { if (it.value == oldFile) updated.fileName else it.value }
            persist { it.copy(quickSlots = slots) }
        }
        reloadProfiles()
    }

    /** Delete a profile to the OS trash and clear it from any slot (§13.3). */
    fun deleteProfile(fileName: String) {
        val repo = profilesRepo ?: return
        repo.deleteToTrash(fileName)
        val slots = _settings.value.quickSlots.mapValues { if (it.value == fileName) null else it.value }
        persist { it.copy(quickSlots = slots) }
        reloadProfiles()
        toast("Deleted")
    }

    /** Run a profile's prompt on the newest dictation for a preview (§13.3); wired by the controller. */
    var testProfileOp: (suspend (Profile) -> String?)? = null

    /** Open the profiles folder in the OS file manager (§13.3); wired by the desktop entry point. */
    var openProfilesFolder: (() -> Unit)? = null

    fun setPostProcessing(enabled: Boolean) = persist { it.copy(postProcessing = enabled) }
    fun setLowVramSwap(enabled: Boolean) = persist { it.copy(lowVramSwap = enabled) }
    fun setLanguage(language: String) = persist { it.copy(language = language) }
    fun setHotkeyWindows(key: String) = persist { it.copy(hotkey = it.hotkey.copy(windows = key)) }
    fun setDoubleTapMs(ms: Int) = persist { it.copy(doubleTapMs = ms) }
    fun setMicrophone(name: String?) = persist { it.copy(microphone = name) }
    fun setSounds(enabled: Boolean) = persist { it.copy(sounds = enabled) }
    fun setKeepAudio(value: String) = persist { it.copy(keepAudio = value) }
    fun setActiveWhisperModel(file: String) = persist { it.copy(activeWhisperModel = file) }
    fun setActiveLlmModel(file: String) = persist { it.copy(activeLlmModel = file) }
    fun setOnboardingDone(done: Boolean) = persist { it.copy(onboardingDone = done) }
    fun setSttBackend(backend: String) = persist { it.copy(sttBackend = backend) }
    fun setLlmBackend(backend: String) = persist { it.copy(llmBackend = backend) }

    /** Launch-at-login toggles the OS entry via [applyLaunchAtLogin] then persists (BoloBhai.md §13.6). */
    var applyLaunchAtLogin: ((Boolean) -> Unit)? = null
    fun setLaunchAtLogin(enabled: Boolean) {
        applyLaunchAtLogin?.invoke(enabled)
        persist { it.copy(launchAtLogin = enabled) }
    }

    /** Delete audio older than the retention window (startup + periodic; BoloBhai.md §13.6). */
    fun runAudioRetention(now: Long = System.currentTimeMillis()) {
        val cutoff = retentionCutoffMs(_settings.value.keepAudio, now) ?: return
        val repo = historyRepo ?: return
        deleteAudioFiles(repo.clearAudioOlderThan(cutoff))
        loadHistory()
    }

    fun saveDictionary(dict: Dictionary) {
        _dictionary.value = dict
        dictionaryRepo?.save(dict)
    }

    /** (Re)load all dictations from the history store (call once at startup). */
    fun loadHistory() {
        _history.value = historyRepo?.list().orEmpty()
    }

    /** Persist a finished dictation (pipeline terminal states) and refresh the flow. */
    fun saveDictation(
        createdAt: Long,
        durationMs: Long,
        rawTranscript: String,
        audioPath: String?,
        status: String,
        version: NewVersion,
    ): Dictation? {
        val repo = historyRepo ?: return null
        val s = _settings.value
        val item = repo.insert(
            createdAt = createdAt,
            durationMs = durationMs,
            rawTranscript = rawTranscript,
            audioPath = audioPath,
            status = status,
            language = s.language,
            whisperModel = s.activeWhisperModel,
            version = version,
        )
        loadHistory()
        return item
    }

    /** Append a re-processed/re-transcribed version (becomes current) and refresh. */
    fun addVersion(historyId: Long, version: NewVersion): Dictation? {
        val item = historyRepo?.addVersion(historyId, version)
        loadHistory()
        return item
    }

    fun setStatus(historyId: Long, status: String) {
        historyRepo?.updateStatus(historyId, status)
        loadHistory()
    }

    fun setRawTranscript(historyId: Long, raw: String) {
        historyRepo?.updateRaw(historyId, raw)
        loadHistory()
    }

    /** Delete one dictation and its audio file (BoloBhai.md §13.1 More menu). */
    fun deleteDictation(id: Long) {
        deleteAudioFiles(listOfNotNull(historyRepo?.delete(id)))
        loadHistory()
        toast("Deleted")
    }

    /** Clear all history and audio (BoloBhai.md §13.6 danger action). */
    fun clearHistory() {
        deleteAudioFiles(historyRepo?.clearAll().orEmpty())
        loadHistory()
        toast("History cleared")
    }

    private fun deleteAudioFiles(paths: List<String>) {
        paths.forEach { runCatching { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(it)) } }
    }

    private fun persist(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        settingsRepo.save(next)
    }
}
