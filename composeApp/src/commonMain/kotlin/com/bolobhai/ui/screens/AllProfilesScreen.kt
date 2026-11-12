package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.domain.Profile
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes
import kotlinx.coroutines.launch

/** All Profiles (BoloBhai.md §13.3): list every .md with a right-hand editor. */
@Composable
fun AllProfilesScreen(appState: AppState) {
    val profiles by appState.profiles.collectAsState()
    val settings by appState.settings.collectAsState()
    val history by appState.history.collectAsState()

    var selected by remember(profiles) { mutableStateOf(profiles.firstOrNull()?.fileName) }
    val profile = profiles.firstOrNull { it.fileName == selected }

    fun slotOf(file: String) = (1..5).firstOrNull { settings.quickSlots[it.toString()] == file }
    fun uses(file: String) = history.sumOf { d -> d.versions.count { it.profileId == file } }

    ScreenScaffold(
        title = "All Profiles",
        subtitle = "Every .md file in ~/.bolobhai/profiles/",
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BoloButton("Open profiles folder", onClick = { appState.openProfilesFolder?.invoke() })
                BoloButton("New profile", variant = ButtonVariant.Primary, onClick = {
                    val fresh = Profile("new-profile.md", "New Profile", "Return only the cleaned-up text, nothing else.")
                    appState.saveProfile(fresh)
                    selected = fresh.fileName
                    appState.toast("Created new-profile.md")
                })
            }
        },
    ) {
        Row(Modifier.fillMaxSize()) {
            // Left list.
            Column(
                Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                profiles.forEach { p ->
                    ProfileCard(p, slotOf(p.fileName), uses(p.fileName), p.fileName == selected) { selected = p.fileName }
                }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Bolo.colors.border))
            // Right editor.
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                if (profile != null) Editor(appState, profile, slotOf(profile.fileName), profiles.size)
            }
        }
    }
}

@Composable
private fun ProfileCard(p: Profile, slot: Int?, uses: Int, selected: Boolean, onClick: () -> Unit) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card)
            .background(if (selected) c.blueSoft else c.card)
            .border(1.dp, if (selected) c.blue else c.border, BoloShapes.card)
            .clickable { onClick() }.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(slot?.toString() ?: "–", style = Bolo.type.monoSemibold, color = if (slot != null) c.orange else c.muted)
            Text(p.name, style = Bolo.type.bodyMedium, color = c.text)
        }
        Text(p.fileName, style = Bolo.type.monoSmall, color = c.muted)
        Text("$uses uses", style = Bolo.type.meta, color = c.muted)
    }
}

@Composable
private fun Editor(appState: AppState, profile: Profile, slot: Int?, profileCount: Int) {
    val c = Bolo.colors
    val scope = rememberCoroutineScope()
    var name by remember(profile.fileName) { mutableStateOf(profile.name) }
    var body by remember(profile.fileName) { mutableStateOf(profile.body) }
    var preview by remember(profile.fileName) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(profile.fileName) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Name", style = Bolo.type.sectionLabel, color = c.muted)
        OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("File", style = Bolo.type.sectionLabel, color = c.muted)
        Text(profile.fileName, style = Bolo.type.mono, color = c.muted)

        Text("Quick slot", style = Bolo.type.sectionLabel, color = c.muted)
        SlotPicker(slot) { picked ->
            if (picked == null) { if (slot != null) appState.assignSlot(slot, null) }
            else appState.assignSlot(picked, profile.fileName)
        }

        Text("System prompt (Markdown, optional YAML frontmatter)", style = Bolo.type.sectionLabel, color = c.muted)
        OutlinedTextField(
            value = body, onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
            textStyle = Bolo.type.mono.copy(fontFamily = FontFamily.Monospace, color = c.text),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BoloButton("Save", variant = ButtonVariant.Primary, onClick = {
                appState.saveProfile(profile.copy(body = body))
                if (name.trim() != profile.name && name.isNotBlank()) appState.renameProfile(profile.fileName, name.trim())
                appState.toast("Saved ${profile.fileName}")
            })
            BoloButton("Test on last dictation", onClick = {
                scope.launch { preview = appState.testProfileOp?.invoke(profile.copy(body = body)) ?: "No dictation yet" }
            })
            BoloButton("Delete profile", variant = ButtonVariant.Danger, enabled = profileCount > 1, onClick = { confirmDelete = true })
        }

        preview?.let { p ->
            Text("PREVIEW", style = Bolo.type.sectionLabel, color = c.muted)
            Box(Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.blueSoft).padding(14.dp)) {
                Text(p, style = Bolo.type.body, color = c.text)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete profile", style = Bolo.type.h2, color = c.text) },
            confirmButton = { BoloButton("Delete", variant = ButtonVariant.Danger, onClick = { appState.deleteProfile(profile.fileName); confirmDelete = false }) },
            dismissButton = { BoloButton("Cancel", onClick = { confirmDelete = false }) },
            text = { Text("Delete “${profile.name}”? The .md file goes to the trash.", style = Bolo.type.body, color = c.text) },
            containerColor = c.panel,
        )
    }
}

@Composable
private fun SlotPicker(current: Int?, onPick: (Int?) -> Unit) {
    val c = Bolo.colors
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(BoloShapes.input).background(c.input).border(1.dp, c.border, BoloShapes.input)
                .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text(current?.let { "Slot $it" } ?: "Not on a key", style = Bolo.type.body, color = c.text) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Not on a key") }, onClick = { onPick(null); open = false })
            (1..5).forEach { n -> DropdownMenuItem(text = { Text("Slot $n") }, onClick = { onPick(n); open = false }) }
        }
    }
}
