package com.bolobhai.app

import com.bolobhai.audio.AudioRecorder
import com.bolobhai.audio.pcm16ToWav
import com.bolobhai.domain.HotkeyEffect
import com.bolobhai.domain.HotkeyInput
import com.bolobhai.domain.HotkeyStateMachine
import com.bolobhai.domain.RecMode
import com.bolobhai.domain.capitalizeFirst
import com.bolobhai.domain.shortProfileName
import com.bolobhai.sidecar.ProcessSupervisor
import com.bolobhai.sidecar.SidecarStatus
import com.bolobhai.sidecar.WhisperClient
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ServerSocket

/**
 * Ties the hotkey hook to the gesture state machine, recorder, pill and the whisper sidecar, then
 * pastes the result (BoloBhai.md §9). No LLM in this milestone: the corrected raw transcript is
 * pasted. When the whisper sidecar is not installed/ready the pipeline degrades gracefully.
 */
class HotkeyController(
    private val appState: AppState,
    private val hotkeyLabel: String,
    private val pasteShortcut: String,
    private val launchWhisper: (port: Int) -> Process?,
    private val paste: (String) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sm = HotkeyStateMachine(slotAssigned = { slot ->
        appState.settings.value.quickSlots[slot.toString()] != null
    })
    private val recorder = AudioRecorder()

    private val whisperPort = freePort()
    private val whisper = WhisperClient("http://127.0.0.1:$whisperPort")
    private val supervisor = ProcessSupervisor(
        launch = { launchWhisper(whisperPort) },
        healthCheck = { whisper.health() },
    )
    val sidecarStatus: StateFlow<SidecarStatus> get() = supervisor.status

    private val _pill = MutableStateFlow<PillUiState?>(null)
    val pill: StateFlow<PillUiState?> = _pill.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    @Volatile private var levels = List(14) { 0f }
    @Volatile private var recordingStartMs = 0L
    private var tickerJob: Job? = null

    fun startSidecars() {
        scope.launch { supervisor.start() }
    }

    fun isRecording(): Boolean = sm.state in RECORDING_STATES

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
        recorder.start { level -> levels = (levels.drop(1) + level) }
        _pill.value = buildPill(toPhase(mode))
        startTicker()
    }

    private fun runPipeline() {
        stopTicker()
        _pill.value = buildPill(PillPhase.Processing)
        scope.launch {
            val pcm = recorder.stop()
            if (supervisor.status.value != SidecarStatus.Ready) {
                fail("Speech engine not installed")
                return@launch
            }
            val wav = pcm16ToWav(pcm)
            val settings = appState.settings.value
            val transcript = whisper.transcribe(wav, settings.language, prompt = "")
            if (transcript.isBlank()) {
                fail("Couldn't transcribe")
                return@launch
            }
            // M2/M6: no LLM yet — paste the corrected raw transcript.
            paste(capitalizeFirst(transcript))
            _pill.value = buildPill(PillPhase.Pasted)
            delay(1200)
            hide()
            sm.onEvent(HotkeyInput.ProcessingDone, System.currentTimeMillis())
        }
    }

    private fun fail(message: String) {
        _pill.value = buildPill(PillPhase.Failed).copy(failedMessage = message)
        _toast.value = message
        scope.launch {
            delay(5000)
            hide()
            sm.onEvent(HotkeyInput.ProcessingDone, System.currentTimeMillis())
        }
    }

    private fun discard(toast: String?) {
        stopTicker()
        recorder.cancel()
        hide()
        if (toast != null) _toast.value = toast
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

        private fun freePort(): Int = ServerSocket(0).use { it.localPort }
    }
}
