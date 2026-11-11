package com.bolobhai.audio

import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Captures 16 kHz mono 16-bit PCM (BoloBhai.md §2) on a background thread, streaming live RMS
 * levels for the waveform. [stop] returns the accumulated raw PCM. JVM/portable (mac + windows).
 */
class AudioRecorder {
    private var line: TargetDataLine? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private val buffer = ByteArrayOutputStream()

    val isRecording: Boolean get() = running

    fun start(deviceName: String? = null, onLevel: (Float) -> Unit) {
        val format = AudioFormat(16_000f, 16, 1, true, false) // signed, little-endian
        val dataLine = openLine(format, deviceName)
        dataLine.open(format)
        dataLine.start()
        line = dataLine
        buffer.reset()
        running = true
        thread = Thread {
            val chunk = ByteArray(3200) // ~100 ms at 16 kHz mono 16-bit
            while (running) {
                val n = dataLine.read(chunk, 0, chunk.size)
                if (n > 0) {
                    buffer.write(chunk, 0, n)
                    onLevel(rms16(chunk.copyOf(n)).toFloat())
                }
            }
        }.apply {
            name = "bolobhai-recorder"
            isDaemon = true
            start()
        }
    }

    /** Stops capture and returns the recorded PCM bytes. */
    fun stop(): ByteArray {
        teardown()
        return buffer.toByteArray()
    }

    /** Stops capture and discards the audio. */
    fun cancel() {
        teardown()
        buffer.reset()
    }

    companion object {
        /** System input devices that can capture (default device first via [AudioSystem]). */
        fun inputDevices(): List<String> {
            val format = AudioFormat(16_000f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            return AudioSystem.getMixerInfo().filter { mixerInfo ->
                runCatching { AudioSystem.getMixer(mixerInfo).isLineSupported(info) }.getOrDefault(false)
            }.map { it.name }.distinct()
        }
    }

    /** Named mixer's capture line if given and available, else the system default. */
    private fun openLine(format: AudioFormat, deviceName: String?): TargetDataLine {
        if (deviceName != null) {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            AudioSystem.getMixerInfo().firstOrNull { it.name == deviceName }?.let { mi ->
                runCatching { AudioSystem.getMixer(mi).getLine(info) as TargetDataLine }.getOrNull()?.let { return it }
            }
        }
        return AudioSystem.getTargetDataLine(format)
    }

    private fun teardown() {
        running = false
        thread?.join(500)
        thread = null
        line?.apply {
            stop()
            close()
        }
        line = null
    }
}
