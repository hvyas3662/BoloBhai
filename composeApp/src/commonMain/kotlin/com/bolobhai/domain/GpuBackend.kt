package com.bolobhai.domain

/**
 * Picks the inference backend from the GPU adapter names reported by the OS (BoloBhai.md §16/§17).
 * One Vulkan build covers both AMD and Intel; NVIDIA gets CUDA (fastest); no/unknown GPU -> CPU.
 * Mac is always Metal and bypasses this (handled in platform code).
 */
fun gpuBackendFor(adapterNames: List<String>): String {
    val names = adapterNames.joinToString(" ").lowercase()
    return when {
        "nvidia" in names || "geforce" in names || "rtx" in names || "quadro" in names -> "cuda"
        "amd" in names || "radeon" in names || "intel" in names || "arc" in names -> "vulkan"
        else -> "cpu"
    }
}
