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
