package com.bolobhai.app

import com.bolobhai.data.ProfilesRepo
import com.bolobhai.data.Settings
import com.bolobhai.data.SettingsRepo
import com.bolobhai.domain.Profile
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
    private val profilesRepo: ProfilesRepo? = null,
) {
    private val _settings = MutableStateFlow(settingsRepo.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    /** Slot numbers cleared by the last reload because their file vanished (for a toast). */
    private val _clearedSlots = MutableStateFlow<List<Int>>(emptyList())
    val clearedSlots: StateFlow<List<Int>> = _clearedSlots.asStateFlow()

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

    /** Copy shipped defaults if the folder is empty, then load (call once at startup). */
    fun initProfiles() {
        profilesRepo?.ensureDefaults()
        reloadProfiles()
    }

    /** (Re)load profiles from disk and clear any slot whose file disappeared (§11). */
    fun reloadProfiles() {
        val repo = profilesRepo ?: return
        val list = repo.list()
        _profiles.value = list
        val reconcile = reconcileQuickSlots(_settings.value.quickSlots, list.map { it.fileName }.toSet())
        _clearedSlots.value = reconcile.removed
        if (reconcile.removed.isNotEmpty()) {
            val resolved = resolveActiveSlot(reconcile.slots, _settings.value.activeSlot)
            _activeSlot.value = resolved
            persist { it.copy(quickSlots = reconcile.slots, activeSlot = resolved) }
        }
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

    /** Assign a profile to a slot (null = clear), keeping a profile in at most one slot (§7). */
    fun assignSlot(slot: Int, fileName: String?) {
        val nextSlots = assignSlotMap(_settings.value.quickSlots, slot, fileName)
        val resolved = resolveActiveSlot(nextSlots, _settings.value.activeSlot)
        _activeSlot.value = resolved
        persist { it.copy(quickSlots = nextSlots, activeSlot = resolved) }
    }

    /** Persist an edited/created profile body and reload. */
    fun saveProfile(profile: Profile) {
        val repo = profilesRepo ?: return
        repo.write(profile)
        reloadProfiles()
    }

    fun setPostProcessing(enabled: Boolean) = persist { it.copy(postProcessing = enabled) }

    private fun persist(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        _settings.value = next
        settingsRepo.save(next)
    }
}
