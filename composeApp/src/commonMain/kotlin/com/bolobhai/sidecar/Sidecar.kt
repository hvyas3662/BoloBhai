package com.bolobhai.sidecar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class SidecarStatus { Stopped, Starting, Ready, Failed, NotInstalled }

/** Restart back-off for a crashed sidecar (BoloBhai.md §9): 1s, 2s, 5s, then give up. */
class BackoffPolicy(private val delaysMs: List<Long> = listOf(1000, 2000, 5000)) {
    /** Delay before attempt [attempt] (0-based), or null once retries are exhausted. */
    fun delayFor(attempt: Int): Long? = delaysMs.getOrNull(attempt)
}

@Serializable
private data class InferenceResponse(val text: String = "")

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** Extract the transcript from a whisper-server /inference JSON response; "" on any parse error. */
fun parseInferenceText(body: String): String = try {
    json.decodeFromString(InferenceResponse.serializer(), body).text.trim()
} catch (_: Exception) {
    ""
}
