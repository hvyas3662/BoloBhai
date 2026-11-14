package com.bolobhai.ui.pill

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.bolobhai.domain.formatTimer
import com.bolobhai.ui.components.BoloIcons
import com.bolobhai.ui.components.SlotKey
import com.bolobhai.ui.components.SlotKeyVisual
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/**
 * The floating recording pill (BoloBhai.md §8). Pure visual: actions are passed in and the pill
 * never owns recording/pipeline state.
 */
@Composable
fun RecordingPill(
    state: PillUiState,
    modifier: Modifier = Modifier,
    onStop: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val c = Bolo.colors
    // Fade + slide-in over 180 ms when the pill appears (BoloBhai.md §8).
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(180)) }
    Row(
        modifier = modifier
            .graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 12.dp.toPx() }
            .height(48.dp)
            .widthIn(max = 460.dp)
            .shadow(16.dp, BoloShapes.pill)
            .clip(BoloShapes.pill)
            .background(c.panel)
            .padding(PaddingValues(start = 16.dp, end = 7.dp)),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state.phase) {
            PillPhase.Hold, PillPhase.HandsFree -> RecordingContent(state, onStop, onCancel)
            PillPhase.Processing -> ProcessingContent(state)
            PillPhase.Pasted -> PastedContent(state)
            PillPhase.Failed -> FailedContent(state, onRetry)
        }
    }
}

@Composable
private fun RecordingContent(state: PillUiState, onStop: () -> Unit, onCancel: () -> Unit) {
    val c = Bolo.colors
    RecDot()
    Text(formatTimer(state.elapsedMs), style = Bolo.type.mono, color = c.text, modifier = Modifier.widthIn(min = 36.dp))
    Waveform(state.levels)
    Text(
        if (state.phase == PillPhase.HandsFree) "Hands-free" else "Release ${state.hotkeyLabel}",
        style = Bolo.type.monoSmall,
        color = c.muted,
    )
    // Profile switcher only in hands-free (you can't press 1–5 while holding the key).
    if (state.phase == PillPhase.HandsFree) SlotSection(state)
    ProfileName(state.profileShort)
    if (state.phase == PillPhase.HandsFree) {
        CircleButton(c.blue, c.onAccent, BoloIcons.Stop, "Stop and process", onStop)
    }
    CircleButton(c.card2, c.muted, BoloIcons.Close, "Cancel recording (Esc)", onCancel)
}

@Composable
private fun ProcessingContent(state: PillUiState) {
    val c = Bolo.colors
    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = c.blue, strokeWidth = 2.dp)
    Text("Processing", style = Bolo.type.monoSmall, color = c.muted)
    ProfileName(state.profileShort)
}

@Composable
private fun PastedContent(state: PillUiState) {
    val c = Bolo.colors
    Box(Modifier.size(22.dp).clip(CircleShape).background(c.green.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Text("✓", style = Bolo.type.monoSemibold, color = c.green)
    }
    Text("Pasted", style = Bolo.type.monoSmall, color = c.text)
    Text(state.pasteShortcut, style = Bolo.type.monoSmall, color = c.muted)
    ProfileName(state.profileShort)
}

@Composable
private fun FailedContent(state: PillUiState, onRetry: () -> Unit) {
    val c = Bolo.colors
    Text("ⓘ", style = Bolo.type.monoSemibold, color = c.red)
    Text(state.failedMessage ?: "LLM timeout · raw text pasted", style = Bolo.type.monoSmall, color = c.red)
    Box(
        Modifier.clip(BoloShapes.pill).background(c.red).clickable { onRetry() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text("Retry", style = Bolo.type.monoSemibold, color = c.onRed)
    }
}

@Composable
private fun RecDot() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
    )
    Box(
        Modifier.size(8.dp).clip(CircleShape).graphicsLayer { this.alpha = alpha }
            .background(Bolo.colors.rec),
    )
}

@Composable
private fun Waveform(levels: List<Float>) {
    val color = Bolo.colors.blue
    Canvas(Modifier.size(52.dp, 22.dp)) {
        val bars = levels.size.coerceAtLeast(1)
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(1f)
        levels.forEachIndexed { i, level ->
            // Boost intensity: perceptual sqrt curve + gain so quiet speech still moves the bars.
            val boosted = (kotlin.math.sqrt(level.coerceIn(0f, 1f)) * 1.4f).coerceIn(0f, 1f)
            val h = (boosted * size.height).coerceAtLeast(2f)
            val x = i * (barWidth + gap)
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.size(1.dp, 22.dp).background(Bolo.colors.border))
}

/** Divider + the assigned slot keys; renders nothing when no slot holds a profile. */
@Composable
private fun SlotSection(state: PillUiState) {
    if (state.slots.none { it != SlotKeyVisual.Empty }) return
    Divider()
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        state.slots.forEachIndexed { i, visual ->
            if (visual != SlotKeyVisual.Empty) SlotKey(number = i + 1, visual = visual, sizeDp = 24.dp)
        }
    }
}

@Composable
private fun ProfileName(name: String) {
    Text(name, style = Bolo.type.monoSemibold, color = Bolo.colors.text, modifier = Modifier.widthIn(min = 58.dp))
}

@Composable
private fun CircleButton(bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = fg, modifier = Modifier.size(16.dp))
    }
}
