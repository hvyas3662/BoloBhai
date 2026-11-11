package com.bolobhai.data

import kotlinx.serialization.json.Json

/** Pure (de)serialization of [Settings]; no disk access. */
object SettingsJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = false
    }

    fun encode(settings: Settings): String = json.encodeToString(Settings.serializer(), settings)

    fun decode(text: String): Settings = json.decodeFromString(Settings.serializer(), text)
}
