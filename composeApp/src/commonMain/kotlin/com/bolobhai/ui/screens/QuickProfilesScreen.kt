package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.domain.Profile
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.components.Kbd
import com.bolobhai.ui.components.SlotKey
import com.bolobhai.ui.components.slotKeyVisual
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

@Composable
fun QuickProfilesScreen(appState: AppState) {
    val settings by appState.settings.collectAsState()
    val activeSlot by appState.activeSlot.collectAsState()
    val profiles by appState.profiles.collectAsState()

    var selectedSlot by remember { mutableStateOf(activeSlot) }

    ScreenScaffold(
        title = "Quick Profiles",
        subtitle = "Five post-processing profiles on number keys. Switch in one tap, even mid-recording.",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("While recording, press ", style = Bolo.type.meta, color = Bolo.colors.muted)
                Kbd("1–5")
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                for (slot in 1..5) {
                    SlotCard(
                        slot = slot,
                        fileName = settings.quickSlots[slot.toString()],
                        activeSlot = activeSlot,
                        selected = slot == selectedSlot,
                        profiles = profiles,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedSlot = slot },
                    )
                }
            }
            SlotEditor(appState, selectedSlot, settings.quickSlots[selectedSlot.toString()], profiles)
        }
    }
}

@Composable
private fun SlotCard(
    slot: Int,
    fileName: String?,
    activeSlot: Int,
    selected: Boolean,
    profiles: List<Profile>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = Bolo.colors
    val name = fileName?.let { f -> profiles.firstOrNull { it.fileName == f }?.name } ?: "Empty"
    val badge = when {
        fileName == null -> "Empty"
        slot == activeSlot -> "Active"
        slot == 1 -> "Default"
        else -> "Ready"
    }
    Column(
        modifier = modifier
            .heightIn(min = 150.dp)
            .clip(BoloShapes.card)
            .background(if (selected) c.orangeSoft else c.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.orange else c.border, BoloShapes.card)
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SlotKey(slot, slotKeyVisual(slot, activeSlot, fileName != null), sizeDp = 40.dp)
        Text(badge.uppercase(), style = Bolo.type.sectionLabel, color = c.muted)
        Text(name, style = Bolo.type.h2, color = c.text)
        Text(fileName ?: "—", style = Bolo.type.monoSmall, color = c.muted)
    }
}

@Composable
private fun SlotEditor(appState: AppState, slot: Int, fileName: String?, profiles: List<Profile>) {
    val c = Bolo.colors
    val profile = fileName?.let { f -> profiles.firstOrNull { it.fileName == f } }

    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card)
            .border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("EDITING SLOT $slot", style = Bolo.type.sectionLabel, color = c.orange)

        ProfilePicker(
            current = fileName,
            profiles = profiles,
            onPick = { appState.assignSlot(slot, it) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BoloButton("Use now", variant = ButtonVariant.Orange, onClick = { appState.setActiveSlot(slot) })
            BoloButton("Clear slot", onClick = { appState.assignSlot(slot, null) })
            BoloButton("New profile", onClick = {
                val fresh = Profile("new-profile.md", "New Profile", "Return only the final text.")
                appState.saveProfile(fresh)
                appState.assignSlot(slot, fresh.fileName)
            })
        }

        if (profile != null) {
            Text("Instructions for the LLM", style = Bolo.type.sectionLabel, color = c.muted)
            var body by remember(profile.fileName) { mutableStateOf(profile.body) }
            LaunchedEffect(profile.fileName) { body = profile.body }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                textStyle = Bolo.type.mono.copy(fontFamily = FontFamily.Monospace, color = c.text),
            )
            BoloButton("Save ${profile.fileName}", variant = ButtonVariant.Primary, onClick = {
                appState.saveProfile(profile.copy(body = body))
            })
        } else {
            Box(
                Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card2).padding(20.dp),
            ) {
                Text("Slot $slot khaali hai — pick a profile above, or create a new one.", style = Bolo.type.body, color = c.muted)
            }
        }
    }
}

@Composable
private fun ProfilePicker(current: String?, profiles: List<Profile>, onPick: (String?) -> Unit) {
    val c = Bolo.colors
    var open by remember { mutableStateOf(false) }
    val label = current?.let { f -> profiles.firstOrNull { it.fileName == f }?.let { "${it.name} · ${it.fileName}" } } ?: "— Empty —"

    Box {
        Row(
            Modifier.clip(BoloShapes.input).background(c.input).border(1.dp, c.border, BoloShapes.input)
                .clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(label, style = Bolo.type.body, color = c.text)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("— Empty —") }, onClick = { onPick(null); open = false })
            profiles.forEach { p ->
                DropdownMenuItem(
                    text = { Text("${p.name} · ${p.fileName}") },
                    onClick = { onPick(p.fileName); open = false },
                )
            }
        }
    }
}
