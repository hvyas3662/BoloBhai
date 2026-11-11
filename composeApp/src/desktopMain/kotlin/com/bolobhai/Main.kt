package com.bolobhai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloTheme
import com.bolobhai.ui.theme.ThemeMode

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1440.dp, 900.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Bolo Bhai",
    ) {
        window.minimumSize = java.awt.Dimension(1100, 700)
        BoloTheme(mode = ThemeMode.Dark, systemDark = true) {
            Box(
                Modifier.fillMaxSize().background(Bolo.colors.bg),
                contentAlignment = Alignment.Center,
            ) {
                Text("Bolo Bhai", style = Bolo.type.onboardingTitle, color = Bolo.colors.text)
            }
        }
    }
}
