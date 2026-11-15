package com.bolobhai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.log.Logger
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * Logs panel (BoloBhai.md §16): shows app + sidecar output, with Copy / Save report and Clear.
 * [onToast] surfaces a confirmation (optional).
 */
@Composable
fun LogsPanel(onToast: (String) -> Unit = {}) {
    val c = Bolo.colors
    val entries by Logger.entries.collectAsState()

    Column(
        Modifier.fillMaxSize().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Logs", style = Bolo.type.h2, color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoloButton("Copy report", onClick = {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(Logger.report()), null)
                    onToast("Log report copied")
                })
                BoloButton("Save report…", onClick = { if (saveReport()) onToast("Log saved") })
                BoloButton("Clear logs", variant = ButtonVariant.Danger, onClick = { Logger.clear(); onToast("Logs cleared") })
            }
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(BoloShapes.input).background(c.input)
                .border(1.dp, c.border, BoloShapes.input).padding(12.dp).verticalScroll(rememberScrollState()),
        ) {
            if (entries.isEmpty()) {
                Text("No logs yet.", style = Bolo.type.monoSmall, color = c.muted)
            } else {
                Column {
                    entries.forEach { e ->
                        val color = when (e.level) {
                            Logger.Level.ERROR -> c.red
                            Logger.Level.WARN -> c.orange
                            Logger.Level.INFO -> c.muted
                        }
                        Text(line(e), style = Bolo.type.monoSmall, color = color)
                    }
                }
            }
        }
    }
}

private fun line(e: Logger.Entry): String {
    val t = java.time.Instant.ofEpochMilli(e.ts).atZone(java.time.ZoneId.systemDefault())
    val hhmmss = "%02d:%02d:%02d".format(t.hour, t.minute, t.second)
    return "$hhmmss ${e.level.name.padEnd(5)} ${e.msg}"
}

/** Native save dialog -> writes the full log report. Returns true if saved. */
private fun saveReport(): Boolean {
    val dialog = FileDialog(null as Frame?, "Save log report", FileDialog.SAVE).apply {
        file = "bolobhai-log.txt"
        isVisible = true
    }
    val dir = dialog.directory ?: return false
    val name = dialog.file ?: return false
    return runCatching { File(dir, name).writeText(Logger.report()); true }.getOrDefault(false)
}
