package com.bolobhai.ui.pill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.bolobhai.ui.theme.BoloTheme
import com.bolobhai.ui.theme.ThemeMode
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Toolkit

private const val PILL_WINDOW_W = 480
private const val PILL_WINDOW_H = 72

/**
 * The floating pill window (BoloBhai.md §8): undecorated, transparent, always-on-top and
 * non-focusing, at the bottom-centre of the screen under the cursor. On macOS the Swift helper
 * additionally reclassifies it as a non-activating panel (later milestone); on Windows these
 * Compose flags suffice to keep focus with the app the user is typing in.
 */
@Composable
fun ApplicationScope.PillWindow(
    state: PillUiState,
    themeMode: ThemeMode,
    systemDark: Boolean,
    onStop: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val position = remember { pillPosition() }
    val windowState = rememberWindowState(
        width = PILL_WINDOW_W.dp,
        height = PILL_WINDOW_H.dp,
        position = position,
    )
    Window(
        onCloseRequest = {},
        state = windowState,
        visible = true,
        title = "Bolo Bhai",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true,
    ) {
        window.isAutoRequestFocus = false
        window.focusableWindowState = false
        BoloTheme(themeMode, systemDark) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                RecordingPill(state, onStop = onStop, onCancel = onCancel, onRetry = onRetry)
            }
        }
    }
}

/** Bottom-centre of the screen containing the mouse cursor, 24 px above the visible bottom. */
private fun pillPosition(): WindowPosition {
    val pointer = MouseInfo.getPointerInfo()?.location
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val screen = env.screenDevices.firstOrNull { gd ->
        pointer != null && gd.defaultConfiguration.bounds.contains(pointer)
    } ?: env.defaultScreenDevice
    val cfg = screen.defaultConfiguration
    val bounds = cfg.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(cfg)

    val x = bounds.x + (bounds.width - PILL_WINDOW_W) / 2
    val visibleBottom = bounds.y + bounds.height - insets.bottom
    val y = visibleBottom - PILL_WINDOW_H - 24
    return WindowPosition(x.dp, y.dp)
}
