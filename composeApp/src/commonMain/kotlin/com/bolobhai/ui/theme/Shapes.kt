package com.bolobhai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radii from BoloBhai.md §12.3. */
object BoloRadii {
    val card = 14.dp
    val panel = 16.dp
    val panelLarge = 20.dp
    val button = 10.dp
    val input = 6.dp
    val slotChip = 10.dp
    val pill = 999.dp
    val filterChip = 999.dp
    val key = 7.dp
    val kbd = 6.dp
}

object BoloShapes {
    val card = RoundedCornerShape(BoloRadii.card)
    val panel = RoundedCornerShape(BoloRadii.panel)
    val panelLarge = RoundedCornerShape(BoloRadii.panelLarge)
    val button = RoundedCornerShape(BoloRadii.button)
    val input = RoundedCornerShape(BoloRadii.input)
    val slotChip = RoundedCornerShape(BoloRadii.slotChip)
    val pill = RoundedCornerShape(BoloRadii.pill)
    val filterChip = RoundedCornerShape(BoloRadii.filterChip)
    val key = RoundedCornerShape(BoloRadii.key)
    val kbd = RoundedCornerShape(BoloRadii.kbd)
}
