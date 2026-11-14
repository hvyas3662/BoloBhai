package com.bolobhai.domain

import java.util.Locale

/** A snapshot of this machine's inference-relevant hardware (#8). Detected once at startup. */
data class SystemHardware(
    val cpuName: String,
    val ramBytes: Long,
    val gpuName: String?,
    val vramBytes: Long?,   // null = couldn't determine (don't warn on VRAM)
    val backend: String,    // cuda | vulkan | metal | cpu
) {
    /** Models run on the GPU's own memory only on discrete-GPU backends with a known VRAM figure. */
    val usesVram: Boolean get() = (backend == "cuda" || backend == "vulkan") && vramBytes != null

    /** The memory pool a model competes for: VRAM on discrete GPUs, otherwise system RAM. */
    val limitBytes: Long? get() = if (usesVram) vramBytes else ramBytes.takeIf { it > 0 }
}

/**
 * Parse a catalog `ram` string to an upper-bound byte requirement. Handles `~2 GB`, `3–4 GB`
 * (en-dash or hyphen → upper bound), `550 MB`. Returns null if no number/unit is found.
 */
fun parseRamRequirement(ram: String): Long? {
    val unitMatch = Regex("(gb|mb)", RegexOption.IGNORE_CASE).find(ram) ?: return null
    val unitBytes = if (unitMatch.value.equals("gb", ignoreCase = true)) 1_000_000_000L else 1_000_000L
    val numbers = Regex("\\d+(?:\\.\\d+)?").findAll(ram).map { it.value.toDouble() }.toList()
    val upper = numbers.maxOrNull() ?: return null
    return (upper * unitBytes).toLong()
}

/**
 * Whether [modelRamBytes] (plus the other-kind active model, when both stay resident) exceeds the
 * machine's memory [limitBytes]. In Low-VRAM swap mode the two models load one at a time, so each is
 * compared alone (#8 decision). Unknown requirement or limit → not flagged.
 */
fun capacityExceeded(
    modelRamBytes: Long?,
    otherActiveRamBytes: Long?,
    swap: Boolean,
    limitBytes: Long?,
): Boolean {
    if (modelRamBytes == null || limitBytes == null) return false
    val required = if (swap) modelRamBytes else modelRamBytes + (otherActiveRamBytes ?: 0L)
    return required > limitBytes
}
