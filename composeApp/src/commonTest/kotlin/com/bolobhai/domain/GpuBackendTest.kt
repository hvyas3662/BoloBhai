package com.bolobhai.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GpuBackendTest {

    @Test fun nvidia_isCuda() {
        assertEquals("cuda", gpuBackendFor(listOf("NVIDIA GeForce RTX 4070")))
        assertEquals("cuda", gpuBackendFor(listOf("Quadro P2000")))
    }

    @Test fun amd_and_intel_areVulkan() {
        assertEquals("vulkan", gpuBackendFor(listOf("AMD Radeon RX 6600")))
        assertEquals("vulkan", gpuBackendFor(listOf("Intel(R) UHD Graphics 630")))
        assertEquals("vulkan", gpuBackendFor(listOf("Intel Arc A770")))
    }

    @Test fun noOrUnknownGpu_isCpu() {
        assertEquals("cpu", gpuBackendFor(emptyList()))
        assertEquals("cpu", gpuBackendFor(listOf("Microsoft Basic Display Adapter")))
    }

    @Test fun nvidia_winsOverIntel_whenBothPresent() {
        // Laptops with an Intel iGPU + NVIDIA dGPU: prefer the discrete NVIDIA (CUDA).
        assertEquals("cuda", gpuBackendFor(listOf("Intel UHD Graphics", "NVIDIA GeForce RTX 3060")))
    }
}
