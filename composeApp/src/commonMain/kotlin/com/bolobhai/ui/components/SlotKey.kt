package com.bolobhai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

enum class SlotKeyVisual { Active, Assigned, Empty }

/** Which visual a 1–5 slot key shows (BoloBhai.md §8). */
fun slotKeyVisual(slot: Int, activeSlot: Int, assigned: Boolean): SlotKeyVisual = when {
    !assigned -> SlotKeyVisual.Empty
    slot == activeSlot -> SlotKeyVisual.Active
    else -> SlotKeyVisual.Assigned
}

/**
 * A Quick Profile slot key (BoloBhai.md §8): Active = orange fill + onAccent; Assigned = card2 +
 * border; Empty = transparent + dashed border, muted, disabled.
 */
@Composable
fun SlotKey(
    number: Int,
    visual: SlotKeyVisual,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
) {
    val c = Bolo.colors
    val shape = BoloShapes.slotChip

    var box = modifier.size(sizeDp).clip(shape)
    box = when (visual) {
        SlotKeyVisual.Active -> box.background(c.orange)
        SlotKeyVisual.Assigned -> box.background(c.card2).border(1.dp, c.border, shape)
        SlotKeyVisual.Empty -> box.drawBehind {
            drawRoundRect(
                color = c.border,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                ),
            )
        }
    }
    if (onClick != null && visual != SlotKeyVisual.Empty) {
        box = box.clickable { onClick() }
    }

    val textColor = when (visual) {
        SlotKeyVisual.Active -> c.onAccent
        SlotKeyVisual.Assigned -> c.text
        SlotKeyVisual.Empty -> c.muted
    }

    Box(box, contentAlignment = Alignment.Center) {
        Text("$number", style = Bolo.type.monoSemibold, color = textColor)
    }
}
