package com.bolobhai.data

import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepoTest {

    private val file = Files.createTempDirectory("bolobhai-test").resolve("settings.json")

    @AfterTest
    fun cleanup() {
        file.deleteIfExists()
    }

    @Test
    fun load_missingFile_returnsDefaults() {
        file.deleteIfExists()
        assertEquals(Settings(), FileSettingsRepo(file).load())
    }

    @Test
    fun save_thenLoad_roundTrips() {
        val repo = FileSettingsRepo(file)
        val custom = Settings(onboardingDone = true, theme = "system", activeSlot = 2)
        repo.save(custom)
        assertEquals(custom, FileSettingsRepo(file).load())
    }

    @Test
    fun load_corruptFile_returnsDefaults() {
        file.writeText("{ this is not valid json ]")
        assertEquals(Settings(), FileSettingsRepo(file).load())
    }
}
