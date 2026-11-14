package com.bolobhai.platform

import com.bolobhai.domain.gpuBackendFor
import com.bolobhai.log.Logger

/**
 * Detects the best inference backend for this machine (BoloBhai.md §16/§17). Mac is always Metal;
 * on Windows we read the GPU adapter names via WMI and map them (NVIDIA->cuda, AMD/Intel->vulkan,
 * none->cpu). Any failure degrades to "cpu" — never blocks startup.
 */
object GpuDetect {
    private val os = System.getProperty("os.name").lowercase()

    fun backend(): String = try {
        when {
            os.contains("mac") || os.contains("darwin") -> "metal"
            os.contains("win") -> gpuBackendFor(windowsAdapterNames())
            else -> "cpu"
        }
    } catch (e: Exception) {
        Logger.warn("GPU detection failed, using CPU: ${e.message}")
        "cpu"
    }

    private fun windowsAdapterNames(): List<String> {
        val proc = ProcessBuilder(
            "powershell", "-NoProfile", "-Command",
            "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name",
        ).redirectErrorStream(true).start()
        val names = proc.inputStream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
        proc.waitFor()
        Logger.info("GPU adapters: ${names.joinToString(", ")}")
        return names
    }
}
