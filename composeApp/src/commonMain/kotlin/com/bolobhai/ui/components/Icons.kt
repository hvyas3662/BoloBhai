package com.bolobhai.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Thin outline icons (BoloBhai.md §12: 2px stroke, round caps, no fill). Path data is the
 * Lucide icon set (ISC License). Tinted at the call site via Icon(tint = ...).
 */
private fun strokeIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

object BoloIcons {
    // Sidebar nav
    val History = strokeIcon(
        "History",
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5 M12 7v5l4 2",
    )
    val QuickProfiles = strokeIcon(
        "QuickProfiles",
        "M13 2 3 14h9l-1 8 10-12h-9z",
    )
    val AllProfiles = strokeIcon(
        "AllProfiles",
        "M4 4a2 2 0 0 1 2-2h8l6 6v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z M14 2v6h6 M8 13h8 M8 17h8",
    )
    val Dictionary = strokeIcon(
        "Dictionary",
        "M12 7v14 M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z",
    )
    val Models = strokeIcon(
        "Models",
        "M12 8c5 0 9-1.57 9-3.5S17 1 12 1 3 2.57 3 4.5 7 8 12 8Z M3 4.5v15C3 21.43 7 23 12 23s9-1.57 9-3.5v-15 M3 12c0 1.93 4 3.5 9 3.5s9-1.57 9-3.5",
    )
    val Settings = strokeIcon(
        "Settings",
        // Lucide "settings" cog: gear teeth path + center circle.
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2Z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
    )

    // Misc
    val Mic = strokeIcon(
        "Mic",
        "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z M19 10v2a7 7 0 0 1-14 0v-2 M12 19v3 M8 22h8",
    )
    val Sun = strokeIcon(
        "Sun",
        "M16 12a4 4 0 1 1-8 0 4 4 0 1 1 8 0 M12 2v2 M12 20v2 M4.93 4.93l1.41 1.41 M17.66 17.66l1.41 1.41 M2 12h2 M20 12h2 M4.93 19.07l1.41-1.41 M17.66 6.34l1.41-1.41",
    )
    val Moon = strokeIcon(
        "Moon",
        "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z",
    )
    val Play = strokeIcon(
        "Play",
        "M6 3v18l15-9z",
    )
    val Close = strokeIcon(
        "Close",
        "M18 6 6 18 M6 6l12 12",
    )
    val Stop = strokeIcon(
        "Stop",
        "M7 7h10v10H7z",
    )
    val Pause = strokeIcon(
        "Pause",
        "M8 4v16 M16 4v16",
    )
    val ChevronDown = strokeIcon(
        "ChevronDown",
        "m6 9 6 6 6-6",
    )
    val Logs = strokeIcon(
        "Logs",
        "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z M14 2v5h5 M9 13h6 M9 17h5",
    )
}
