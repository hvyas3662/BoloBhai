package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HardwareTest {
    @Test
    fun parsesTildeAndRangeAndMb() {
        assertEquals(2_000_000_000L, parseRamRequirement("~2 GB"))
        assertEquals(4_000_000_000L, parseRamRequirement("3–4 GB")) // en-dash -> upper bound
        assertEquals(4_000_000_000L, parseRamRequirement("3-4 GB"))
        assertEquals(550_000_000L, parseRamRequirement("550 MB"))
        assertNull(parseRamRequirement("unknown"))
    }

    @Test
    fun combinedFootprintExceedsButSwapRelaxes() {
        val vram = 4_000_000_000L
        val llm = parseRamRequirement("3–4 GB")   // 4 GB
        val stt = parseRamRequirement("~2 GB")     // 2 GB
        // Normal mode: both resident (6 GB) > 4 GB VRAM -> exceeds.
        assertTrue(capacityExceeded(llm, stt, swap = false, limitBytes = vram))
        // Swap mode: only the LLM resident (4 GB) == 4 GB -> fits.
        assertFalse(capacityExceeded(llm, stt, swap = true, limitBytes = vram))
    }

    @Test
    fun unknownRequirementOrLimitNeverFlags() {
        assertFalse(capacityExceeded(null, 1L, swap = false, limitBytes = 1L))
        assertFalse(capacityExceeded(5_000_000_000L, null, swap = false, limitBytes = null))
    }

    @Test
    fun limitPicksVramOnlyWhenKnownDiscreteBackend() {
        val cuda = SystemHardware("cpu", 16_000_000_000L, "RTX", 8_000_000_000L, "cuda")
        assertTrue(cuda.usesVram)
        assertEquals(8_000_000_000L, cuda.limitBytes)

        val cudaNoVram = cuda.copy(vramBytes = null)
        assertFalse(cudaNoVram.usesVram)
        assertEquals(16_000_000_000L, cudaNoVram.limitBytes) // falls back to RAM

        val cpu = SystemHardware("cpu", 16_000_000_000L, null, null, "cpu")
        assertFalse(cpu.usesVram)
        assertEquals(16_000_000_000L, cpu.limitBytes)
    }
}
