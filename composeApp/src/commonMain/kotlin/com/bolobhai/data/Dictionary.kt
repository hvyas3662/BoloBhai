package com.bolobhai.data

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable

/** A heard→intended spelling fix applied after transcription (BoloBhai.md §13.4). */
@Serializable
data class Correction(val wrong: String, val right: String)

/** Custom vocabulary + corrections (`dictionary.yaml`, BoloBhai.md §13.4). */
@Serializable
data class Dictionary(
    val customWords: List<String> = emptyList(),
    val corrections: List<Correction> = emptyList(),
)

/** Pure (de)serialization of [Dictionary] as YAML; no disk access. */
object DictionaryYaml {
    private val yaml = Yaml.default
    fun encode(dict: Dictionary): String = yaml.encodeToString(Dictionary.serializer(), dict)
    fun decode(text: String): Dictionary = yaml.decodeFromString(Dictionary.serializer(), text)
}
