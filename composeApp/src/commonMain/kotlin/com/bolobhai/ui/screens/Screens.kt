package com.bolobhai.ui.screens

import androidx.compose.runtime.Composable

/**
 * Placeholder screens. Titles and subtitles follow BoloBhai.md §13; each milestone replaces
 * the ComingSoon body with the real content.
 */

@Composable
fun AllProfilesScreen() = ScreenScaffold(
    title = "All Profiles",
    subtitle = "Every .md file in ~/.bolobhai/profiles/",
) { ComingSoon() }

