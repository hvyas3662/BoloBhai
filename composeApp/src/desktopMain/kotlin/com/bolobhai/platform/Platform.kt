package com.bolobhai.platform

import java.awt.Desktop
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

/**
 * Narrow OS-specific surface (BoloBhai.md §4). M1 only needs the app-data root and open-folder;
 * hotkeys, paste and permissions arrive with the Mac/Windows milestones.
 */
interface Platform {
    val id: String // "mac" | "windows" | "other"

    /** App data root: ~/.bolobhai (mac) / %APPDATA%\BoloBhai (windows). */
    fun appDataRoot(): Path

    fun settingsFile(): Path = appDataRoot().resolve("settings.json")

    fun openFolder(path: Path)
}

private fun openInFileManager(path: Path) {
    path.createDirectories()
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        Desktop.getDesktop().open(path.toFile())
    }
}

private object MacPlatform : Platform {
    override val id = "mac"
    override fun appDataRoot(): Path = Path(System.getProperty("user.home"), ".bolobhai")
    override fun openFolder(path: Path) = openInFileManager(path)
}

private object WinPlatform : Platform {
    override val id = "windows"
    override fun appDataRoot(): Path {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        return Path(appData, "BoloBhai")
    }
    override fun openFolder(path: Path) = openInFileManager(path)
}

private object OtherPlatform : Platform {
    override val id = "other"
    override fun appDataRoot(): Path = Path(System.getProperty("user.home"), ".bolobhai")
    override fun openFolder(path: Path) = openInFileManager(path)
}

fun currentPlatform(): Platform {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") || os.contains("darwin") -> MacPlatform
        os.contains("win") -> WinPlatform
        else -> OtherPlatform
    }
}
