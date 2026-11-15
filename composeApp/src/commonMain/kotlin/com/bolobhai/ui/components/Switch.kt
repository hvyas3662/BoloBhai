package com.bolobhai.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import com.bolobhai.ui.theme.Bolo

/**
 * Themed switch. Material3's default switch colors wash out on our light theme (grey-on-white with
 * low contrast); this maps every slot to our tokens so it reads clearly in both themes.
 */
@Composable
fun BoloSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = Bolo.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onAccent,
            checkedTrackColor = c.blue,
            checkedBorderColor = c.blue,
            uncheckedThumbColor = c.muted,
            uncheckedTrackColor = c.card2,
            uncheckedBorderColor = c.border,
        ),
    )
}
