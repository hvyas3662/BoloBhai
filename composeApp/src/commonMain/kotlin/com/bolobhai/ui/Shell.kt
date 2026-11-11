package com.bolobhai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.app.Screen
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
            ContentHost(screen)
        }
    }
}

/** Temporary placeholder; replaced by the real screens in Group G. */
@Composable
private fun ContentHost(screen: Screen) {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text(screen.title, style = Bolo.type.h1, color = Bolo.colors.text)
        Text("Coming soon", style = Bolo.type.body, color = Bolo.colors.muted, modifier = Modifier.padding(top = 8.dp))
    }
}
