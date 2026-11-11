package com.bolobhai.data

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Reads/writes `dictionary.yaml` (BoloBhai.md §13.4); empty dictionary on a missing/corrupt file. */
class DictionaryRepo(private val file: Path) {

    fun load(): Dictionary {
        if (!Files.exists(file)) return Dictionary()
        return try {
            DictionaryYaml.decode(file.readText())
        } catch (_: Exception) {
            Dictionary()
        }
    }

    fun save(dict: Dictionary) {
        file.createParentDirectories()
        file.writeText(DictionaryYaml.encode(dict))
    }
}
