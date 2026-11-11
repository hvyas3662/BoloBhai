package com.bolobhai.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin

/** Short start/stop cues (BoloBhai.md §13.6 "Start / stop sound"). Best-effort; silent if no line. */
object Sounds {
    private const val SAMPLE_RATE = 44_100

    fun start() = play(tone(880.0, 110))
    fun stop() = play(tone(440.0, 110))

    /** A mono 16-bit PCM sine with 5 ms linear fades so it doesn't click. */
    fun tone(freqHz: Double, ms: Int, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val samples = sampleRate * ms / 1000
        val fade = (sampleRate * 5 / 1000).coerceAtLeast(1)
        val out = ByteArray(samples * 2)
        for (i in 0 until samples) {
            val env = minOf(1.0, i.toDouble() / fade, (samples - i).toDouble() / fade)
            val v = (sin(2 * PI * freqHz * i / sampleRate) * env * 0.3 * Short.MAX_VALUE).toInt().toShort()
            out[i * 2] = (v.toInt() and 0xFF).toByte()
            out[i * 2 + 1] = (v.toInt() shr 8).toByte()
        }
        return out
    }

    private fun play(pcm: ByteArray) {
        Thread {
            runCatching {
                val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
                AudioSystem.getSourceDataLine(format).apply {
                    open(format); start()
                    write(pcm, 0, pcm.size)
                    drain(); close()
                }
            }
        }.apply { isDaemon = true; start() }
    }
}
