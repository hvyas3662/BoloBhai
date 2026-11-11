package com.bolobhai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalBoloColors = staticCompositionLocalOf<BoloColors> { error("BoloColors not provided") }
val LocalBoloTypography = staticCompositionLocalOf<BoloTypography> { error("BoloTypography not provided") }

/** Access the current theme's tokens: `Bolo.colors.bg`, `Bolo.type.h1`. */
object Bolo {
    val colors: BoloColors
        @Composable @ReadOnlyComposable get() = LocalBoloColors.current
    val type: BoloTypography
        @Composable @ReadOnlyComposable get() = LocalBoloTypography.current
}

@Composable
fun BoloTheme(
    mode: ThemeMode,
    systemDark: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = boloColors(mode, systemDark)
    val typography = boloTypography()
    CompositionLocalProvider(
        LocalBoloColors provides colors,
        LocalBoloTypography provides typography,
    ) {
        MaterialTheme(content = content)
    }
}
