package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.data.Dictation
import com.bolobhai.data.Settings
import com.bolobhai.domain.HistoryFilter
import com.bolobhai.domain.failedCount
import com.bolobhai.domain.filterMatches
import com.bolobhai.domain.formatClock
import com.bolobhai.domain.formatDuration
import com.bolobhai.domain.groupByDay
import com.bolobhai.domain.profileCounts
import com.bolobhai.domain.queryMatches
import com.bolobhai.domain.rawCount
import com.bolobhai.domain.Profile
import com.bolobhai.ui.components.AudioPlayerBar
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.BoloTextField
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Files

/** History home (BoloBhai.md §13.1): search + filter chips over day-grouped dictation cards. */
@Composable
fun HistoryScreen(appState: AppState) {
    val items by appState.history.collectAsState()
    val settings by appState.settings.collectAsState()
    val profiles by appState.profiles.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<HistoryFilter>(HistoryFilter.All) }
    var playingId by remember { mutableStateOf<Long?>(null) }
    // Filter resets to All when a new dictation lands (§13.1).
    LaunchedEffect(items.size) { filter = HistoryFilter.All }

    val filtered = items.filter { queryMatches(it, query) && filterMatches(it, filter) }

    ScreenScaffold(
        title = "History",
        subtitle = "${items.size} dictations",
        trailing = {
            BoloTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search output or original…",
                modifier = Modifier.width(300.dp),
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            FilterChips(items, filter, onSelect = { filter = it })
            when {
                items.isEmpty() -> Empty(
                    "No dictations yet — Hold ${settings.hotkey.windows} anywhere and start speaking…",
                )
                filtered.isEmpty() -> Empty("Nothing matches — Try another word, or pick All above.")
                else -> HistoryList(
                    appState, filtered, settings, profiles, playingId,
                    onTogglePlay = { id -> playingId = if (playingId == id) null else id },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(items: List<Dictation>, selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    val profiles = profileCounts(items)
    val raw = rawCount(items)
    val failed = failedCount(items)
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip("All", items.size, selected == HistoryFilter.All) { onSelect(HistoryFilter.All) }
        profiles.forEach { (name, count) ->
            Chip(name, count, selected == HistoryFilter.ByProfile(name)) { onSelect(HistoryFilter.ByProfile(name)) }
        }
        if (raw > 0) Chip("Raw (no LLM)", raw, selected == HistoryFilter.Raw) { onSelect(HistoryFilter.Raw) }
        if (failed > 0) Chip("Failed", failed, selected == HistoryFilter.Failed, danger = true) { onSelect(HistoryFilter.Failed) }
    }
}

@Composable
private fun Chip(label: String, count: Int, selected: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val c = Bolo.colors
    val accent = if (danger) c.red else c.blue
    val bg = if (selected) accent else c.card2
    val fg = if (selected) c.onAccent else if (danger) c.red else c.text
    Row(
        Modifier.clip(BoloShapes.filterChip).background(bg)
            .border(1.dp, if (selected) accent else c.border, BoloShapes.filterChip)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Bolo.type.button, color = fg)
        Text("$count", style = Bolo.type.monoSmall, color = if (selected) c.onAccent else c.muted)
    }
}

@Composable
private fun HistoryList(
    appState: AppState,
    items: List<Dictation>,
    settings: Settings,
    profiles: List<Profile>,
    playingId: Long?,
    onTogglePlay: (Long) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        groupByDay(items).forEach { (label, dayItems) ->
            item(key = "day-$label") {
                Text(label, style = Bolo.type.sectionLabel, color = Bolo.colors.muted, modifier = Modifier.padding(top = 6.dp))
            }
            items(dayItems, key = { it.id }) {
                HistoryCard(appState, it, settings, profiles, playing = playingId == it.id, onTogglePlay = { onTogglePlay(it.id) })
            }
        }
    }
}

@Composable
private fun HistoryCard(
    appState: AppState,
    item: Dictation,
    settings: Settings,
    profiles: List<Profile>,
    playing: Boolean,
    onTogglePlay: () -> Unit,
) {
    val c = Bolo.colors
    val scope = rememberCoroutineScope()
    val failed = item.status != "ok"
    var showDeleteConfirm by remember(item.id) { mutableStateOf(false) }
    var selectedVersion by remember(item.id, item.currentVersion) { mutableStateOf(item.currentVersion) }
    var expanded by remember(item.id) { mutableStateOf(false) }
    var showOriginal by remember(item.id) { mutableStateOf(false) }
    val version = item.versions.getOrNull(selectedVersion) ?: item.current
    val busyItems by appState.busyItems.collectAsState()
    val busy = item.id in busyItems

    Box(Modifier.fillMaxWidth()) {
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card)
            .border(1.dp, if (failed) c.redBorder else c.border, BoloShapes.card)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Meta row: profile tag · time · duration · version tabs.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileTag(version?.profileName ?: "—", slotOf(settings, version?.profileId))
            Text(formatClock(item.createdAt), style = Bolo.type.meta, color = c.muted)
            Text(formatDuration(item.durationMs), style = Bolo.type.meta, color = c.muted)
            if (item.versions.size > 1) {
                item.versions.forEach { v ->
                    VersionTab("v${v.idx + 1} · ${v.profileName}", v.idx == selectedVersion) { selectedVersion = v.idx }
                }
            }
        }

        // Output (failed cards show the raw transcript in muted).
        Text(
            version?.output.orEmpty(),
            style = Bolo.type.body,
            color = if (failed) c.muted else c.text,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        )

        // While retrying: hide the error + button (only the top-right loader shows). It reappears
        // only if the retry fails again; on success the status flips to "ok" and it stays hidden.
        if (failed && item.status == "llm_failed" && !busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ⓘ Post-processing failed · raw transcript was pasted", style = Bolo.type.meta, color = c.red)
                BoloButton("Retry", variant = ButtonVariant.Danger, onClick = {
                    scope.launch { appState.runRetry(item) }
                })
            }
        }

        if (showOriginal) OriginalPanel(appState, item.rawTranscript)

        if (playing && item.audioPath != null) AudioPlayerBar(item.audioPath!!)

        // Action row.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BoloButton("Copy output", onClick = {
                copyToClipboard(version?.output.orEmpty())
                appState.toast("Copied output")
            })
            BoloButton(if (showOriginal) "Hide original" else "Show original", onClick = { showOriginal = !showOriginal })
            if (item.audioPath != null) {
                BoloButton(if (playing) "Pause" else "Play audio", onClick = onTogglePlay)
            }
            Box(Modifier.weight(1f))
            MoreMenu(appState, item, profiles, scope, onDeleteRequest = { showDeleteConfirm = true })
        }
    }
        if (busy) {
            CircularProgressIndicator(
                Modifier.align(Alignment.TopEnd).padding(12.dp).size(18.dp),
                strokeWidth = 2.dp,
                color = c.blue,
            )
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            text = "Delete this dictation? The output, original transcript, all versions and the audio file will be removed.",
            onConfirm = { appState.deleteDictation(item.id); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun MoreMenu(
    appState: AppState,
    item: Dictation,
    profiles: List<Profile>,
    scope: kotlinx.coroutines.CoroutineScope,
    onDeleteRequest: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var reprocessOpen by remember { mutableStateOf(false) }
    Box {
        BoloButton("⋯ More", variant = ButtonVariant.Ghost, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Re-process with profile ▸") },
                onClick = { reprocessOpen = !reprocessOpen },
            )
            if (reprocessOpen) profiles.forEach { p ->
                DropdownMenuItem(
                    text = { Text("   ${p.name}") },
                    onClick = { open = false; scope.launch { appState.runReprocess(item, p) } },
                )
            }
            DropdownMenuItem(
                text = { Text("Re-transcribe audio") },
                enabled = item.audioPath != null,
                onClick = { open = false; scope.launch { appState.runRetranscribe(item) } },
            )
            DropdownMenuItem(
                text = { Text("Save audio as .wav") },
                enabled = item.audioPath != null,
                onClick = { open = false; saveWav(appState, item) },
            )
            DropdownMenuItem(
                text = { Text("Delete item", color = Bolo.colors.red) },
                onClick = { open = false; onDeleteRequest() },
            )
        }
    }
}

@Composable
private fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete dictation", style = Bolo.type.h2, color = Bolo.colors.text) },
        confirmButton = { BoloButton("Delete", variant = ButtonVariant.Danger, onClick = onConfirm) },
        dismissButton = { BoloButton("Cancel", onClick = onDismiss) },
        text = { Text(text, style = Bolo.type.body, color = Bolo.colors.text) },
        containerColor = Bolo.colors.panel,
    )
}

/** Native save dialog; copies the stored WAV to the chosen location (default `bolobhai-{id}.wav`). */
private fun saveWav(appState: AppState, item: Dictation) {
    val source = item.audioPath ?: return
    val dialog = FileDialog(null as Frame?, "Save audio", FileDialog.SAVE).apply {
        file = "bolobhai-${item.id}.wav"
        isVisible = true
    }
    val dir = dialog.directory ?: return
    val name = dialog.file ?: return
    Files.copy(File(source).toPath(), File(dir, name).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    appState.toast("Saved $name")
}

@Composable
private fun OriginalPanel(appState: AppState, raw: String) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card2)
            .border(1.dp, c.border, BoloShapes.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ORIGINAL · WHISPER", style = Bolo.type.sectionLabel, color = c.muted)
            BoloButton("Copy", variant = ButtonVariant.Ghost, onClick = {
                copyToClipboard(raw)
                appState.toast("Copied original transcript")
            })
        }
        Text(raw, style = Bolo.type.body, color = c.muted)
    }
}

@Composable
private fun ProfileTag(name: String, slot: Int?) {
    val c = Bolo.colors
    Row(
        Modifier.clip(BoloShapes.filterChip).background(c.blueSoft).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (slot != null) Text("$slot", style = Bolo.type.monoSmall, color = c.blue)
        Text(name, style = Bolo.type.meta, color = c.blue)
    }
}

@Composable
private fun VersionTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Bolo.colors
    Box(
        Modifier.clip(BoloShapes.button)
            .background(if (selected) c.card2 else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) c.border else androidx.compose.ui.graphics.Color.Transparent, BoloShapes.button)
            .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, style = Bolo.type.meta, color = if (selected) c.text else c.muted)
    }
}

@Composable
private fun Empty(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = Bolo.type.body, color = Bolo.colors.muted, modifier = Modifier.padding(32.dp))
    }
}

/** Slot number holding this profile file, or null if unassigned. */
private fun slotOf(settings: Settings, profileFile: String?): Int? {
    if (profileFile == null) return null
    return (1..5).firstOrNull { settings.quickSlots[it.toString()] == profileFile }
}

private fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
