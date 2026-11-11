package com.bolobhai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

enum class ButtonVariant { Default, Primary, Orange, Danger, Ghost }

enum class ButtonSize(val height: Dp) { Normal(38.dp), Large(44.dp) }

private data class ButtonStyle(val bg: Color, val fg: Color, val border: Color?)

@Composable
private fun styleFor(variant: ButtonVariant): ButtonStyle {
    val c = Bolo.colors
    return when (variant) {
        ButtonVariant.Default -> ButtonStyle(c.card2, c.text, c.border)
        ButtonVariant.Primary -> ButtonStyle(c.blue, c.onAccent, null)
        ButtonVariant.Orange -> ButtonStyle(c.orange, c.onAccent, null)
        ButtonVariant.Danger -> ButtonStyle(c.red, c.onRed, null)
        ButtonVariant.Ghost -> ButtonStyle(Color.Transparent, c.text, null)
    }
}

@Composable
fun BoloButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Normal,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val style = styleFor(variant)
    val shape: RoundedCornerShape = BoloShapes.button
    var m = modifier
        .height(size.height)
        .clip(shape)
        .background(style.bg)
    if (style.border != null) m = m.border(BorderStroke(1.dp, style.border), shape)
    m = m.clickable(enabled = enabled) { onClick() }.padding(horizontal = 14.dp)

    Row(
        modifier = m,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = style.fg, modifier = Modifier.size(16.dp))
        }
        Text(text, style = Bolo.type.button, color = style.fg)
    }
}
