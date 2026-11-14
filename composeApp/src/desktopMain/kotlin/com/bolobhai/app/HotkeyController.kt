package com.bolobhai.app

import com.bolobhai.audio.AudioRecorder
import com.bolobhai.audio.Sounds
import com.bolobhai.audio.pcm16ToWav
import com.bolobhai.domain.HotkeyEffect
import com.bolobhai.domain.HotkeyInput
import com.bolobhai.domain.HotkeyStateMachine
import com.bolobhai.domain.Profile
import com.bolobhai.domain.RecMode
import com.bolobhai.domain.applyCorrections
import com.bolobhai.domain.capitalizeFirst
import com.bolobhai.domain.cleanupLlmOutput
import com.bolobhai.domain.whisperPrompt
import com.bolobhai.domain.shortProfileName
import com.bolobhai.sidecar.LlamaClient
import com.bolobhai.sidecar.ProcessSupervisor
import com.bolobhai.sidecar.SidecarStatus
import com.bolobhai.sidecar.WhisperClient
import com.bolobhai.data.Dictation
import com.bolobhai.data.NewVersion
import com.bolobhai.ui.components.slotKeyVisual
import com.bolobhai.ui.pill.PillPhase
import com.bolobhai.ui.pill.PillUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Ties the hotkey hook to the gesture state machine, recorder, pill and the whisper + llama
 * sidecars, then pastes the result (BoloBhai.md §9). Post-processing runs the active profile's
 * body as the LLM system prompt with a 15 s timeout; on failure/timeout the corrected raw
 * transcript is pasted instead. Sidecars absent => graceful degradation (no crash).
 */
class HotkeyController(
    private val appState: AppState,
    private val hotkeyLabel: String,
    private val pasteShortcut: String,
    launchWhisper: (port: Int) -> Process?,
    launchLlama: (port: Int) -> Process?,
    // CPU-args launchers; the supervisor falls back to these if the GPU launch never gets healthy
    // (e.g. a low-VRAM iGPU can't fit the model). Default to the primary when GPU isn't in play.
    launchWhisperCpu: (port: Int) -> Process? = launchWhisper,
    launchLlamaCpu: (port: Int) -> Process? = launchLlama,
    private val paste: (String) -> Unit,
    private val audioDir: Path,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sm = HotkeyStateMachine(slotAssigned = { slot ->
        appState.settings.value.quickSlots[slot.toString()] != null
    })
    private val recorder = AudioRecorder()

    private val whisperPort = freePort()
    private val whisper = WhisperClient("http://127.0.0.1:$whisperPort")
    private val whisperSupervisor = ProcessSupervisor(
        launch = { launchWhisper(whisperPort) },
        fallbackLaunch = { launchWhisperCpu(whisperPort) },
        healthCheck = { whisper.health() },
    )

    private val llamaPort = freePort()
    private val llama = LlamaClient("http://127.0.0.1:$llamaPort")
    private val llamaSupervisor = ProcessSupervisor(
        launch = { launchLlama(llamaPort) },
        fallbackLaunch = { launchLlamaCpu(llamaPort) },
        healthCheck = { llama.health() },
    )

    val whisperStatus: StateFlow<SidecarStatus> get() = whisperSupervisor.status
    val llamaStatus: StateFlow<SidecarStatus> get() = llamaSupervisor.status

    private val _pill = MutableStateFlow<PillUiState?>(null)
    val pill: StateFlow<PillUiState?> = _pill.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    @Volatile private var levels = List(14) { 0f }
    @Volatile private var recordingStartMs = 0L
    @Volatile private var llmMissingNotified = false
    private var tickerJob: Job? = null

    fun startSidecars() {
        scope.launch { whisperSupervisor.status.collect { com.bolobhai.log.Logger.info("whisper status: $it") } }
        scope.launch { llamaSupervisor.status.collect { com.bolobhai.log.Logger.info("llama status: $it") } }
        scope.launch { whisperSupervisor.start() }
        // Low-VRAM mode loads llama on demand (freeing whisper's VRAM first); otherwise keep it warm.
        if (!appState.settings.value.lowVramSwap) scope.launch { llamaSupervisor.start() }

        // Live-apply model / backend / low-VRAM changes: restart the sidecars with the new settings
        // instead of forcing an app restart. The launchers read settings fresh on each (re)start.
        scope.launch {
            appState.settings
                .map { listOf(it.activeWhisperModel, it.activeLlmModel, it.sttBackend, it.llmBackend, it.lowVramSwap.toString()) }
                .distinctUntilChanged()
                .drop(1) // skip the value present at startup
                .collect { restartSidecars() }
        }
    }

    /** Stop both sidecars and bring them back under the current settings (whisper always; llama warm
     *  only outside low-VRAM mode, where it loads per dictation). */
    private fun restartSidecars() {
        whisperSupervisor.stop()
        llamaSupervisor.stop()
        scope.launch { whisperSupervisor.start() }
        if (!appState.settings.value.lowVramSwap) scope.launch { llamaSupervisor.start() }
    }

    /**
     * Run [block] with llama ready. In low-VRAM mode: stop whisper (free ~1.6 GB VRAM), load llama
     * fully on the GPU, run, then unload llama and warm whisper back up for the next dictation.
     * Returns null if llama can't be made ready. In normal mode: run only when llama is already warm.
     */
    private suspend fun <T> withLlamaReady(block: suspend () -> T): T? {
        if (!appState.settings.value.lowVramSwap) {
            return if (llamaSupervisor.status.value == SidecarStatus.Ready) block() else null
        }
        whisperSupervisor.stop()
        val ready = llamaSupervisor.start() == SidecarStatus.Ready
        return try {
            if (ready) block() else null
        } finally {
            llamaSupervisor.stop()
            scope.launch { whisperSupervisor.start() }
        }
    }

    /** Kill both sidecar servers (call on full app quit so nothing is left running). */
    fun stopSidecars() {
        whisperSupervisor.stop()
        llamaSupervisor.stop()
    }

    /** §13.1 More menu — re-run the LLM on an item's raw transcript, saved as a new version. */
    suspend fun reprocess(item: Dictation, profile: Profile) {
        val out = runLlm(profile, item.rawTranscript) ?: run { appState.toast("Re-process failed"); return }
        val updated = appState.addVersion(item.id, llmVersion(profile, out))
        appState.toast("Re-processed with ${profile.name} · saved as v${(updated?.currentVersion ?: 0) + 1}")
    }

    /** Pill Retry (§8): re-run the most recent (just-failed) dictation. */
    fun retryLast() {
        scope.launch { appState.history.value.firstOrNull()?.let { retry(it) } }
    }

    /** §9 failed-card Retry — re-run the same profile; on success mark ok and copy to clipboard. */
    suspend fun retry(item: Dictation) {
        val profile = profileOf(item.current?.profileId) ?: run { appState.toast("No profile to retry"); return }
        val out = runLlm(profile, item.rawTranscript) ?: run { appState.toast("Still failing · raw text kept"); return }
        appState.addVersion(item.id, llmVersion(profile, out))
        appState.setStatus(item.id, "ok")
        copyToClipboard(out)
        appState.toast("Cleaned up · copied to clipboard")
    }

    /** §13.1 More menu — re-transcribe the kept audio, then re-run the active profile. */
    suspend fun retranscribe(item: Dictation) {
        val path = item.audioPath ?: run { appState.toast("Audio isn't kept for this item"); return }
        if (whisperSupervisor.status.value != SidecarStatus.Ready) {
            appState.toast("Speech engine not installed"); return
        }
        appState.toast("Re-transcribing with Whisper…")
        val dict = appState.dictionary.value
        val transcript = try {
            whisper.transcribe(Files.readAllBytes(Path.of(path)), appState.settings.value.language, prompt = whisperPrompt(dict))
        } catch (_: Exception) {
            appState.toast("Couldn't transcribe"); return
        }
        if (transcript.isBlank()) { appState.toast("Didn't catch that"); return }
        val raw = applyCorrections(transcript, dict)
        appState.setRawTranscript(item.id, raw)
        val profile = activeProfile()
        val version = if (profile != null && appState.settings.value.postProcessing) {
            runLlm(profile, raw)?.let { llmVersion(profile, it) }
                ?: NewVersion(profile.fileName, profile.name, capitalizeFirst(raw), null, null)
        } else {
            NewVersion(null, "Raw (no LLM)", capitalizeFirst(raw), null, null)
        }
        appState.addVersion(item.id, version)
        appState.toast("Re-transcribed · dictionary applied")
    }

    /** §13.3 "Test on last dictation" — run [profile] on the newest raw transcript, no save. */
    suspend fun testProfile(profile: Profile): String? {
        val item = appState.history.value.firstOrNull() ?: return null
        return runLlm(profile, item.rawTranscript)
    }

    /** Run the profile's prompt with the 15 s timeout; null on timeout/error/empty/not-ready. */
    private suspend fun runLlm(profile: Profile, text: String): String? {
        val raw = withLlamaReady { withTimeoutOrNull(15_000) { llama.complete(profile.body, text) } } ?: return null
        return cleanupLlmOutput(raw).takeIf { it.isNotBlank() }
    }

    private fun llmVersion(profile: Profile, output: String) =
        NewVersion(profile.fileName, profile.name, output, appState.settings.value.activeLlmModel, null)

    private fun profileOf(fileName: String?): Profile? =
        fileName?.let { f -> appState.profiles.value.firstOrNull { it.fileName == f } }

    private fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    fun isRecording(): Boolean = sm.state in RECORDING_STATES

    /** Start a hands-free recording from the tray (§13.9) by replaying a double-tap. */
    fun startHandsFree() {
        if (isRecording()) return
        scope.launch {
            onInput(HotkeyInput.KeyDown); delay(30)
            onInput(HotkeyInput.KeyUp); delay(30)
            onInput(HotkeyInput.KeyDown); delay(30)
            onInput(HotkeyInput.KeyUp)
        }
    }

    /** Fed by the platform hook; returns whether to swallow the key event. */
    fun onInput(input: HotkeyInput): Boolean {
        val now = System.currentTimeMillis()
        val result = sm.onEvent(input, now)
        result.effects.forEach { apply(it, now) }
        return result.swallow
    }

    private fun apply(effect: HotkeyEffect, now: Long) {
        when (effect) {
            is HotkeyEffect.StartRecording -> startRecording(effect.mode, now)
            is HotkeyEffect.ShowPill -> _pill.value = buildPill(toPhase(effect.mode))
            is HotkeyEffect.SetPillMode -> _pill.value = buildPill(toPhase(effect.mode))
            HotkeyEffect.StartDoubleTapTimer -> scheduleDoubleTapTimeout()
            is HotkeyEffect.SwitchSlot -> {
                appState.setActiveSlot(effect.n)
                _pill.value = _pill.value?.let { buildPill(it.phase) }
            }
            HotkeyEffect.StopAndProcess -> runPipeline()
            is HotkeyEffect.Discard -> discard(effect.toast)
        }
    }

    private fun startRecording(mode: RecMode, now: Long) {
        recordingStartMs = now
        levels = List(14) { 0f }
        // Show the pill instantly; opening the mic line can take tens of ms, so do it off the
        // hook thread — otherwise the pill appears with a visible delay.
        _pill.value = buildPill(toPhase(mode))
        startTicker()
        scope.launch {
            try {
                recorder.start(appState.settings.value.microphone) { level -> levels = (levels.drop(1) + level) }
            } catch (e: Exception) {
                // §14: no microphone -> cancel the gesture and tell the user.
                com.bolobhai.log.Logger.error("recorder.start failed: ${e.message}")
                onInput(HotkeyInput.Esc)
                appState.toast("No microphone — check Settings › Audio")
                return@launch
            }
            if (appState.settings.value.sounds) Sounds.start()
        }
    }

    private fun runPipeline() {
        stopTicker()
        if (appState.settings.value.sounds) Sounds.stop()
        _pill.value = buildPill(PillPhase.Processing)
        val createdAt = System.currentTimeMillis()
        val durationMs = createdAt - recordingStartMs
        scope.launch {
            val pcm = try {
                recorder.stop()
            } catch (e: Exception) {
                com.bolobhai.log.Logger.error("recorder.stop failed: ${e.message}")
                finishFailed("Recording failed — see Logs")
                return@launch
            }
            val wav = pcm16ToWav(pcm)
            com.bolobhai.log.Logger.info("pipeline: ${durationMs}ms audio, ${wav.size} bytes")
            try {
                runStages(wav, createdAt, durationMs)
            } catch (e: Exception) {
                // Any unexpected failure is now visible (pill + log + saved) instead of silent.
                com.bolobhai.log.Logger.error("pipeline failed: ${e}")
                runCatching {
                    save(wav, createdAt, durationMs, raw = "", status = "stt_failed",
                        version = NewVersion(null, "—", "", null, null))
                }
                finishFailed("Something went wrong — see Logs")
            }
        }
    }

    private suspend fun runStages(wav: ByteArray, createdAt: Long, durationMs: Long) {
        // In low-VRAM mode whisper may still be warming back up after the previous dictation's swap.
        if (whisperSupervisor.status.value != SidecarStatus.Ready && appState.settings.value.lowVramSwap) {
            var waited = 0
            while (whisperSupervisor.status.value != SidecarStatus.Ready && waited < 15_000) { delay(250); waited += 250 }
        }
        if (whisperSupervisor.status.value != SidecarStatus.Ready) {
            finishFailed("Speech engine not installed")
            return
        }
        val settings = appState.settings.value
        val dict = appState.dictionary.value
        val transcript = try {
            whisper.transcribe(wav, settings.language, prompt = whisperPrompt(dict))
        } catch (e: Exception) {
            // §9 whisper failed -> nothing pasted, audio kept, History status stt_failed.
            com.bolobhai.log.Logger.error("whisper transcribe failed: ${e.message}")
            save(wav, createdAt, durationMs, raw = "", status = "stt_failed",
                version = NewVersion(null, "—", "", null, null))
            finishToast("Couldn't transcribe — audio saved to History")
            return
        }
        com.bolobhai.log.Logger.info("whisper transcript: \"${transcript.take(120)}\"")
        if (transcript.isBlank()) {
            finishToast("Didn't catch that")
            return
        }
        val corrected = applyCorrections(transcript, dict)
        val profile = activeProfile()

        // Normal mode needs llama already warm; low-VRAM mode loads it on demand inside withLlamaReady.
        val canCleanup = settings.postProcessing && profile != null &&
            (settings.lowVramSwap || llamaSupervisor.status.value == SidecarStatus.Ready)
        if (canCleanup && profile != null) {
            val started = System.currentTimeMillis()
            val raw = withLlamaReady { withTimeoutOrNull(15_000) { llama.complete(profile.body, corrected) } }
            val cleaned = raw?.let { cleanupLlmOutput(it) }
            if (!cleaned.isNullOrBlank()) {
                paste(cleaned)
                save(wav, createdAt, durationMs, corrected, status = "ok",
                    version = NewVersion(profile.fileName, profile.name, cleaned,
                        settings.activeLlmModel, System.currentTimeMillis() - started))
                finishPasted()
            } else {
                // §9: LLM failed/timeout -> paste the corrected raw transcript, show Failed.
                paste(capitalizeFirst(corrected))
                save(wav, createdAt, durationMs, corrected, status = "llm_failed",
                    version = NewVersion(profile.fileName, profile.name, capitalizeFirst(corrected), null, null))
                finishFailed("LLM timeout · raw text pasted")
            }
        } else {
            // §14: user wanted post-processing but the LLM model is missing -> one-time notice.
            if (settings.postProcessing && profile != null && !llmMissingNotified) {
                llmMissingNotified = true
                appState.toast("LLM model missing — pasting Whisper text. Add it in Models.")
            }
            paste(capitalizeFirst(corrected))
            save(wav, createdAt, durationMs, corrected, status = "ok",
                version = NewVersion(null, "Raw (no LLM)", capitalizeFirst(corrected), null, null))
            finishPasted()
        }
    }

    /** Write the WAV (unless keepAudio is off) and persist the dictation to History. */
    private fun save(
        wav: ByteArray,
        createdAt: Long,
        durationMs: Long,
        raw: String,
        status: String,
        version: NewVersion,
    ) {
        val audioPath = if (appState.settings.value.keepAudio != "none") {
            audioDir.createDirectories()
            val file = audioDir.resolve("$createdAt.wav")
            Files.write(file, wav)
            file.toString()
        } else {
            null
        }
        appState.saveDictation(createdAt, durationMs, raw, audioPath, status, version)
    }

    private suspend fun finishPasted() {
        _pill.value = buildPill(PillPhase.Pasted)
        delay(1200)
        endProcessing()
    }

    private suspend fun finishFailed(message: String) {
        _pill.value = buildPill(PillPhase.Failed).copy(failedMessage = message)
        _toast.value = message
        delay(5000)
        endProcessing()
    }

    private fun finishToast(message: String) {
        _toast.value = message
        endProcessing()
    }

    private fun endProcessing() {
        hide()
        sm.onEvent(HotkeyInput.ProcessingDone, System.currentTimeMillis())
    }

    private fun discard(toast: String?) {
        stopTicker()
        recorder.cancel()
        hide()
        if (toast != null) _toast.value = toast
    }

    private fun activeProfile(): Profile? {
        val file = appState.settings.value.quickSlots[appState.activeSlot.value.toString()] ?: return null
        return appState.profiles.value.firstOrNull { it.fileName == file }
    }

    private fun scheduleDoubleTapTimeout() {
        val window = appState.settings.value.doubleTapMs.toLong()
        scope.launch {
            delay(window)
            onInput(HotkeyInput.DoubleTapTimeout)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val phase = _pill.value?.phase ?: break
                if (phase == PillPhase.Hold || phase == PillPhase.HandsFree) {
                    // §14: cap recording at 10 minutes, then auto-stop and process.
                    if (recordingStartMs > 0 && System.currentTimeMillis() - recordingStartMs >= MAX_RECORDING_MS) {
                        appState.toast("Stopped at 10 minutes")
                        onInput(if (phase == PillPhase.HandsFree) HotkeyInput.KeyDown else HotkeyInput.KeyUp)
                        break
                    }
                    _pill.value = buildPill(phase)
                }
                delay(100)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun hide() {
        _pill.value = null
    }

    private fun buildPill(phase: PillPhase): PillUiState {
        val settings = appState.settings.value
        val active = appState.activeSlot.value
        val slots = (1..5).map { slot ->
            slotKeyVisual(slot, active, settings.quickSlots[slot.toString()] != null)
        }
        return PillUiState(
            phase = phase,
            elapsedMs = if (recordingStartMs > 0) System.currentTimeMillis() - recordingStartMs else 0,
            levels = levels,
            slots = slots,
            profileShort = shortProfileName(settings.quickSlots[active.toString()]),
            hotkeyLabel = hotkeyLabel,
            pasteShortcut = pasteShortcut,
        )
    }

    private fun toPhase(mode: RecMode) = if (mode == RecMode.HandsFree) PillPhase.HandsFree else PillPhase.Hold

    companion object {
        private val RECORDING_STATES = setOf(
            HotkeyStateMachine.State.HoldDown,
            HotkeyStateMachine.State.TapWait,
            HotkeyStateMachine.State.HandsFree,
        )

        private const val MAX_RECORDING_MS = 10 * 60 * 1000L

        private fun freePort(): Int = ServerSocket(0).use { it.localPort }
    }
}
