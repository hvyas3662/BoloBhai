package com.bolobhai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bolobhai.app.AppState
import com.bolobhai.data.FileSettingsRepo
import com.bolobhai.platform.currentPlatform
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloTheme

fun main() = application {
    val platform = remember { currentPlatform() }
    val appState = remember {
        // System theme resolves to the Dark default at startup in M1 (spec P5); manual toggle is live.
        AppState(FileSettingsRepo(platform.settingsFile()), systemDark = true)
    }
    val theme by appState.theme.collectAsState()

    val windowState = rememberWindowState(size = DpSize(1440.dp, 900.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Bolo Bhai",
    ) {
        window.minimumSize = java.awt.Dimension(1100, 700)
        BoloTheme(mode = theme, systemDark = appState.systemDark) {
            // Placeholder until the Shell lands in Group F.
            Box(
                Modifier.fillMaxSize().background(Bolo.colors.bg),
                contentAlignment = Alignment.Center,
            ) {
                Text("Bolo Bhai", style = Bolo.type.onboardingTitle, color = Bolo.colors.text)
            }
        }
    }
}
