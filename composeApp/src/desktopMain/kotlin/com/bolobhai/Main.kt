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
import com.bolobhai.data.DictionaryRepo
import com.bolobhai.data.FileSettingsRepo
import com.bolobhai.app.ModelsController
import com.bolobhai.data.HistoryRepo
import com.bolobhai.data.ModelCatalog
import com.bolobhai.data.ModelRepo
import com.bolobhai.data.ProfilesRepo
import com.bolobhai.data.ProfilesWatcher
import com.bolobhai.domain.HotkeyInput
import com.bolobhai.platform.LlamaLauncher
import com.bolobhai.platform.WhisperLauncher
import com.bolobhai.platform.currentPlatform
import com.bolobhai.platform.mac.MacHelper
import com.bolobhai.platform.win.WinKeyboardHook
import com.bolobhai.platform.win.WinPaster
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.io.path.Path
import com.bolobhai.platform.win.dictationKeyFromSetting
import com.bolobhai.ui.OnboardingScreen
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
            dictionaryRepo = DictionaryRepo(platform.appDataRoot().resolve("dictionary.yaml")),
        )
    }
    val appScope = rememberCoroutineScope()
    val watcher = remember { ProfilesWatcher(profilesDir, appScope) }
    val modelsController = remember {
        ModelsController(appState, ModelRepo(platform.appDataRoot().resolve("models")), ModelCatalog.load().models, appScope)
    }
    val theme by appState.theme.collectAsState()
    val settings by appState.settings.collectAsState()
    val isWindows = platform.id == "windows"
    val isMac = platform.id == "mac"
    val hotkeyLabel = if (isWindows) settings.hotkey.windows else settings.hotkey.mac
    val pasteShortcut = if (isWindows) "Ctrl+V" else "Cmd+V"
    val macHelper = remember { MacHelper(macHelperBinary()) }

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
            paste = when {
                isWindows -> WinPaster::paste
                isMac -> { text ->
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                    macHelper.paste()
                }
                else -> { _ -> }
            },
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
        appState.testProfileOp = controller::testProfile
        appState.openProfilesFolder = { platform.openFolder(profilesDir) }
        appState.applyLaunchAtLogin = platform::setLaunchAtLogin
        appState.initProfiles()
        appState.loadHistory()
        appState.runAudioRetention()
        watcher.start { appState.reloadProfiles() }
        controller.startSidecars()
        // macOS: the Swift helper is our global key source; keep its capture flag in sync so it
        // swallows 1–5/Esc only while recording (BoloBhai.md §6.4).
        if (isMac) macHelper.start { ev ->
            controller.onInput(ev)
            macHelper.setCapture(controller.isRecording())
        }
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

    // §4/§8: no Dock icon while the window is closed (accessory); regular while it's open.
    LaunchedEffect(windowVisible) { if (isMac) macHelper.setActivationPolicy(windowVisible) }

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
                macHelper.stop()
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
            if (settings.onboardingDone) {
                Shell(appState, hotkeyLabel, modelsController)
            } else {
                OnboardingScreen(appState, modelsController, hotkeyLabel)
            }
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

/** Bundled Swift helper path: `sidecars/mac-<arch>/bolobhai-mac-helper` (BoloBhai.md §17). */
private fun macHelperBinary() = Path(
    "sidecars",
    if (System.getProperty("os.arch").lowercase().contains("aarch64")) "mac-arm64" else "mac-x64",
    "bolobhai-mac-helper",
)
