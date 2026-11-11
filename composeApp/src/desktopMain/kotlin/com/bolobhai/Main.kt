package com.bolobhai

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bolobhai.app.AppState
import com.bolobhai.app.Screen
import com.bolobhai.data.FileSettingsRepo
import com.bolobhai.platform.currentPlatform
import com.bolobhai.ui.Shell
import com.bolobhai.ui.components.BoloIcons
import com.bolobhai.ui.theme.BoloTheme

fun main() = application {
    val platform = remember { currentPlatform() }
    val appState = remember {
        // System theme resolves to the Dark default at startup in M1 (spec P5); manual toggle is live.
        AppState(FileSettingsRepo(platform.settingsFile()), systemDark = true)
    }
    val theme by appState.theme.collectAsState()
    val settings by appState.settings.collectAsState()
    val hotkeyLabel = if (platform.id == "windows") settings.hotkey.windows else settings.hotkey.mac

    // §6.0: closing the main window only hides it; the app keeps running and quits via the tray.
    var windowVisible by remember { mutableStateOf(true) }

    Tray(
        icon = rememberVectorPainter(BoloIcons.Mic),
        tooltip = "Bolo Bhai",
        menu = {
            Item("Open Bolo Bhai") {
                appState.navigateTo(Screen.History)
                windowVisible = true
            }
            Item("Settings…") {
                appState.navigateTo(Screen.Settings)
                windowVisible = true
            }
            Separator()
            Item("Quit Bolo Bhai") { exitApplication() }
        },
    )

    val windowState = rememberWindowState(size = DpSize(1440.dp, 900.dp))
    Window(
        onCloseRequest = { windowVisible = false },
        visible = windowVisible,
        state = windowState,
        title = "Bolo Bhai",
    ) {
        window.minimumSize = java.awt.Dimension(1100, 700)
        BoloTheme(mode = theme, systemDark = appState.systemDark) {
            Shell(appState, hotkeyLabel)
        }
    }
}
