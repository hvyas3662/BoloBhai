package com.bolobhai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.bolobhai.ui.theme.Bolo
import com.bolobhai.ui.theme.BoloShapes

/**
 * Compact themed text field (BoloBhai.md §12): rounded (radius 10), short height, our input bg +
 * border + text colors. Replaces Material3's tall OutlinedTextField.
 */
@Composable
fun BoloTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    textStyle: TextStyle = Bolo.type.body,
) {
    val c = Bolo.colors
    Box(
        modifier
            .clip(BoloShapes.input)
            .background(c.input)
            .border(1.dp, c.border, BoloShapes.input)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = c.text),
            cursorBrush = SolidColor(c.blue),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder != null) {
                    androidx.compose.material3.Text(placeholder, style = textStyle, color = c.muted)
                }
                inner()
            },
        )
    }
}
