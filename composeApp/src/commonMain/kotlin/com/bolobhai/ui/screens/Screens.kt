package com.bolobhai.ui.screens

import androidx.compose.runtime.Composable
import com.bolobhai.ui.components.BoloButton

/**
 * M1 placeholder screens. Titles and subtitles follow BoloBhai.md §13; each milestone replaces
 * the ComingSoon body with the real content.
 */

@Composable
fun AllProfilesScreen() = ScreenScaffold(
    title = "All Profiles",
    subtitle = "Every .md file in ~/.bolobhai/profiles/",
) { ComingSoon() }

@Composable
fun DictionaryScreen() = ScreenScaffold(
    title = "Dictionary",
    subtitle = "Custom words and corrections, applied before the LLM.",
) { ComingSoon() }

@Composable
fun ModelsScreen() = ScreenScaffold(
    title = "Models",
    subtitle = "Everything runs on this computer. No cloud, no paid APIs.",
) { ComingSoon() }

@Composable
fun SettingsScreen() = ScreenScaffold(
    title = "Settings",
    trailing = { BoloButton("Run first-time setup again", onClick = {}) },
) { ComingSoon() }
