package com.bolobhai.app

import com.bolobhai.data.Settings
import com.bolobhai.data.SettingsRepo
import com.bolobhai.ui.theme.ThemeMode
import com.bolobhai.ui.theme.asSettingValue
import com.bolobhai.ui.theme.themeModeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Resolves the active Quick Profile slot (BoloBhai.md §7): keep [requested] if it holds a
 * profile, else fall back to the first non-empty slot, else slot 1.
 */
fun resolveActiveSlot(quickSlots: Map<String, String?>, requested: Int): Int {
    if (quickSlots[requested.toString()] != null) return requested
    for (n in 1..5) if (quickSlots[n.toString()] != null) return n
    return 1
}

/** Single source of truth for all windows (BoloBhai.md §4). */
class AppState(
    private val settingsRepo: SettingsRepo,
    val systemDark: Boolean,
) {
    private val _settings = MutableStateFlow(settingsRepo.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.History)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _theme = MutableStateFlow(themeModeOf(_settings.value.theme))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _activeSlot =
        MutableStateFlow(resolveActiveSlot(_settings.value.quickSlots, _settings.value.activeSlot))
    val activeSlot: StateFlow<Int> = _activeSlot.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        persist { it.copy(theme = mode.asSettingValue()) }
    }

    fun setActiveSlot(slot: Int) {
        val resolved = resolveActiveSlot(_settings.value.quickSlots, slot)
        _activeSlot.value = resolved
        persist { it.copy(activeSlot = resolved) }
    }

    private fun persist(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        settingsRepo.save(next)
    }
}
