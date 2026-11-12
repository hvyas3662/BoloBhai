package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.audio.AudioRecorder
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes
import com.bolobhai.ui.theme.ThemeMode

/** Settings (BoloBhai.md §13.6). Windows controls only; mac key/accessibility rows arrive with M2. */
@Composable
fun SettingsScreen(appState: AppState) {
    val settings by appState.settings.collectAsState()
    val theme by appState.theme.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    val historyCount = appState.history.collectAsState().value.size

    ScreenScaffold(
        title = "Settings",
        trailing = { BoloButton("Run first-time setup again", onClick = { appState.setOnboardingDone(false) }) },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Two-column grid (BoloBhai.md §13.6).
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Card("Hotkey", Modifier.weight(1f).fillMaxHeight()) {
                    Segmented(listOf("Fn", "Alt", "Right Ctrl"), settings.hotkey.windows) { appState.setHotkeyWindows(it) }
                    Text("Fn works only if your laptop reports it to Windows. Takes effect on restart.", style = Bolo.type.meta, color = Bolo.colors.muted)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Double-tap", style = Bolo.type.body, color = Bolo.colors.text)
                        Slider(
                            value = settings.doubleTapMs.toFloat(),
                            onValueChange = { appState.setDoubleTapMs((it / 25).toInt() * 25) },
                            valueRange = 250f..600f, steps = (600 - 250) / 25 - 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${settings.doubleTapMs} ms", style = Bolo.type.mono, color = Bolo.colors.muted)
                    }
                }
                Card("Audio", Modifier.weight(1f).fillMaxHeight()) {
                    val devices = remember { listOf<String?>(null) + AudioRecorder.inputDevices() }
                    Picker(
                        label = settings.microphone ?: "System default",
                        options = devices.map { (it ?: "System default") to it },
                        onPick = { appState.setMicrophone(it) },
                    )
                    LabeledSwitch("Start / stop sound", settings.sounds) { appState.setSounds(it) }
                }
            }

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Card("General", Modifier.weight(1f).fillMaxHeight()) {
                    Segmented(listOf("Dark", "Light", "System"), theme.name) { appState.setTheme(ThemeMode.valueOf(it)) }
                    LabeledSwitch("Launch at login", settings.launchAtLogin) { appState.setLaunchAtLogin(it) }
                }
                Card("History & storage", Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Keep audio for", style = Bolo.type.body, color = Bolo.colors.text)
                        val labels = mapOf("7d" to "7 days", "30d" to "30 days", "forever" to "Forever", "none" to "Don't save")
                        SegmentedMapped(labels, settings.keepAudio) { appState.setKeepAudio(it) }
                    }
                    BoloButton("Clear all history", variant = ButtonVariant.Danger, onClick = { confirmClear = true })
                }
            }

            com.bolobhai.ui.LogsPanel(onToast = { appState.toast(it) })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history", style = Bolo.type.h2, color = Bolo.colors.text) },
            confirmButton = {
                BoloButton("Clear", variant = ButtonVariant.Danger, onClick = { appState.clearHistory(); confirmClear = false })
            },
            dismissButton = { BoloButton("Cancel", onClick = { confirmClear = false }) },
            text = {
                Text(
                    "Clear all history? All $historyCount dictations and their audio files will be deleted. This cannot be undone.",
                    style = Bolo.type.body, color = Bolo.colors.text,
                )
            },
            containerColor = Bolo.colors.panel,
        )
    }
}

@Composable
private fun Card(title: String, modifier: Modifier = Modifier, body: @Composable () -> Unit) {
    val c = Bolo.colors
    Column(
        modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = Bolo.type.h2, color = c.text)
        body()
    }
}

@Composable
private fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) =
    SegmentedMapped(options.associateWith { it }, selected, onSelect)

/** Segmented control over stored-value -> label pairs; the current [selected] stored value is lit. */
@Composable
private fun SegmentedMapped(labels: Map<String, String>, selected: String, onSelect: (String) -> Unit) {
    val c = Bolo.colors
    Row(
        Modifier.clip(BoloShapes.button).background(c.card2).border(1.dp, c.border, BoloShapes.button),
    ) {
        labels.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier.clip(BoloShapes.button).background(if (on) c.blue else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(value) }.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(label, style = Bolo.type.button, color = if (on) c.onAccent else c.text)
            }
        }
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Bolo.type.body, color = Bolo.colors.text)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Picker(label: String, options: List<Pair<String, String?>>, onPick: (String?) -> Unit) {
    val c = Bolo.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(BoloShapes.input).background(c.input).border(1.dp, c.border, BoloShapes.input)
                .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text(label, style = Bolo.type.body, color = c.text) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onPick(value); open = false })
            }
        }
    }
}
