package com.bolobhai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.bolobhai.app.ModelsController
import com.bolobhai.app.Screen
import com.bolobhai.audio.AudioRecorder
import com.bolobhai.domain.ModelState
import com.bolobhai.domain.formatBytes
import com.bolobhai.domain.formatProgress
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.components.Kbd
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes
import kotlinx.coroutines.delay

/** First-run onboarding, 5 steps inside the main window (BoloBhai.md §13.8). */
@Composable
fun OnboardingScreen(appState: AppState, models: ModelsController, hotkeyLabel: String) {
    val c = Bolo.colors
    var step by remember { mutableStateOf(0) }
    val steps = listOf("Welcome", "Permissions", "Hotkey", "Models", "Ready")

    val modelRows by models.models.collectAsState()
    val defaults = modelRows.filter { it.entry.default }
    val modelsReady = defaults.isNotEmpty() && defaults.all { it.state == ModelState.Installed }

    var micOk by remember { mutableStateOf(false) }
    LaunchedEffect(step) {
        while (step == 1) { micOk = AudioRecorder.inputDevices().isNotEmpty(); delay(1000) }
    }

    val canContinue = when (step) {
        1 -> micOk
        3 -> modelsReady
        else -> true
    }

    Column(Modifier.fillMaxSize().background(c.bg).padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Bolo Bhai setup", style = Bolo.type.h2, color = c.text)
            BoloButton("Skip for now", variant = ButtonVariant.Ghost, onClick = { finish(appState) })
        }
        // 5-segment progress bar.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEachIndexed { i, _ ->
                Box(Modifier.weight(1f).height(6.dp).clip(BoloShapes.pill).background(if (i <= step) c.blue else c.card2))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().widthIn(max = 820.dp).clip(BoloShapes.panelLarge).background(c.card)
            .border(1.dp, c.border, BoloShapes.panelLarge).padding(32.dp)) {
            when (step) {
                0 -> Welcome()
                1 -> Permissions(micOk)
                2 -> HotkeyStep(hotkeyLabel)
                3 -> ModelsStep(defaults, models)
                4 -> Ready()
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) BoloButton("Back", onClick = { step-- })
            Box(Modifier.weight(1f))
            if (step < 4) {
                BoloButton("Continue", variant = ButtonVariant.Primary, enabled = canContinue, onClick = { step++ })
            } else {
                BoloButton("Start dictating", variant = ButtonVariant.Primary, onClick = { finish(appState) })
            }
        }
    }
}

private fun finish(appState: AppState) {
    appState.setOnboardingDone(true)
    appState.navigateTo(Screen.History)
}

@Composable
private fun Welcome() = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Bolo Bhai", style = Bolo.type.onboardingTitle, color = Bolo.colors.text)
    Text("Local, offline Hinglish voice dictation.", style = Bolo.type.body, color = Bolo.colors.muted)
    Tile("Speak Hinglish", "Hindi + English, written back in clean Roman letters.")
    Tile("Fully offline", "Whisper and the LLM run on this computer. No cloud, no paid API.")
    Tile("Profiles on 1–5", "Press 1–5 while recording to pick how your text is cleaned up.")
}

@Composable
private fun Permissions(micOk: Boolean) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Permissions", style = Bolo.type.h2, color = Bolo.colors.text)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (micOk) "✓ Allowed" else "Waiting…", style = Bolo.type.bodyMedium, color = if (micOk) Bolo.colors.green else Bolo.colors.muted)
        Text("Microphone", style = Bolo.type.body, color = Bolo.colors.text)
    }
}

@Composable
private fun HotkeyStep(hotkeyLabel: String) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Test your hotkey", style = Bolo.type.h2, color = Bolo.colors.text)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Kbd(hotkeyLabel); Kbd(hotkeyLabel) }
    Text("Hold $hotkeyLabel to talk; double-tap for hands-free.", style = Bolo.type.body, color = Bolo.colors.muted)
}

@Composable
private fun ModelsStep(defaults: List<com.bolobhai.app.ModelUi>, controller: ModelsController) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    val total = defaults.sumOf { it.entry.sizeBytes }
    Text("Download models", style = Bolo.type.h2, color = Bolo.colors.text)
    Text("Needs ${formatBytes(total)} free disk.", style = Bolo.type.meta, color = Bolo.colors.muted)
    defaults.forEach { ui ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(ui.entry.name, style = Bolo.type.bodyMedium, color = Bolo.colors.text)
                when {
                    ui.state == ModelState.Installed -> Text("SHA-256 verified", style = Bolo.type.meta, color = Bolo.colors.green)
                    ui.downloading -> Text("Downloading…", style = Bolo.type.meta, color = Bolo.colors.muted)
                    else -> Text("Waiting", style = Bolo.type.meta, color = Bolo.colors.muted)
                }
            }
            ui.progress?.let { p ->
                LinearProgressIndicator(progress = { if (p.total > 0) p.done.toFloat() / p.total else 0f }, modifier = Modifier.fillMaxWidth().height(6.dp))
                Text(formatProgress(p.done, p.total, p.bytesPerSec), style = Bolo.type.monoSmall, color = Bolo.colors.muted)
            }
        }
    }
    BoloButton("Download ${formatBytes(total)}", variant = ButtonVariant.Primary, onClick = {
        defaults.filter { it.state != ModelState.Installed }.forEach { controller.download(it.entry) }
    })
}

@Composable
private fun Ready() = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Sab set hai. Ab bolo!", style = Bolo.type.onboardingTitle, color = Bolo.colors.text)
    Tile("Hold to talk, release to paste", "fn / Alt")
    Tile("Hands-free, tap again to stop", "double-tap")
    Tile("Switch profile while recording", "1–5")
    Tile("Cancel recording", "Esc")
}

@Composable
private fun Tile(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(Bolo.colors.card2).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = Bolo.type.bodyMedium, color = Bolo.colors.text)
        Text(body, style = Bolo.type.meta, color = Bolo.colors.muted)
    }
}
