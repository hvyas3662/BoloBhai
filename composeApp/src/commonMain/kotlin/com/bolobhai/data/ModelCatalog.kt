package com.bolobhai.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A downloadable model from `models.json` (BoloBhai.md §2.1). */
@Serializable
data class ModelEntry(
    val id: String,
    val kind: String, // "stt" | "llm"
    val name: String,
    val file: String,
    val url: String,
    val sizeBytes: Long,
    val ram: String,
    val note: String? = null,
    val default: Boolean = false,
)

@Serializable
data class ModelCatalog(val models: List<ModelEntry>) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): ModelCatalog = json.decodeFromString(serializer(), text)

        /** The bundled catalog from resources (BoloBhai.md §17: shipped, not the models). */
        fun load(): ModelCatalog {
            val stream = ModelCatalog::class.java.getResourceAsStream("/models.json")
                ?: error("models.json missing from resources")
            return parse(stream.bufferedReader().use { it.readText() })
        }
    }
}
