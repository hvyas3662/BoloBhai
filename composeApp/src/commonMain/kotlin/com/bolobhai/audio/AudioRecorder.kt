package com.bolobhai.audio

import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
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

    fun start(onLevel: (Float) -> Unit) {
        val format = AudioFormat(16_000f, 16, 1, true, false) // signed, little-endian
        val dataLine = AudioSystem.getTargetDataLine(format)
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
