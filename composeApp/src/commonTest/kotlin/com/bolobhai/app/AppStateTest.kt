package com.bolobhai.app

import com.bolobhai.data.HistoryRepo
import com.bolobhai.data.NewVersion
import com.bolobhai.data.Settings
import com.bolobhai.data.SettingsRepo
import java.nio.file.Files
import com.bolobhai.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeSettingsRepo(var stored: Settings = Settings()) : SettingsRepo {
    var saveCount = 0
    override fun load(): Settings = stored
    override fun save(settings: Settings) {
        stored = settings
        saveCount++
    }
}

class AppStateTest {

    private fun state(settings: Settings = Settings()) =
        AppState(FakeSettingsRepo(settings), systemDark = true)

    @Test
    fun initialScreen_isHistory() {
        assertEquals(Screen.History, state().currentScreen.value)
    }

    @Test
    fun saveDictation_insertsAndRefreshesHistory() {
        val db = Files.createTempDirectory("appstate-hist").resolve("history.db")
        val repo = HistoryRepo(db)
        val s = AppState(FakeSettingsRepo(), systemDark = true, historyRepo = repo)
        s.loadHistory()
        assertEquals(0, s.history.value.size)
        s.saveDictation(
            createdAt = 1000, durationMs = 5000, rawTranscript = "hi there", audioPath = null,
            status = "ok", version = NewVersion(null, "Raw (no LLM)", "Hi there", null, null),
        )
        assertEquals(1, s.history.value.size)
        assertEquals("Hi there", s.history.value.first().current?.output)
        repo.close()
    }

    @Test
    fun navigateTo_changesScreen() {
        val s = state()
        s.navigateTo(Screen.Settings)
        assertEquals(Screen.Settings, s.currentScreen.value)
    }

    @Test
    fun setTheme_updatesAndPersists() {
        val repo = FakeSettingsRepo()
        val s = AppState(repo, systemDark = true)
        s.setTheme(ThemeMode.Light)
        assertEquals(ThemeMode.Light, s.theme.value)
        assertEquals("light", repo.stored.theme)
    }

    @Test
    fun theme_readsFromStoredSettings() {
        assertEquals(ThemeMode.System, state(Settings(theme = "system")).theme.value)
    }

    // §7: active slot fallback
    @Test
    fun resolveActiveSlot_keepsAssignedSlot() {
        val slots = mapOf("1" to "a.md", "2" to "b.md", "3" to null, "4" to null, "5" to null)
        assertEquals(2, resolveActiveSlot(slots, requested = 2))
    }

    @Test
    fun resolveActiveSlot_emptySlot_fallsBackToFirstNonEmpty() {
        val slots = mapOf("1" to null, "2" to "b.md", "3" to null, "4" to null, "5" to null)
        assertEquals(2, resolveActiveSlot(slots, requested = 4))
    }

    @Test
    fun resolveActiveSlot_allEmpty_fallsBackToOne() {
        val slots = mapOf("1" to null, "2" to null, "3" to null, "4" to null, "5" to null)
        assertEquals(1, resolveActiveSlot(slots, requested = 3))
    }

    @Test
    fun setActiveSlot_emptySlot_fallsBackAndPersists() {
        val repo = FakeSettingsRepo(
            Settings(quickSlots = mapOf("1" to "a.md", "2" to null, "3" to null, "4" to null, "5" to null)),
        )
        val s = AppState(repo, systemDark = true)
        s.setActiveSlot(5)
        assertEquals(1, s.activeSlot.value)
        assertEquals(1, repo.stored.activeSlot)
    }
}
