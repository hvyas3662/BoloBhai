package com.bolobhai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/** Sidebar nav row (BoloBhai.md §13.0): 44px, icon + label + mono counter; selected = blueSoft/blue. */
@Composable
fun NavRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    counter: String? = null,
) {
    val c = Bolo.colors
    val fg = if (selected) c.blue else c.text
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(BoloShapes.button)
            .background(if (selected) c.blueSoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = Bolo.type.bodyMedium,
            color = fg,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        if (counter != null) {
            Text(counter, style = Bolo.type.monoSmall, color = c.muted)
        }
    }
}
