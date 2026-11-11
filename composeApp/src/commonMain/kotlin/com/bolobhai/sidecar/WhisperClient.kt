package com.bolobhai.sidecar

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/**
 * HTTP client for the whisper.cpp server sidecar (BoloBhai.md §9): health + /inference.
 * Transcription runs at temperature 0 with the custom words passed as the initial prompt.
 */
class WhisperClient(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient(CIO),
) {
    suspend fun health(): Boolean = try {
        client.get("$baseUrl/health").status.isSuccess()
    } catch (_: Exception) {
        false
    }

    /** Returns the transcript text, or "" if the request failed. */
    suspend fun transcribe(wav: ByteArray, language: String, prompt: String): String = try {
        val response = client.submitFormWithBinaryData(
            url = "$baseUrl/inference",
            formData = formData {
                append(
                    "file", wav,
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/wav")
                        append(HttpHeaders.ContentDisposition, "filename=\"audio.wav\"")
                    },
                )
                append("temperature", "0")
                append("response_format", "json")
                append("language", language)
                if (prompt.isNotBlank()) append("prompt", prompt)
            },
        )
        parseInferenceText(response.bodyAsText())
    } catch (_: Exception) {
        ""
    }

    fun close() = client.close()
}
