package com.bolobhai

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.bolobhai.app.AppState
import com.bolobhai.app.HotkeyController
import com.bolobhai.app.Screen
import com.bolobhai.data.FileSettingsRepo
import com.bolobhai.data.HistoryRepo
import com.bolobhai.data.ProfilesRepo
import com.bolobhai.data.ProfilesWatcher
import com.bolobhai.domain.HotkeyInput
import com.bolobhai.platform.LlamaLauncher
import com.bolobhai.platform.WhisperLauncher
import com.bolobhai.platform.currentPlatform
import com.bolobhai.platform.win.WinKeyboardHook
import com.bolobhai.platform.win.WinPaster
import com.bolobhai.platform.win.dictationKeyFromSetting
import com.bolobhai.ui.Shell
import com.bolobhai.ui.components.BoloIcons
import com.bolobhai.ui.pill.PillWindow
import com.bolobhai.ui.theme.BoloTheme

fun main() = application {
    val platform = remember { currentPlatform() }
    val profilesDir = remember { platform.appDataRoot().resolve("profiles") }
    val audioDir = remember { platform.appDataRoot().resolve("audio") }
    val appState = remember {
        AppState(
            settingsRepo = FileSettingsRepo(platform.settingsFile()),
            systemDark = true,
            profilesRepo = ProfilesRepo(profilesDir),
            historyRepo = HistoryRepo(platform.appDataRoot().resolve("history.db")),
        )
    }
    val appScope = rememberCoroutineScope()
    val watcher = remember { ProfilesWatcher(profilesDir, appScope) }
    val theme by appState.theme.collectAsState()
    val settings by appState.settings.collectAsState()
    val isWindows = platform.id == "windows"
    val hotkeyLabel = if (isWindows) settings.hotkey.windows else settings.hotkey.mac
    val pasteShortcut = if (isWindows) "Ctrl+V" else "Cmd+V"

    val controller = remember {
        HotkeyController(
            appState = appState,
            hotkeyLabel = hotkeyLabel,
            pasteShortcut = pasteShortcut,
            launchWhisper = { port ->
                WhisperLauncher.launch(
                    port,
                    platform.appDataRoot().resolve("models").resolve(appState.settings.value.activeWhisperModel),
                )
            },
            launchLlama = { port ->
                LlamaLauncher.launch(
                    port,
                    platform.appDataRoot().resolve("models").resolve(appState.settings.value.activeLlmModel),
                )
            },
            paste = if (isWindows) WinPaster::paste else { _ -> },
            audioDir = audioDir,
        )
    }
    val pill by controller.pill.collectAsState()
    val activeSlot by appState.activeSlot.collectAsState()
    val profiles by appState.profiles.collectAsState()

    // Load profiles (+ defaults), watch for external edits, and start sidecars once.
    LaunchedEffect(Unit) {
        appState.reprocessOp = controller::reprocess
        appState.retranscribeOp = controller::retranscribe
        appState.retryOp = controller::retry
        appState.initProfiles()
        appState.loadHistory()
        watcher.start { appState.reloadProfiles() }
        controller.startSidecars()
    }
    val hook = remember {
        if (isWindows) {
            WinKeyboardHook(
                dictationKey = dictationKeyFromSetting(appState.settings.value.hotkey.windows),
                onInput = controller::onInput,
                isRecording = controller::isRecording,
            ).also { it.start() }
        } else {
            null
        }
    }

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
            for (slot in 1..5) {
                val file = settings.quickSlots[slot.toString()]
                val name = file?.let { f -> profiles.firstOrNull { it.fileName == f }?.name } ?: "Empty"
                val marker = if (slot == activeSlot) "● " else "   "
                Item("$marker[$slot] $name", enabled = file != null) { appState.setActiveSlot(slot) }
            }
            Separator()
            Item("Quit Bolo Bhai") {
                hook?.stop()
                watcher.stop()
                exitApplication()
            }
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

    // Floating recording pill (shown only while a gesture is active).
    pill?.let { state ->
        PillWindow(
            state = state,
            themeMode = theme,
            systemDark = appState.systemDark,
            onStop = { controller.onInput(HotkeyInput.KeyDown) },
            onCancel = { controller.onInput(HotkeyInput.Esc) },
        )
    }
}
