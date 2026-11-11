package com.bolobhai.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WavTest {

    private fun le16(b: ByteArray, off: Int) =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)

    private fun le32(b: ByteArray, off: Int) =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or ((b[off + 3].toInt() and 0xFF) shl 24)

    private fun ascii(b: ByteArray, off: Int, len: Int) =
        String(b.copyOfRange(off, off + len), Charsets.US_ASCII)

    @Test
    fun wavHeader_matchesSpec() {
        val pcm = ByteArray(100)
        val wav = pcm16ToWav(pcm, sampleRate = 16000, channels = 1)

        assertEquals(44 + 100, wav.size)
        assertEquals("RIFF", ascii(wav, 0, 4))
        assertEquals(36 + 100, le32(wav, 4))
        assertEquals("WAVE", ascii(wav, 8, 4))
        assertEquals("fmt ", ascii(wav, 12, 4))
        assertEquals(16, le32(wav, 16))      // subchunk1 size
        assertEquals(1, le16(wav, 20))       // PCM
        assertEquals(1, le16(wav, 22))       // mono
        assertEquals(16000, le32(wav, 24))   // sample rate
        assertEquals(16000 * 1 * 2, le32(wav, 28)) // byte rate
        assertEquals(2, le16(wav, 32))       // block align
        assertEquals(16, le16(wav, 34))      // bits per sample
        assertEquals("data", ascii(wav, 36, 4))
        assertEquals(100, le32(wav, 40))     // data size
    }

    @Test
    fun waveformBars_count_and_normalization() {
        // Louder first half, quieter second half.
        val pcm = ByteArray(48 * 100 * 2)
        for (i in 0 until pcm.size / 2) {
            val loud = i < pcm.size / 4
            val v = if (loud) 0x4000 else 0x0400
            pcm[i * 2] = (v and 0xFF).toByte()
            pcm[i * 2 + 1] = (v shr 8).toByte()
        }
        val bars = waveformBars(pcm, 48)
        assertEquals(48, bars.size)
        assertTrue(bars.all { it in 0f..1f })
        assertTrue(bars.max() in 0.99f..1.01f)      // normalized to the loudest bucket
        assertTrue(bars.first() > bars.last())       // loud half louder than quiet half
    }

    @Test
    fun waveformBars_silence_isAllZero() {
        assertTrue(waveformBars(ByteArray(48 * 4), 48).all { it == 0f })
    }

    @Test
    fun wavData_stripsHeader() {
        val wav = pcm16ToWav(ByteArray(20) { 7 })
        assertEquals(20, wavData(wav).size)
        assertEquals(7.toByte(), wavData(wav)[0])
    }

    @Test
    fun rms_silence_isZero() {
        assertEquals(0.0, rms16(ByteArray(64)), 1e-9)
    }

    @Test
    fun rms_fullScale_isApproxOne() {
        // All samples = -32768 (0x8000 little-endian) -> max magnitude.
        val pcm = ByteArray(64)
        for (i in pcm.indices step 2) {
            pcm[i] = 0x00
            pcm[i + 1] = 0x80.toByte()
        }
        assertTrue(rms16(pcm) in 0.99..1.01)
    }
}
