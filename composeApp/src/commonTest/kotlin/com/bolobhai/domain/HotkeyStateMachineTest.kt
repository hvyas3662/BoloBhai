package com.bolobhai.domain

import com.bolobhai.domain.HotkeyEffect.Discard
import com.bolobhai.domain.HotkeyEffect.SetPillMode
import com.bolobhai.domain.HotkeyEffect.ShowPill
import com.bolobhai.domain.HotkeyEffect.StartDoubleTapTimer
import com.bolobhai.domain.HotkeyEffect.StartRecording
import com.bolobhai.domain.HotkeyEffect.StopAndProcess
import com.bolobhai.domain.HotkeyEffect.SwitchSlot
import com.bolobhai.domain.HotkeyInput.Digit
import com.bolobhai.domain.HotkeyInput.DoubleTapTimeout
import com.bolobhai.domain.HotkeyInput.Esc
import com.bolobhai.domain.HotkeyInput.KeyDown
import com.bolobhai.domain.HotkeyInput.KeyUp
import com.bolobhai.domain.HotkeyInput.ProcessingDone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class HotkeyStateMachineTest {

    private fun sm(assigned: Set<Int> = setOf(1, 2, 3, 4)) =
        HotkeyStateMachine(slotAssigned = { it in assigned })

    // --- Hold ---

    @Test
    fun keyDown_startsHoldRecording() {
        val m = sm()
        val r = m.onEvent(KeyDown, nowMs = 0)
        assertEquals(listOf(StartRecording(RecMode.Hold), ShowPill(RecMode.Hold)), r.effects)
        assertEquals(HotkeyStateMachine.State.HoldDown, m.state)
    }

    @Test
    fun hold_thenReleaseAfterMin_processes() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        val r = m.onEvent(KeyUp, 600) // >= TAP_MAX and >= MIN_RECORDING
        assertEquals(listOf(StopAndProcess), r.effects)
        assertEquals(HotkeyStateMachine.State.Processing, m.state)
    }

    @Test
    fun hold_releaseBetweenTapMaxAndMin_isTooShort() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        val r = m.onEvent(KeyUp, 300) // > TAP_MAX (280) but < MIN (500)
        assertEquals(listOf(Discard("Too short")), r.effects)
        assertEquals(HotkeyStateMachine.State.Idle, m.state)
    }

    // --- Tap / double-tap ---

    @Test
    fun shortTap_entersTapWait() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        val r = m.onEvent(KeyUp, 100) // < TAP_MAX
        assertEquals(listOf(StartDoubleTapTimer), r.effects)
        assertEquals(HotkeyStateMachine.State.TapWait, m.state)
    }

    @Test
    fun tapWait_timeout_discardsWithHint() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        m.onEvent(KeyUp, 100)
        val r = m.onEvent(DoubleTapTimeout, 500)
        assertEquals(1, r.effects.size)
        assertTrue((r.effects[0] as Discard).toast!!.startsWith("Too short · hold"))
        assertEquals(HotkeyStateMachine.State.Idle, m.state)
    }

    @Test
    fun doubleTap_entersHandsFree_thenKeyUpSwallowed() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        m.onEvent(KeyUp, 100)
        val down2 = m.onEvent(KeyDown, 200)
        assertEquals(listOf(SetPillMode(RecMode.HandsFree)), down2.effects)
        assertEquals(HotkeyStateMachine.State.HandsFree, m.state)
        val up2 = m.onEvent(KeyUp, 260)
        assertTrue(up2.swallow)
        assertEquals(HotkeyStateMachine.State.HandsFree, m.state)
    }

    @Test
    fun handsFree_pressAgain_stopsAndProcesses() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        m.onEvent(KeyUp, 100)
        m.onEvent(KeyDown, 200)
        m.onEvent(KeyUp, 260)
        val stop = m.onEvent(KeyDown, 900) // total from first keydown >= MIN
        assertEquals(listOf(StopAndProcess), stop.effects)
        assertEquals(HotkeyStateMachine.State.Processing, m.state)
    }

    // --- Digits while recording ---

    @Test
    fun digit_assignedSlot_switches_andSwallows() {
        val m = sm(assigned = setOf(3))
        m.onEvent(KeyDown, 0)
        val r = m.onEvent(Digit(3), 50)
        assertEquals(listOf(SwitchSlot(3)), r.effects)
        assertTrue(r.swallow)
    }

    @Test
    fun digit_emptySlot_swallows_butNoSwitch() {
        val m = sm(assigned = setOf(1))
        m.onEvent(KeyDown, 0)
        val r = m.onEvent(Digit(5), 50)
        assertTrue(r.effects.isEmpty())
        assertTrue(r.swallow)
    }

    @Test
    fun digit_whenIdle_notSwallowed() {
        val m = sm()
        val r = m.onEvent(Digit(2), 0)
        assertFalse(r.swallow)
        assertTrue(r.effects.isEmpty())
    }

    // --- Esc ---

    @Test
    fun esc_whileRecording_cancels_andSwallows() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        val r = m.onEvent(Esc, 50)
        assertEquals(listOf(Discard("Recording cancelled")), r.effects)
        assertTrue(r.swallow)
        assertEquals(HotkeyStateMachine.State.Idle, m.state)
    }

    // --- Processing ---

    @Test
    fun processing_ignoresInput_untilDone() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        m.onEvent(KeyUp, 600) // -> Processing
        val ignored = m.onEvent(KeyDown, 700)
        assertTrue(ignored.effects.isEmpty())
        assertEquals(HotkeyStateMachine.State.Processing, m.state)
        m.onEvent(ProcessingDone, 800)
        assertEquals(HotkeyStateMachine.State.Idle, m.state)
    }

    // --- Hands-free keeps audio from the first tap ---

    @Test
    fun handsFree_durationMeasuredFromFirstKeyDown() {
        val m = sm()
        m.onEvent(KeyDown, 0)
        m.onEvent(KeyUp, 100)
        m.onEvent(KeyDown, 200)
        m.onEvent(KeyUp, 260)
        // Stop only 300ms after the LAST keydown but 600ms after the FIRST -> not too short.
        val stop = m.onEvent(KeyDown, 600)
        assertEquals(listOf(StopAndProcess), stop.effects)
    }
}
