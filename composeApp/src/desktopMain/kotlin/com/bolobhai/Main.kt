package com.bolobhai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1440.dp, 900.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Bolo Bhai",
    ) {
        window.minimumSize = java.awt.Dimension(1100, 700)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bolo Bhai")
        }
    }
}
