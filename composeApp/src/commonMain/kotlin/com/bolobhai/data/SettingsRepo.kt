package com.bolobhai.data

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

interface SettingsRepo {
    fun load(): Settings
    fun save(settings: Settings)
}

/** Reads/writes settings.json at [file]; falls back to defaults on a missing or corrupt file. */
class FileSettingsRepo(private val file: Path) : SettingsRepo {

    override fun load(): Settings {
        if (!Files.exists(file)) return Settings()
        return try {
            SettingsJson.decode(file.readText())
        } catch (_: Exception) {
            Settings()
        }
    }

    override fun save(settings: Settings) {
        file.createParentDirectories()
        file.writeText(SettingsJson.encode(settings))
    }
}
