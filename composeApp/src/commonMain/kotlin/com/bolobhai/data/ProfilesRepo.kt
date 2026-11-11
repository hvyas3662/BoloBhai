package com.bolobhai.data

import com.bolobhai.domain.Profile
import com.bolobhai.domain.parseProfile
import com.bolobhai.domain.serializeProfile
import com.bolobhai.domain.slug
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Reads/writes profile `.md` files in the profiles folder (BoloBhai.md §11). */
class ProfilesRepo(private val dir: Path) {

    /** Copy the shipped defaults only if the folder has no profiles yet. */
    fun ensureDefaults() {
        dir.createDirectories()
        if (listFiles().isNotEmpty()) return
        DefaultProfiles.files.forEach { (fileName, content) ->
            dir.resolve(fileName).writeText(content)
        }
    }

    fun list(): List<Profile> = listFiles()
        .map { parseProfile(it.name, it.readText()) }
        .sortedBy { it.name.lowercase() }

    fun read(fileName: String): Profile? {
        val file = dir.resolve(fileName)
        return if (Files.exists(file)) parseProfile(fileName, file.readText()) else null
    }

    fun write(profile: Profile) {
        dir.createDirectories()
        dir.resolve(profile.fileName).writeText(serializeProfile(profile))
    }

    fun delete(fileName: String) {
        dir.resolve(fileName).deleteIfExists()
    }

    /** Move the profile to the OS trash (BoloBhai.md §13.3); falls back to a hard delete. */
    fun deleteToTrash(fileName: String) {
        val file = dir.resolve(fileName)
        val trashed = runCatching {
            val desktop = java.awt.Desktop.getDesktop()
            desktop.isSupported(java.awt.Desktop.Action.MOVE_TO_TRASH) && desktop.moveToTrash(file.toFile())
        }.getOrDefault(false)
        if (!trashed) file.deleteIfExists()
    }

    /** Rename the display name: rewrite frontmatter and rename the file to slug(name).md. */
    fun rename(fileName: String, newName: String): Profile {
        val current = read(fileName) ?: error("No such profile: $fileName")
        val newFile = "${slug(newName)}.md"
        val updated = current.copy(fileName = newFile, name = newName)
        dir.resolve(newFile).writeText(serializeProfile(updated))
        if (newFile != fileName) delete(fileName)
        return updated
    }

    private fun listFiles(): List<Path> {
        if (!Files.isDirectory(dir)) return emptyList()
        Files.list(dir).use { stream ->
            return stream.filter { it.extension == "md" }.sorted().toList()
        }
    }
}
