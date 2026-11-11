package com.bolobhai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.app.Screen
import com.bolobhai.ui.screens.AllProfilesScreen
import com.bolobhai.ui.screens.DictionaryScreen
import com.bolobhai.ui.screens.HistoryScreen
import com.bolobhai.ui.screens.ModelsScreen
import com.bolobhai.ui.screens.QuickProfilesScreen
import com.bolobhai.ui.screens.SettingsScreen
import com.bolobhai.ui.theme.Bolo

/** Main-window layout: sidebar + right border + content host (BoloBhai.md §13.0). */
@Composable
fun Shell(appState: AppState, hotkeyLabel: String) {
    val c = Bolo.colors
    val screen by appState.currentScreen.collectAsState()

    Row(Modifier.fillMaxSize().background(c.bg)) {
        Sidebar(appState, hotkeyLabel)
        // Right border of the sidebar.
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
        Box(Modifier.fillMaxSize()) {
            ContentHost(appState, screen)
        }
    }
}

@Composable
private fun ContentHost(appState: AppState, screen: Screen) {
    when (screen) {
        Screen.History -> HistoryScreen()
        Screen.QuickProfiles -> QuickProfilesScreen(appState)
        Screen.AllProfiles -> AllProfilesScreen()
        Screen.Dictionary -> DictionaryScreen()
        Screen.Models -> ModelsScreen()
        Screen.Settings -> SettingsScreen()
    }
}
