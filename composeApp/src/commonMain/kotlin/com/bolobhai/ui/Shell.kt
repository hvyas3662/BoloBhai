package com.bolobhai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.app.ModelsController
import com.bolobhai.app.Screen
import com.bolobhai.ui.theme.BoloShapes
import kotlinx.coroutines.delay
import com.bolobhai.ui.screens.AllProfilesScreen
import com.bolobhai.ui.screens.DictionaryScreen
import com.bolobhai.ui.screens.HistoryScreen
import com.bolobhai.ui.screens.ModelsScreen
import com.bolobhai.ui.screens.QuickProfilesScreen
import com.bolobhai.ui.screens.SettingsScreen
import com.bolobhai.ui.theme.Bolo

/** Main-window layout: sidebar + right border + content host (BoloBhai.md §13.0). */
@Composable
fun Shell(appState: AppState, hotkeyLabel: String, modelsController: ModelsController) {
    val c = Bolo.colors
    val screen by appState.currentScreen.collectAsState()

    Row(Modifier.fillMaxSize().background(c.bg)) {
        Sidebar(appState, hotkeyLabel)
        // Right border of the sidebar.
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
        Box(Modifier.fillMaxSize()) {
            ContentHost(appState, screen, modelsController)
            ToastHost(appState)
        }
    }
}

/** Bottom-centered transient toast (BoloBhai.md §13.7); auto-dismisses after 2.2 s. */
@Composable
private fun ToastHost(appState: AppState) {
    val c = Bolo.colors
    val toast by appState.toast.collectAsState()
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            appState.clearToast()
        }
    }
    toast?.let { message ->
        Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.clip(BoloShapes.pill).background(c.card2).padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text(message, style = Bolo.type.body, color = c.text)
            }
        }
    }
}

@Composable
private fun ContentHost(appState: AppState, screen: Screen, modelsController: ModelsController) {
    when (screen) {
        Screen.History -> HistoryScreen(appState)
        Screen.QuickProfiles -> QuickProfilesScreen(appState)
        Screen.AllProfiles -> AllProfilesScreen()
        Screen.Dictionary -> DictionaryScreen(appState)
        Screen.Models -> ModelsScreen(appState, modelsController)
        Screen.Settings -> SettingsScreen(appState)
    }
}
