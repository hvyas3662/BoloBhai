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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bolobhai.app.AppState
import com.bolobhai.app.ModelUi
import com.bolobhai.app.ModelsController
import com.bolobhai.domain.AUTO_DETECT
import com.bolobhai.domain.ModelState
import com.bolobhai.domain.SystemHardware
import com.bolobhai.domain.formatBytes
import com.bolobhai.domain.formatProgress
import com.bolobhai.domain.spokenLanguagesFor
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.BoloIcons
import com.bolobhai.ui.components.BoloTextField
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/** Models screen (BoloBhai.md §13.5): active model per kind + a picker of all models, with a
 * hardware capacity check (#8) and active-only cards (#11). */
@Composable
fun ModelsScreen(appState: AppState, controller: ModelsController) {
    val models by controller.models.collectAsState()
    val settings by appState.settings.collectAsState()

    // Capacity depends on swap mode and the active models; recompute when those change.
    LaunchedEffect(settings.lowVramSwap, settings.activeWhisperModel, settings.activeLlmModel) {
        controller.refresh()
    }

    val usedOnDisk = models.filter { it.state == ModelState.Installed }.sumOf { it.entry.sizeBytes }
    ScreenScaffold(
        title = "Models",
        subtitle = "Everything runs on this computer · ${formatBytes(usedOnDisk)} used on disk.",
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HardwareCard(controller.hardware)

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KindSection("Speech to text", models.filter { it.entry.kind == "stt" }, controller, Modifier.weight(1f).fillMaxHeight())
                KindSection("Post-processing LLM", models.filter { it.entry.kind == "llm" }, controller, Modifier.weight(1f).fillMaxHeight())
            }

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SettingsCard(Modifier.weight(1f).fillMaxHeight()) {
                    Text("Spoken language", style = Bolo.type.h2, color = Bolo.colors.text)
                    val activeStt = models.firstOrNull { it.entry.kind == "stt" && it.active }?.entry
                        ?: models.firstOrNull { it.entry.kind == "stt" }?.entry
                    val langs = spokenLanguagesFor(activeStt?.file, activeStt?.languages)
                    val current = langs.firstOrNull { it.code == settings.language } ?: langs.firstOrNull()
                    LanguagePicker(
                        label = current?.label ?: settings.language,
                        options = langs.map { it.label to it.code },
                        onPick = { appState.setLanguage(it) },
                    )
                    Text(
                        "Transcription uses this language in strict mode. The list reflects what the selected model supports.",
                        style = Bolo.type.meta, color = Bolo.colors.muted,
                    )
                    if (settings.language == AUTO_DETECT) {
                        Text("Auto-detect is not advised for Hinglish.", style = Bolo.type.meta, color = Bolo.colors.orange)
                    }
                }

                SettingsCard(Modifier.weight(1f).fillMaxHeight()) {
                    Text("Post-processing", style = Bolo.type.h2, color = Bolo.colors.text)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Clean up transcript with LLM", style = Bolo.type.body, color = Bolo.colors.text)
                        com.bolobhai.ui.components.BoloSwitch(settings.postProcessing) { appState.setPostProcessing(it) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Low-VRAM mode · swap models per dictation", style = Bolo.type.body, color = Bolo.colors.text)
                        com.bolobhai.ui.components.BoloSwitch(settings.lowVramSwap) { appState.setLowVramSwap(it) }
                    }
                    Text(
                        "For small GPUs (≈4 GB): runs Whisper and the LLM one at a time so the LLM fully offloads. " +
                            "Best quality, but adds a model (re)load each dictation.",
                        style = Bolo.type.meta, color = Bolo.colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun HardwareCard(hw: SystemHardware) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Your computer", style = Bolo.type.h2, color = c.text)
        Text("CPU · ${hw.cpuName}", style = Bolo.type.monoSmall, color = c.muted)
        Text("RAM · ${if (hw.ramBytes > 0) formatBytes(hw.ramBytes) else "unknown"}", style = Bolo.type.monoSmall, color = c.muted)
        val gpu = hw.gpuName ?: "No dedicated GPU"
        val vram = hw.vramBytes?.let { " · ${formatBytes(it)} VRAM" } ?: ""
        Text("GPU · $gpu$vram · running on ${hw.backend.uppercase()}", style = Bolo.type.monoSmall, color = c.muted)
    }
}

@Composable
private fun KindSection(title: String, rows: List<ModelUi>, controller: ModelsController, modifier: Modifier = Modifier) {
    val c = Bolo.colors
    val active = rows.firstOrNull { it.active } ?: rows.firstOrNull()
    Column(
        modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = Bolo.type.h2, color = c.text)
        if (active != null) ModelCard(active, controller, inMenu = false)
        ModelPicker(rows, controller)
    }
}

/** "Change model" opens a searchable dialog of all models of this kind (#11). */
@Composable
private fun ModelPicker(rows: List<ModelUi>, controller: ModelsController) {
    val c = Bolo.colors
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(BoloShapes.input).background(c.input).border(1.dp, c.border, BoloShapes.input)
            .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Change model", style = Bolo.type.button, color = c.text, modifier = Modifier.weight(1f))
        Icon(BoloIcons.ChevronDown, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
    }
    if (open) ModelPickerDialog(rows, controller) { open = false }
}

/**
 * Searchable model chooser. Clicking a downloaded (non-active) model selects it and closes. Download
 * runs in the controller's own scope, so closing the dialog mid-download keeps it going in the
 * background — reopening shows the live progress again.
 */
@Composable
private fun ModelPickerDialog(rows: List<ModelUi>, controller: ModelsController, onDismiss: () -> Unit) {
    val c = Bolo.colors
    var query by remember { mutableStateOf("") }
    val filtered = rows.filter {
        query.isBlank() || it.entry.name.contains(query, ignoreCase = true) || it.entry.file.contains(query, ignoreCase = true)
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(560.dp).heightIn(max = 640.dp).clip(BoloShapes.panelLarge).background(c.panel)
                .border(1.dp, c.border, BoloShapes.panelLarge).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Choose model", style = Bolo.type.h2, color = c.text)
            BoloTextField(query, { query = it }, placeholder = "Search models…", modifier = Modifier.fillMaxWidth())
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (filtered.isEmpty()) Text("No models match “$query”.", style = Bolo.type.meta, color = c.muted)
                filtered.forEach { ui ->
                    val selectable = ui.state == ModelState.Installed && !ui.active
                    ModelCard(ui, controller, inMenu = true, onClick = if (selectable) ({ controller.use(ui.entry); onDismiss() }) else null)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BoloButton("Close", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ModelCard(ui: ModelUi, controller: ModelsController, inMenu: Boolean, onClick: (() -> Unit)? = null) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth()
            .then(if (inMenu) Modifier.clip(BoloShapes.card).background(c.card2) else Modifier)
            .then(if (ui.exceedsCapacity) Modifier.border(1.dp, c.redBorder, BoloShapes.card) else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .then(if (inMenu) Modifier.padding(14.dp) else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui.entry.name, style = Bolo.type.bodyMedium, color = c.text)
            if (ui.active) Text("Active", style = Bolo.type.meta, color = c.blue)
            ui.entry.note?.let { Text(it, style = Bolo.type.meta, color = c.muted) }
            Box(Modifier.weight(1f))
            Actions(ui, controller)
        }
        Text("${ui.entry.file} · ${formatBytes(ui.entry.sizeBytes)} · ${ui.entry.ram} RAM", style = Bolo.type.monoSmall, color = c.muted)
        if (ui.exceedsCapacity) {
            Text(
                "May exceed your ${if (controller.hardware.usesVram) "VRAM" else "RAM"} — can still be used, but may be slow or fail.",
                style = Bolo.type.meta, color = c.red,
            )
        }
        ui.progress?.let { p ->
            LinearProgressIndicator(
                progress = { if (p.total > 0) p.done.toFloat() / p.total else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text(formatProgress(p.done, p.total, p.bytesPerSec), style = Bolo.type.monoSmall, color = c.muted)
        }
    }
}

@Composable
private fun Actions(ui: ModelUi, controller: ModelsController) {
    var confirmDelete by remember { mutableStateOf(false) }
    when {
        ui.downloading -> BoloButton("Pause", onClick = { controller.pause(ui.entry) })
        ui.state == ModelState.Partial -> BoloButton("Resume", variant = ButtonVariant.Primary, onClick = { controller.download(ui.entry) })
        ui.state == ModelState.NotDownloaded -> BoloButton("Download", variant = ButtonVariant.Primary, onClick = { controller.download(ui.entry) })
        ui.state == ModelState.Installed && !ui.active -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoloButton("Use", variant = ButtonVariant.Primary, onClick = { controller.use(ui.entry) })
            BoloButton("Delete", variant = ButtonVariant.Danger, onClick = { confirmDelete = true })
        }
        // Installed + active: no actions (BoloBhai.md §13.5).
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete model", style = Bolo.type.h2, color = Bolo.colors.text) },
            confirmButton = {
                BoloButton("Delete", variant = ButtonVariant.Danger, onClick = { controller.delete(ui.entry); confirmDelete = false })
            },
            dismissButton = { BoloButton("Cancel", onClick = { confirmDelete = false }) },
            text = {
                Text(
                    "Delete ${ui.entry.name}? The ${formatBytes(ui.entry.sizeBytes)} file will be removed from disk. You can download it again later.",
                    style = Bolo.type.body, color = Bolo.colors.text,
                )
            },
            containerColor = Bolo.colors.panel,
        )
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, body: @Composable () -> Unit) {
    val c = Bolo.colors
    Column(
        modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { body() }
}

/** Searchable picker of spoken languages supported by the selected STT model. */
@Composable
private fun LanguagePicker(label: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    val c = Bolo.colors
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(BoloShapes.input).background(c.input).border(1.dp, c.border, BoloShapes.input)
            .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Bolo.type.body, color = c.text, modifier = Modifier.weight(1f))
        Icon(BoloIcons.ChevronDown, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
    }
    if (open) LanguagePickerDialog(options, onPick) { open = false }
}

@Composable
private fun LanguagePickerDialog(options: List<Pair<String, String>>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Bolo.colors
    var query by remember { mutableStateOf("") }
    val filtered = options.filter { query.isBlank() || it.first.contains(query, ignoreCase = true) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(420.dp).heightIn(max = 560.dp).clip(BoloShapes.panelLarge).background(c.panel)
                .border(1.dp, c.border, BoloShapes.panelLarge).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Choose language", style = Bolo.type.h2, color = c.text)
            BoloTextField(query, { query = it }, placeholder = "Search languages…", modifier = Modifier.fillMaxWidth())
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (filtered.isEmpty()) Text("No languages match “$query”.", style = Bolo.type.meta, color = c.muted)
                filtered.forEach { (text, value) ->
                    Text(
                        text, style = Bolo.type.body, color = c.text,
                        modifier = Modifier.fillMaxWidth().clip(BoloShapes.button).clickable { onPick(value); onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BoloButton("Close", onClick = onDismiss)
            }
        }
    }
}
