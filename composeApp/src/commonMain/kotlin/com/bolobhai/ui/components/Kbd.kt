package com.bolobhai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/** Keycap label (BoloBhai.md §12.3): mono 12, padding 3/8, card2 + border. */
@Composable
fun Kbd(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Bolo.type.monoSemibold,
        color = Bolo.colors.muted,
        modifier = modifier
            .clip(BoloShapes.kbd)
            .background(Bolo.colors.card2)
            .border(1.dp, Bolo.colors.border, BoloShapes.kbd)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
