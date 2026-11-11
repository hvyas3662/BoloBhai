package com.bolobhai.sidecar

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Integration test against a fake whisper-server (BoloBhai.md §18). */
class WhisperClientTest {

    private fun respond(ex: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray()
        ex.sendResponseHeaders(code, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    @Test
    fun health_and_transcribe() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/health") { respond(it, 200, "OK") }
        server.createContext("/inference") { ex ->
            ex.requestBody.readBytes() // drain multipart
            respond(ex, 200, """{"text":" kal meeting hai "}""")
        }
        server.start()
        val client = WhisperClient("http://127.0.0.1:${server.address.port}")
        try {
            assertTrue(client.health())
            assertEquals("kal meeting hai", client.transcribe(ByteArray(16), "hi", "Triologic"))
        } finally {
            client.close()
            server.stop(0)
        }
    }

    @Test
    fun health_falseWhenServerDown() = runBlocking {
        val client = WhisperClient("http://127.0.0.1:1") // nothing listening
        try {
            assertFalse(client.health())
        } finally {
            client.close()
        }
    }
}
