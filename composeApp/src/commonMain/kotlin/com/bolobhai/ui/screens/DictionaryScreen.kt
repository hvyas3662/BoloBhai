package com.bolobhai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bolobhai.app.AppState
import com.bolobhai.data.Correction
import com.bolobhai.data.Dictionary
import com.bolobhai.domain.applyCorrections
import com.bolobhai.ui.components.BoloButton
import com.bolobhai.ui.components.BoloTextField
import com.bolobhai.ui.components.ButtonVariant
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/** Dictionary screen (BoloBhai.md §13.4): custom words, corrections and a live "Try it" preview. */
@Composable
fun DictionaryScreen(appState: AppState) {
    val dict by appState.dictionary.collectAsState()

    ScreenScaffold(
        title = "Dictionary",
        subtitle = "Custom words and corrections, applied before the LLM.",
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CustomWordsCard(dict) { appState.saveDictionary(it) }
            CorrectionsCard(dict) { appState.saveDictionary(it) }
            TryItCard(dict)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomWordsCard(dict: Dictionary, onChange: (Dictionary) -> Unit) {
    val c = Bolo.colors
    var input by remember { mutableStateOf("") }
    Card("Custom words", "Passed to Whisper as its initial prompt.") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dict.customWords.forEach { word ->
                Chip(word) { onChange(dict.copy(customWords = dict.customWords - word)) }
            }
        }
        val add = {
            val w = input.trim()
            if (w.isNotEmpty() && w !in dict.customWords) onChange(dict.copy(customWords = dict.customWords + w))
            input = ""
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BoloTextField(input, { input = it }, placeholder = "e.g. Kubernetes", modifier = Modifier.width(260.dp))
            BoloButton("Add", variant = ButtonVariant.Primary, onClick = { add() })
        }
    }
}

@Composable
private fun CorrectionsCard(dict: Dictionary, onChange: (Dictionary) -> Unit) {
    val c = Bolo.colors
    var wrong by remember { mutableStateOf("") }
    var right by remember { mutableStateOf("") }
    Card("Corrections", "Applied after transcription, before the LLM.") {
        dict.corrections.forEach { corr ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(corr.wrong, style = Bolo.type.body, color = c.muted, textDecoration = TextDecoration.LineThrough)
                Text("→", style = Bolo.type.body, color = c.muted)
                Text(corr.right, style = Bolo.type.bodyMedium, color = c.text)
                Box(Modifier.weight(1f))
                BoloButton("✕", variant = ButtonVariant.Ghost, onClick = { onChange(dict.copy(corrections = dict.corrections - corr)) })
            }
        }
        val add = {
            val w = wrong.trim(); val r = right.trim()
            if (w.isNotEmpty() && r.isNotEmpty()) onChange(dict.copy(corrections = dict.corrections + Correction(w, r)))
            wrong = ""; right = ""
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BoloTextField(wrong, { wrong = it }, placeholder = "Wrong (heard)", modifier = Modifier.width(200.dp))
            Text("→", style = Bolo.type.body, color = c.muted)
            BoloTextField(right, { right = it }, placeholder = "Right", modifier = Modifier.width(200.dp))
            BoloButton("Add", variant = ButtonVariant.Primary, onClick = { add() })
        }
    }
}

@Composable
private fun TryItCard(dict: Dictionary) {
    val c = Bolo.colors
    var input by remember { mutableStateOf("") }
    Card("Try it", "See how corrections rewrite a transcript.") {
        BoloTextField(input, { input = it }, placeholder = "Type a raw transcript…", singleLine = false, modifier = Modifier.fillMaxWidth())
        Text("AFTER DICTIONARY", style = Bolo.type.sectionLabel, color = c.muted)
        Box(Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.blueSoft).padding(14.dp)) {
            Text(applyCorrections(input, dict).ifBlank { "—" }, style = Bolo.type.body, color = c.text)
        }
    }
}

@Composable
private fun Card(title: String, subtitle: String, body: @Composable () -> Unit) {
    val c = Bolo.colors
    Column(
        Modifier.fillMaxWidth().clip(BoloShapes.card).background(c.card).border(1.dp, c.border, BoloShapes.card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = Bolo.type.h2, color = c.text)
        Text(subtitle, style = Bolo.type.meta, color = c.muted)
        body()
    }
}

@Composable
private fun Chip(label: String, onRemove: () -> Unit) {
    val c = Bolo.colors
    Row(
        Modifier.clip(BoloShapes.filterChip).background(c.card2).border(1.dp, c.border, BoloShapes.filterChip)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Bolo.type.body, color = c.text)
        Text("✕", style = Bolo.type.meta, color = c.muted, modifier = Modifier.clickable { onRemove() })
    }
}
