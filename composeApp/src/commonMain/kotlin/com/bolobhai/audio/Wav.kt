package com.bolobhai.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Wraps raw 16-bit PCM in a canonical 44-byte WAV header (BoloBhai.md §2: 16 kHz mono). */
fun pcm16ToWav(pcm: ByteArray, sampleRate: Int = 16000, channels: Int = 1): ByteArray {
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8

    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + pcm.size)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1) // PCM
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(bitsPerSample.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcm.size)

    return ByteArrayOutputStream(44 + pcm.size).apply {
        write(header.array())
        write(pcm)
    }.toByteArray()
}

/**
 * PCM data chunk of a canonical WAV produced by [pcm16ToWav].
 * shortcut: assumes the fixed 44-byte header; replace with real chunk parsing if we ever read
 * foreign WAV files.
 */
fun wavData(wav: ByteArray): ByteArray =
    if (wav.size <= 44) ByteArray(0) else wav.copyOfRange(44, wav.size)

/**
 * [bars] amplitude bars (0.0..1.0) for a waveform strip (BoloBhai.md §13.1): split the PCM16 into
 * equal buckets, take each bucket's RMS, normalize by the loudest bucket.
 */
fun waveformBars(pcm: ByteArray, bars: Int = 48): List<Float> {
    val samples = pcm.size / 2
    if (samples == 0 || bars <= 0) return List(bars.coerceAtLeast(0)) { 0f }
    val perBar = (samples / bars).coerceAtLeast(1)
    val raw = (0 until bars).map { b ->
        val start = (b * perBar * 2).coerceAtMost(pcm.size)
        val end = ((b + 1) * perBar * 2).coerceAtMost(pcm.size)
        if (start >= end) 0.0 else rms16(pcm.copyOfRange(start, end))
    }
    val max = raw.maxOrNull() ?: 0.0
    return if (max <= 0.0) List(bars) { 0f } else raw.map { (it / max).toFloat() }
}

/** RMS level of little-endian PCM16 samples, normalized to 0.0..1.0 (for the live waveform). */
fun rms16(pcm: ByteArray): Double {
    if (pcm.size < 2) return 0.0
    var sumSquares = 0.0
    var count = 0
    var i = 0
    while (i + 1 < pcm.size) {
        val sample = ((pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)).toShort()
        val norm = sample / 32768.0
        sumSquares += norm * norm
        count++
        i += 2
    }
    if (count == 0) return 0.0
    return kotlin.math.sqrt(sumSquares / count)
}
