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
import androidx.compose.material3.DropdownMenu
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
import com.bolobhai.app.AppState
import com.bolobhai.app.ModelUi
import com.bolobhai.app.ModelsController
import com.bolobhai.domain.ModelState
import com.bolobhai.domain.SystemHardware
import com.bolobhai.domain.formatBytes
import com.bolobhai.domain.formatProgress
import com.bolobhai.ui.components.BoloButton
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

            SettingsCard {
                Text("Spoken language", style = Bolo.type.sectionLabel, color = Bolo.colors.muted)
                val langs = mapOf("hi" to "Hindi / Hinglish", "en" to "English", "auto" to "Auto-detect")
                Segmented(langs, settings.language) { appState.setLanguage(it) }
                if (settings.language == "auto") {
                    Text("Auto-detect is not advised for Hinglish.", style = Bolo.type.meta, color = Bolo.colors.orange)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Post-processing with LLM", style = Bolo.type.body, color = Bolo.colors.text)
                    androidx.compose.material3.Switch(settings.postProcessing, { appState.setPostProcessing(it) })
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

/** A "Change model ▾" dropdown listing every model of this kind as a readable card (#11). */
@Composable
private fun ModelPicker(rows: List<ModelUi>, controller: ModelsController) {
    val c = Bolo.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(BoloShapes.input).background(c.input).border(1.dp, c.border, BoloShapes.input)
                .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text("Change model  ▾", style = Bolo.type.button, color = c.text) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(Modifier.width(420.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { ModelCard(it, controller, inMenu = true) }
            }
        }
    }
}

@Composable
private fun ModelCard(ui: ModelUi, controller: ModelsController, inMenu: Boolean) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth()
            .let { if (inMenu) it.clip(BoloShapes.card).background(c.card2).padding(14.dp) else it }
            .let { if (ui.exceedsCapacity) it.border(1.dp, c.redBorder, BoloShapes.card) else it },
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
    when {
        ui.downloading -> BoloButton("Pause", onClick = { controller.pause(ui.entry) })
        ui.state == ModelState.Partial -> BoloButton("Resume", variant = ButtonVariant.Primary, onClick = { controller.download(ui.entry) })
        ui.state == ModelState.NotDownloaded -> BoloButton("Download", variant = ButtonVariant.Primary, onClick = { controller.download(ui.entry) })
        ui.state == ModelState.Installed && !ui.active -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BoloButton("Use", variant = ButtonVariant.Primary, onClick = { controller.use(ui.entry) })
            BoloButton("Delete", variant = ButtonVariant.Danger, onClick = { controller.delete(ui.entry) })
        }
        // Installed + active: no actions (BoloBhai.md §13.5).
    }
}

@Composable
private fun SettingsCard(body: @Composable () -> Unit) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { body() }
}

@Composable
private fun Segmented(labels: Map<String, String>, selected: String, onSelect: (String) -> Unit) {
    val c = Bolo.colors
    Row(Modifier.clip(BoloShapes.button).background(c.card2).border(1.dp, c.border, BoloShapes.button)) {
        labels.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier.clip(BoloShapes.button)
                    .background(if (on) c.blue else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text(label, style = Bolo.type.button, color = if (on) c.onAccent else c.text) }
        }
    }
}
