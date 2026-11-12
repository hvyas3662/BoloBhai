package com.bolobhai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.app.Screen
import com.bolobhai.domain.profileDisplayName
import com.bolobhai.resources.Res
import com.bolobhai.resources.app_icon
import com.bolobhai.ui.components.BoloIcons
import com.bolobhai.ui.components.Kbd
import com.bolobhai.ui.components.NavRow
import com.bolobhai.ui.components.SlotKey
import com.bolobhai.ui.components.slotKeyVisual
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes
import com.bolobhai.ui.theme.ThemeMode
import com.bolobhai.ui.theme.resolveDark
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.collectAsState

private data class NavItem(val screen: Screen, val icon: androidx.compose.ui.graphics.vector.ImageVector, val counter: String?)

@Composable
fun Sidebar(appState: AppState, hotkeyLabel: String, statusLine: String, ready: Boolean, modifier: Modifier = Modifier) {
    val c = Bolo.colors
    val currentScreen by appState.currentScreen.collectAsState()
    val theme by appState.theme.collectAsState()
    val settings by appState.settings.collectAsState()
    val activeSlot by appState.activeSlot.collectAsState()

    val isDark = resolveDark(theme, appState.systemDark)

    Column(
        modifier = modifier
            .width(264.dp)
            .fillMaxHeight()
            .background(c.side)
            .padding(16.dp),
    ) {
        // 1. Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "Bolo Bhai",
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text("Bolo Bhai", style = Bolo.type.h2, color = c.text)
                Text("baaki hum likh denge", style = Bolo.type.meta, color = c.muted)
            }
        }

        Spacer(Modifier.height(20.dp))

        // 2. Record button (visual-only in M1)
        RecordButton(hotkeyLabel)

        Spacer(Modifier.height(20.dp))

        // 3. Nav
        val items = listOf(
            NavItem(Screen.History, BoloIcons.History, null),
            NavItem(Screen.QuickProfiles, BoloIcons.QuickProfiles, "1–5"),
            NavItem(Screen.AllProfiles, BoloIcons.AllProfiles, null),
            NavItem(Screen.Dictionary, BoloIcons.Dictionary, null),
            NavItem(Screen.Models, BoloIcons.Models, null),
            NavItem(Screen.Settings, BoloIcons.Settings, null),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEach { item ->
                NavRow(
                    icon = item.icon,
                    label = item.screen.title,
                    selected = currentScreen == item.screen,
                    onClick = { appState.navigateTo(item.screen) },
                    counter = item.counter,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // 4. Active profile card
        Column(
            Modifier.fillMaxWidth()
                .clip(BoloShapes.card)
                .background(c.card2)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("ACTIVE PROFILE", style = Bolo.type.sectionLabel, color = c.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                for (slot in 1..5) {
                    val assigned = settings.quickSlots[slot.toString()] != null
                    SlotKey(
                        number = slot,
                        visual = slotKeyVisual(slot, activeSlot, assigned),
                        sizeDp = 32.dp,
                        onClick = { appState.setActiveSlot(slot) },
                    )
                }
            }
            Text(
                "$activeSlot · ${profileDisplayName(settings.quickSlots[activeSlot.toString()])}",
                style = Bolo.type.bodyMedium,
                color = c.text,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 5. Status line
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (ready) c.green else c.orange))
            Text(
                statusLine,
                style = Bolo.type.meta,
                color = c.muted,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Icon(
                imageVector = if (isDark) BoloIcons.Sun else BoloIcons.Moon,
                contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                tint = c.muted,
                modifier = Modifier
                    .size(44.dp)
                    .clip(BoloShapes.button)
                    .clickable { appState.setTheme(if (isDark) ThemeMode.Light else ThemeMode.Dark) }
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun RecordButton(hotkeyLabel: String) {
    val c = Bolo.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(BoloShapes.button)
                .background(c.blue)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hold to talk", style = Bolo.type.bodyMedium, color = c.onAccent)
            Spacer(Modifier.width(8.dp))
            Kbd(hotkeyLabel)
        }
        Text(
            "Double-click for hands-free · like $hotkeyLabel",
            style = Bolo.type.meta,
            color = c.muted,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
