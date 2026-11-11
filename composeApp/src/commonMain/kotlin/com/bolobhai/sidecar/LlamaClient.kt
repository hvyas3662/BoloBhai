package com.bolobhai.sidecar

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatRequest(
    val messages: List<ChatMessage>,
    val temperature: Double,
    val max_tokens: Int,
    val top_p: Double,
)

@Serializable
private data class ChatChoice(val message: ChatMessage? = null)

@Serializable
private data class ChatResponse(val choices: List<ChatChoice> = emptyList())

/**
 * OpenAI-compatible client for the llama.cpp server sidecar (BoloBhai.md §2/§9).
 * Post-processing settings: temperature 0.2, max_tokens 1024, top_p 0.9.
 */
class LlamaClient(
    private val baseUrl: String,
    private val client: HttpClient = HttpClient(CIO),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun health(): Boolean = try {
        client.get("$baseUrl/health").status.isSuccess()
    } catch (_: Exception) {
        false
    }

    /** Returns the raw assistant content (caller applies cleanup), or "" on failure. */
    suspend fun complete(system: String, user: String): String = try {
        val request = ChatRequest(
            messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
            temperature = 0.2,
            max_tokens = 1024,
            top_p = 0.9,
        )
        val response = client.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), request))
        }
        val parsed = json.decodeFromString(ChatResponse.serializer(), response.bodyAsText())
        parsed.choices.firstOrNull()?.message?.content ?: ""
    } catch (_: Exception) {
        ""
    }

    fun close() = client.close()
}
