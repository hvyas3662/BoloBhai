package com.bolobhai.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.test.Test
import kotlin.test.assertTrue

/** Forces every icon's Lucide path string to parse/build — a malformed path throws here. */
class IconsTest {

    private val all: List<ImageVector> = listOf(
        BoloIcons.History,
        BoloIcons.QuickProfiles,
        BoloIcons.AllProfiles,
        BoloIcons.Dictionary,
        BoloIcons.Models,
        BoloIcons.Settings,
        BoloIcons.Mic,
        BoloIcons.Sun,
        BoloIcons.Moon,
    )

    @Test
    fun allIcons_buildWithExpectedViewport() {
        assertTrue(all.isNotEmpty())
        all.forEach {
            assertTrue(it.viewportWidth == 24f && it.viewportHeight == 24f)
        }
    }
}
