package com.bolobhai.domain

enum class RecMode { Hold, HandsFree }

/** Inputs to the hotkey state machine. [nowMs] is supplied per event by the caller's clock. */
sealed interface HotkeyInput {
    data object KeyDown : HotkeyInput
    data object KeyUp : HotkeyInput
    data class Digit(val n: Int) : HotkeyInput
    data object Esc : HotkeyInput
    data object DoubleTapTimeout : HotkeyInput
    data object ProcessingDone : HotkeyInput
}

/** Side effects the caller must carry out (recording, pill, pipeline, toast). */
sealed interface HotkeyEffect {
    data class StartRecording(val mode: RecMode) : HotkeyEffect
    data class ShowPill(val mode: RecMode) : HotkeyEffect
    data class SetPillMode(val mode: RecMode) : HotkeyEffect
    data object StartDoubleTapTimer : HotkeyEffect
    data class SwitchSlot(val n: Int) : HotkeyEffect
    data object StopAndProcess : HotkeyEffect
    /** Discard audio + hide pill; show [toast] if non-null. */
    data class Discard(val toast: String?) : HotkeyEffect
}

data class HotkeyResult(val effects: List<HotkeyEffect>, val swallow: Boolean)

/**
 * Pure hotkey gesture state machine (BoloBhai.md §6.3). Platform-agnostic: the Mac helper and the
 * Windows LL hook both feed it [HotkeyInput]s and execute the returned [HotkeyEffect]s.
 */
class HotkeyStateMachine(
    private val slotAssigned: (Int) -> Boolean = { false },
) {
    enum class State { Idle, HoldDown, TapWait, HandsFree, Processing }

    var state: State = State.Idle
        private set

    private var recordingStartMs = 0L
    private var swallowNextKeyUp = false

    companion object {
        const val TAP_MAX_MS = 280L
        const val MIN_RECORDING_MS = 500L
    }

    fun onEvent(input: HotkeyInput, nowMs: Long): HotkeyResult = when (state) {
        State.Idle -> onIdle(input, nowMs)
        State.HoldDown -> onHoldDown(input, nowMs)
        State.TapWait -> onTapWait(input, nowMs)
        State.HandsFree -> onHandsFree(input, nowMs)
        State.Processing -> onProcessing(input)
    }

    private fun onIdle(input: HotkeyInput, nowMs: Long): HotkeyResult = when (input) {
        is HotkeyInput.KeyDown -> {
            recordingStartMs = nowMs
            state = State.HoldDown
            none(HotkeyEffect.StartRecording(RecMode.Hold), HotkeyEffect.ShowPill(RecMode.Hold))
        }
        // Digits and Esc are not intercepted when not recording.
        else -> ignore()
    }

    private fun onHoldDown(input: HotkeyInput, nowMs: Long): HotkeyResult = when (input) {
        is HotkeyInput.KeyUp ->
            if (nowMs - recordingStartMs >= TAP_MAX_MS) {
                stop(nowMs)
            } else {
                state = State.TapWait
                none(HotkeyEffect.StartDoubleTapTimer)
            }
        is HotkeyInput.Digit -> digit(input.n)
        is HotkeyInput.Esc -> cancel()
        else -> ignore() // key-repeat KeyDown etc.
    }

    private fun onTapWait(input: HotkeyInput, nowMs: Long): HotkeyResult = when (input) {
        is HotkeyInput.KeyDown -> {
            swallowNextKeyUp = true
            state = State.HandsFree
            none(HotkeyEffect.SetPillMode(RecMode.HandsFree))
        }
        is HotkeyInput.DoubleTapTimeout -> {
            state = State.Idle
            none(HotkeyEffect.Discard("Too short · hold to talk, or double-tap for hands-free"))
        }
        is HotkeyInput.Digit -> digit(input.n)
        is HotkeyInput.Esc -> cancel()
        else -> ignore()
    }

    private fun onHandsFree(input: HotkeyInput, nowMs: Long): HotkeyResult = when (input) {
        is HotkeyInput.KeyUp ->
            if (swallowNextKeyUp) {
                swallowNextKeyUp = false
                HotkeyResult(emptyList(), swallow = true)
            } else {
                ignore()
            }
        is HotkeyInput.KeyDown -> {
            swallowNextKeyUp = true
            stop(nowMs)
        }
        is HotkeyInput.Digit -> digit(input.n)
        is HotkeyInput.Esc -> cancel()
        else -> ignore()
    }

    private fun onProcessing(input: HotkeyInput): HotkeyResult = when (input) {
        is HotkeyInput.ProcessingDone -> {
            state = State.Idle
            ignore()
        }
        else -> ignore()
    }

    // --- helpers ---

    private fun stop(nowMs: Long): HotkeyResult {
        val duration = nowMs - recordingStartMs
        return if (duration < MIN_RECORDING_MS) {
            state = State.Idle
            none(HotkeyEffect.Discard("Too short"))
        } else {
            state = State.Processing
            none(HotkeyEffect.StopAndProcess)
        }
    }

    private fun cancel(): HotkeyResult {
        state = State.Idle
        return HotkeyResult(listOf(HotkeyEffect.Discard("Recording cancelled")), swallow = true)
    }

    private fun digit(n: Int): HotkeyResult {
        val effects = if (slotAssigned(n)) listOf(HotkeyEffect.SwitchSlot(n)) else emptyList()
        return HotkeyResult(effects, swallow = true)
    }

    private fun none(vararg effects: HotkeyEffect) = HotkeyResult(effects.toList(), swallow = false)

    private fun ignore() = HotkeyResult(emptyList(), swallow = false)
}
