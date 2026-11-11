package com.bolobhai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bolobhai.resources.Res
import com.bolobhai.resources.inter_bold
import com.bolobhai.resources.inter_medium
import com.bolobhai.resources.inter_regular
import com.bolobhai.resources.inter_semibold
import com.bolobhai.resources.jetbrains_mono_medium
import com.bolobhai.resources.jetbrains_mono_semibold
import com.bolobhai.resources.space_grotesk_bold
import com.bolobhai.resources.space_grotesk_medium
import com.bolobhai.resources.space_grotesk_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun interFamily() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

@Composable
fun spaceGroteskFamily() = FontFamily(
    Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(Res.font.space_grotesk_bold, FontWeight.Bold),
)

@Composable
fun jetBrainsMonoFamily() = FontFamily(
    Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

/** The Bolo Bhai type scale (BoloBhai.md §12.2). */
data class BoloTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val onboardingTitle: TextStyle,
    val body: TextStyle,
    val bodyMedium: TextStyle,
    val button: TextStyle,
    val meta: TextStyle,
    val sectionLabel: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
    val monoSemibold: TextStyle,
)

@Composable
fun boloTypography(): BoloTypography {
    val inter = interFamily()
    val space = spaceGroteskFamily()
    val mono = jetBrainsMonoFamily()
    return BoloTypography(
        h1 = TextStyle(fontFamily = space, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.02).em),
        h2 = TextStyle(fontFamily = space, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        onboardingTitle = TextStyle(fontFamily = space, fontWeight = FontWeight.Bold, fontSize = 40.sp),
        body = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodyMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        button = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        meta = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        sectionLabel = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.06.em),
        mono = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        monoSmall = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 11.sp),
        monoSemibold = TextStyle(fontFamily = mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    )
}
