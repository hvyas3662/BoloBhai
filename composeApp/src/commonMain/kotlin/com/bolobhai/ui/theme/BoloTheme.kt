package com.bolobhai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    // Feed our tokens into Material3 so OutlinedTextField / AlertDialog / DropdownMenu / Switch
    // pick up the right text and surface colors instead of the default light scheme.
    val base = if (resolveDark(mode, systemDark)) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.blue,
        onPrimary = colors.onAccent,
        secondary = colors.orange,
        onSecondary = colors.onAccent,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.card,
        onSurface = colors.text,
        surfaceVariant = colors.card2,
        onSurfaceVariant = colors.muted,
        outline = colors.border,
        outlineVariant = colors.border,
        error = colors.red,
        onError = colors.onRed,
    )
    CompositionLocalProvider(
        LocalBoloColors provides colors,
        LocalBoloTypography provides typography,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
