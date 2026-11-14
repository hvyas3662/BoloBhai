package com.bolobhai.platform

import com.bolobhai.domain.SystemHardware
import com.bolobhai.log.Logger
import java.lang.management.ManagementFactory

/**
 * Detects inference-relevant hardware (#8): CPU name, total RAM, GPU name and VRAM. Best-effort and
 * never throws — any probe that fails leaves its field null/empty so the Models page simply shows
 * less, never breaks. VRAM is only trusted for NVIDIA (nvidia-smi); other vendors report null.
 */
object HardwareDetect {
    private val os = System.getProperty("os.name").lowercase()
    private val isWindows = os.contains("win")
    private val isMac = os.contains("mac") || os.contains("darwin")

    fun detect(): SystemHardware {
        val backend = GpuDetect.backend()
        val gpuName = gpuName()
        return SystemHardware(
            cpuName = cpuName(),
            ramBytes = ramBytes(),
            gpuName = gpuName,
            vramBytes = vramBytes(gpuName),
            backend = backend,
        )
    }

    private fun ramBytes(): Long = try {
        val bean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
        bean.totalMemorySize
    } catch (e: Exception) {
        Logger.warn("RAM detection failed: ${e.message}"); 0L
    }

    private fun cpuName(): String = try {
        when {
            isWindows -> wmi("Win32_Processor", "Name").firstOrNull()
            else -> null
        } ?: (System.getProperty("os.arch") ?: "Unknown CPU")
    } catch (e: Exception) {
        Logger.warn("CPU detection failed: ${e.message}"); System.getProperty("os.arch") ?: "Unknown CPU"
    }

    private fun gpuName(): String? = try {
        when {
            isMac -> "Apple GPU"
            isWindows -> wmi("Win32_VideoController", "Name").firstOrNull()
            else -> null
        }
    } catch (e: Exception) {
        Logger.warn("GPU name detection failed: ${e.message}"); null
    }

    /** VRAM in bytes. Only NVIDIA (via nvidia-smi) is trusted; WMI AdapterRAM caps at 4 GB, so skip it. */
    private fun vramBytes(gpuName: String?): Long? = try {
        val name = (gpuName ?: "").lowercase()
        if ("nvidia" in name || "geforce" in name || "rtx" in name || "quadro" in name) {
            val out = exec(listOf("nvidia-smi", "--query-gpu=memory.total", "--format=csv,noheader,nounits"))
            out.firstOrNull()?.trim()?.toLongOrNull()?.let { it * 1_000_000L } // reported in MiB-ish MB
        } else null
    } catch (e: Exception) {
        Logger.warn("VRAM detection failed: ${e.message}"); null
    }

    private fun wmi(className: String, property: String): List<String> =
        exec(listOf("powershell", "-NoProfile", "-Command", "Get-CimInstance $className | Select-Object -ExpandProperty $property"))

    private fun exec(command: List<String>): List<String> {
        val proc = ProcessBuilder(command).redirectErrorStream(true).start()
        val lines = proc.inputStream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
        proc.waitFor()
        return lines
    }
}
