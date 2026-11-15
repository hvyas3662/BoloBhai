package com.bolobhai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState

/** Logs screen (own nav module): just the log viewer, filling the space below the header. */
@Composable
fun LogsScreen(appState: AppState) {
    ScreenScaffold(
        title = "Logs",
        subtitle = "App, Whisper and LLM logs for troubleshooting.",
    ) {
        Box(Modifier.fillMaxSize().padding(32.dp)) {
            com.bolobhai.ui.LogsPanel(onToast = { appState.toast(it) })
        }
    }
}
