package com.bolobhai.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import org.jetbrains.compose.resources.painterResource
import com.bolobhai.resources.Res
import com.bolobhai.resources.app_icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.app.ModelsController
import com.bolobhai.app.SidecarsController
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
import kotlinx.coroutines.launch

// Step indices for the guided flow (#3).
private const val WELCOME = 0
private const val MICROPHONE = 1
private const val MODELS = 2
private const val HOTKEY = 3
private const val TRY_IT = 4
private const val STARTUP = 5
private const val READY = 6

/** First-run onboarding, a guided flow inside the main window (BoloBhai.md §13.8, #3). */
@Composable
fun OnboardingScreen(appState: AppState, models: ModelsController, sidecars: SidecarsController, hotkeyLabel: String) {
    val c = Bolo.colors
    var step by remember { mutableStateOf(WELCOME) }
    val steps = listOf("Welcome", "Microphone", "Models", "Hotkey", "Try it", "Startup", "Ready")

    val modelRows by models.models.collectAsState()
    val defaults = modelRows.filter { it.entry.default }
    val settings by appState.settings.collectAsState()

    // A shared recorder for the mic-check meter and the speak-and-verify step.
    val recorder = remember { AudioRecorder() }
    var micLevel by remember { mutableStateOf(0f) }
    DisposableEffect(step) {
        if (step == MICROPHONE) runCatching { recorder.start(settings.microphone) { micLevel = it } }
        onDispose {
            if (recorder.isRecording) runCatching { recorder.cancel() }
            micLevel = 0f
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 880.dp).fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(Res.drawable.app_icon), contentDescription = "Bolo Bhai",
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                )
                Text("  Bolo Bhai setup", style = Bolo.type.h2, color = c.text)
                Spacer(Modifier.weight(1f))
                BoloButton("Skip for now", variant = ButtonVariant.Ghost, onClick = { finish(appState) })
            }
            // Progress bar with step labels.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEachIndexed { i, _ ->
                        Box(Modifier.weight(1f).height(6.dp).clip(BoloShapes.pill).background(if (i <= step) c.blue else c.card2))
                    }
                }
                Text("${step + 1}. ${steps[step]}", style = Bolo.type.meta, color = c.muted)
            }

            Box(
                Modifier.weight(1f).fillMaxWidth().clip(BoloShapes.panelLarge).background(c.card)
                    .border(1.dp, c.border, BoloShapes.panelLarge).padding(36.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (step) {
                    WELCOME -> Welcome()
                    MICROPHONE -> MicCheck(micLevel)
                    MODELS -> ModelsStep(defaults, models, sidecars)
                    HOTKEY -> HotkeyStep(hotkeyLabel)
                    TRY_IT -> TryItStep(appState, recorder, settings.microphone)
                    STARTUP -> StartupStep(settings.launchAtLogin) { appState.setLaunchAtLogin(it) }
                    READY -> Ready()
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > WELCOME) BoloButton("Back", onClick = { step-- })
                Spacer(Modifier.weight(1f))
                if (step < READY) {
                    BoloButton("Continue", variant = ButtonVariant.Primary, onClick = { step++ })
                } else {
                    BoloButton("Start dictating", variant = ButtonVariant.Primary, onClick = { finish(appState) })
                }
            }
        }
    }
}

private fun finish(appState: AppState) {
    appState.setOnboardingDone(true)
    appState.navigateTo(Screen.History)
}

@Composable
private fun Welcome() = Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth(),
) {
    Image(
        painterResource(Res.drawable.app_icon), contentDescription = "Bolo Bhai",
        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)),
    )
    Text("Bolo Bhai", style = Bolo.type.onboardingTitle, color = Bolo.colors.text)
    Text("Local, offline Hinglish voice dictation.", style = Bolo.type.body, color = Bolo.colors.muted)
    Spacer(Modifier.height(4.dp))
    Tile("Speak Hinglish", "Hindi + English, written back in clean Roman letters.")
    Tile("Fully offline", "Whisper and the LLM run on this computer. No cloud, no paid API.")
    Tile("Profiles on 1–5", "Press 1–5 while recording to pick how your text is cleaned up.")
}

/** Live microphone check: the bar should move while you speak (#3). */
@Composable
private fun MicCheck(level: Float) {
    val c = Bolo.colors
    // RMS is small; scale and smooth so normal speech clearly fills the bar.
    val fraction by animateFloatAsState((level * 3f).coerceIn(0f, 1f))
    val heard = level > 0.02f
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Check your microphone", style = Bolo.type.h2, color = c.text)
        Text("Say something — the bar should move.", style = Bolo.type.body, color = c.muted)
        Box(Modifier.fillMaxWidth().height(16.dp).clip(BoloShapes.pill).background(c.card2)) {
            Box(Modifier.fillMaxWidth(fraction).height(16.dp).clip(BoloShapes.pill).background(if (heard) c.green else c.blue))
        }
        Text(
            if (heard) "✓ We can hear you." else "Waiting for sound… (pick a different mic in Settings if nothing moves)",
            style = Bolo.type.meta, color = if (heard) c.green else c.muted,
        )
    }
}

@Composable
private fun HotkeyStep(hotkeyLabel: String) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Your hotkey", style = Bolo.type.h2, color = Bolo.colors.text)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Kbd(hotkeyLabel); Kbd(hotkeyLabel) }
    Text("Hold $hotkeyLabel to talk, release to paste. Double-tap $hotkeyLabel for hands-free (tap again to stop).", style = Bolo.type.body, color = Bolo.colors.muted)
    Text("Press 1–5 while recording to switch cleanup profile. Esc cancels.", style = Bolo.type.meta, color = Bolo.colors.muted)
}

/** Record a phrase and show the transcript so the user sees the pipeline work end to end (#3). */
@Composable
private fun TryItStep(appState: AppState, recorder: AudioRecorder, mic: String?) {
    val c = Bolo.colors
    val scope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Try it once", style = Bolo.type.h2, color = c.text)
        Text("Record a short sentence and we'll show what Whisper heard.", style = Bolo.type.body, color = c.muted)

        when {
            verifying -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.blue)
                Text("Transcribing…", style = Bolo.type.body, color = c.muted)
            }
            recording -> BoloButton("Stop & transcribe", variant = ButtonVariant.Primary, onClick = {
                val pcm = runCatching { recorder.stop() }.getOrDefault(ByteArray(0))
                recording = false
                verifying = true
                scope.launch {
                    transcript = appState.onboardingTranscribeOp?.invoke(pcm)
                    verifying = false
                }
            })
            else -> BoloButton(if (transcript == null) "Start recording" else "Record again", variant = ButtonVariant.Primary, onClick = {
                transcript = null
                runCatching { recorder.start(mic) { } }
                recording = true
            })
        }

        transcript?.let {
            Column(Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card2).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Heard", style = Bolo.type.meta, color = c.muted)
                Text(it, style = Bolo.type.body, color = c.text)
            }
        }
        if (!verifying && !recording && transcript == null) {
            Text("Models must be installed (previous step) for this to work. You can skip and try later.", style = Bolo.type.meta, color = c.muted)
        }
    }
}

/** Ask to launch Bolo Bhai at Windows login; changeable later in Settings (#14/#3). */
@Composable
private fun StartupStep(launchAtLogin: Boolean, onChange: (Boolean) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Start with Windows", style = Bolo.type.h2, color = Bolo.colors.text)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Launch Bolo Bhai at login", style = Bolo.type.body, color = Bolo.colors.text)
        Switch(checked = launchAtLogin, onCheckedChange = onChange)
    }
    Text("So your hotkey is ready the moment you log in. You can change this later in Settings.", style = Bolo.type.meta, color = Bolo.colors.muted)
}

@Composable
private fun ModelsStep(defaults: List<com.bolobhai.app.ModelUi>, controller: ModelsController, sidecars: SidecarsController) {
    val c = Bolo.colors
    val engineRows by sidecars.rows.collectAsState()

    val modelsReady = defaults.isNotEmpty() && defaults.all { it.state == ModelState.Installed }
    val engineReady = engineRows.all { it.installed } // empty catalog (bundled binaries) counts as ready
    val allReady = modelsReady && engineReady

    val pendingBytes = defaults.filterNot { it.state == ModelState.Installed }.sumOf { it.entry.sizeBytes } +
        engineRows.filterNot { it.installed }.sumOf { it.entry.sizeBytes }
    val anyDownloading = defaults.any { it.downloading } || engineRows.any { it.downloading }
    val freeBytes = remember { runCatching { java.io.File(System.getProperty("user.home")).usableSpace }.getOrDefault(Long.MAX_VALUE) }
    val enoughDisk = freeBytes >= pendingBytes

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Models & engine", style = Bolo.type.h2, color = c.text)

        if (allReady) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("✓", style = Bolo.type.onboardingTitle, color = c.green)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Sab ready", style = Bolo.type.bodyMedium, color = c.text)
                    Text("Models downloaded · engine installed · SHA-256 verified.", style = Bolo.type.meta, color = c.muted)
                }
            }
            return
        }

        Text(
            if (enoughDisk) "Needs ${formatBytes(pendingBytes)} · ${formatBytes(freeBytes)} free."
            else "Not enough disk: needs ${formatBytes(pendingBytes)}, only ${formatBytes(freeBytes)} free.",
            style = Bolo.type.meta,
            color = if (enoughDisk) c.muted else c.orange,
        )

        defaults.forEach { ui ->
            SetupRow(ui.entry.name, ui.state == ModelState.Installed, ui.downloading, ui.progress?.done, ui.progress?.total, ui.progress?.bytesPerSec)
        }
        engineRows.forEach { ui ->
            SetupRow("Engine · ${ui.entry.engine}", ui.installed, ui.downloading, ui.progress?.done, ui.progress?.total, ui.progress?.bytesPerSec)
        }

        if (anyDownloading) {
            BoloButton("Pause", onClick = {
                defaults.filter { it.downloading }.forEach { controller.pause(it.entry) }
                sidecars.pause()
            })
        } else {
            BoloButton("Download ${formatBytes(pendingBytes)}", variant = ButtonVariant.Primary, enabled = enoughDisk, onClick = {
                defaults.filter { it.state != ModelState.Installed }.forEach { controller.download(it.entry) }
                sidecars.installMissing()
            })
        }
    }
}

/** One model/engine line: name on the left, compact status on the right, progress bar while downloading. */
@Composable
private fun SetupRow(name: String, installed: Boolean, downloading: Boolean, done: Long?, total: Long?, bytesPerSec: Long?) =
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = Bolo.type.bodyMedium, color = Bolo.colors.text)
            when {
                installed -> Text("✓ Ready", style = Bolo.type.meta, color = Bolo.colors.green)
                downloading -> Text("Downloading…", style = Bolo.type.meta, color = Bolo.colors.muted)
                else -> Text("Waiting", style = Bolo.type.meta, color = Bolo.colors.muted)
            }
        }
        if (done != null && total != null && bytesPerSec != null) {
            LinearProgressIndicator(progress = { if (total > 0) done.toFloat() / total else 0f }, modifier = Modifier.fillMaxWidth().height(6.dp))
            Text(formatProgress(done, total, bytesPerSec), style = Bolo.type.monoSmall, color = Bolo.colors.muted)
        }
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
