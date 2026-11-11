package com.bolobhai.domain

import com.bolobhai.data.ModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelViewTest {

    @Test fun `state from file and part presence`() {
        assertEquals(ModelState.Installed, modelState(hasFile = true, hasPart = true))
        assertEquals(ModelState.Partial, modelState(hasFile = false, hasPart = true))
        assertEquals(ModelState.NotDownloaded, modelState(hasFile = false, hasPart = false))
    }

    @Test fun `formatBytes picks GB or MB`() {
        assertEquals("2.50 GB", formatBytes(2_500_000_000))
        assertEquals("550 MB", formatBytes(550_000_000))
    }

    @Test fun `formatProgress matches spec layout`() {
        assertEquals("46% · 1.15 of 2.50 GB · 41.6 MB/s", formatProgress(1_150_000_000, 2_500_000_000, 41_600_000))
    }

    @Test fun `sha256 of empty is known digest`() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(ByteArray(0)))
    }

    @Test fun `bundled catalog parses with the two defaults`() {
        val catalog = ModelCatalog.load()
        assertTrue(catalog.models.size >= 4)
        assertEquals(2, catalog.models.count { it.default })
        assertTrue(catalog.models.any { it.kind == "stt" } && catalog.models.any { it.kind == "llm" })
    }
}
