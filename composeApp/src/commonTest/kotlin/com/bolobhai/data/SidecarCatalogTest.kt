package com.bolobhai.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SidecarCatalogTest {

    private val sample = """
        { "sidecars": [
          { "engine": "whisper", "backend": "vulkan", "os": "windows", "url": "u1", "sizeBytes": 10 },
          { "engine": "llama",   "backend": "vulkan", "os": "windows", "url": "u2", "sizeBytes": 20, "sha256": "ab" },
          { "engine": "whisper", "backend": "cpu",    "os": "windows", "url": "u3", "sizeBytes": 30 }
        ] }
    """.trimIndent()

    @Test fun parse_readsEntries() {
        val c = SidecarCatalog.parse(sample)
        assertEquals(3, c.sidecars.size)
        assertEquals("ab", c.sidecars[1].sha256)
        assertEquals("", c.sidecars[0].sha256) // default when absent
    }

    @Test fun forBackend_filtersByOsAndBackend() {
        val c = SidecarCatalog.parse(sample)
        val win = c.forBackend("windows", "vulkan").map { it.engine }.toSet()
        assertEquals(setOf("whisper", "llama"), win)
        assertEquals(emptyList(), c.forBackend("macos-arm64", "metal"))
    }
}
