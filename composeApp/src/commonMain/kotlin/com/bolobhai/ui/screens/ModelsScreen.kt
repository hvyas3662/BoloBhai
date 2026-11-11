package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.app.ModelUi
import com.bolobhai.app.ModelsController
import com.bolobhai.domain.ModelState
import com.bolobhai.domain.formatBytes
import com.bolobhai.domain.formatProgress
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/** Models screen (BoloBhai.md §13.5): per-model download/use/delete + language & post-processing. */
@Composable
fun ModelsScreen(appState: AppState, controller: ModelsController) {
    val models by controller.models.collectAsState()
    val settings by appState.settings.collectAsState()

    ScreenScaffold(
        title = "Models",
        subtitle = "Everything runs on this computer. No cloud, no paid APIs.",
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            KindCard("Speech to text", models.filter { it.entry.kind == "stt" }, controller)
            KindCard("Post-processing LLM", models.filter { it.entry.kind == "llm" }, controller)

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
private fun KindCard(title: String, rows: List<ModelUi>, controller: ModelsController) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = Bolo.type.h2, color = c.text)
        rows.forEach { ModelRow(it, controller) }
    }
}

@Composable
private fun ModelRow(ui: ModelUi, controller: ModelsController) {
    val c = Bolo.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui.entry.name, style = Bolo.type.bodyMedium, color = c.text)
            if (ui.active) Text("Active", style = Bolo.type.meta, color = c.blue)
            ui.entry.note?.let { Text(it, style = Bolo.type.meta, color = c.muted) }
            Box(Modifier.weight(1f))
            Actions(ui, controller)
        }
        Text("${ui.entry.file} · ${formatBytes(ui.entry.sizeBytes)} · ${ui.entry.ram} RAM", style = Bolo.type.monoSmall, color = c.muted)
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
