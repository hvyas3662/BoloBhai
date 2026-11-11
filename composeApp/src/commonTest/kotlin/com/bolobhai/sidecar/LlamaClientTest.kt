package com.bolobhai.sidecar

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/** Integration test against a fake llama-server (BoloBhai.md §18). */
class LlamaClientTest {

    private fun respond(ex: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray()
        ex.sendResponseHeaders(code, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    @Test
    fun complete_extractsChoiceContent() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { ex ->
            ex.requestBody.readBytes()
            respond(ex, 200, """{"choices":[{"message":{"role":"assistant","content":"Dear team, kal meeting hai."}}]}""")
        }
        server.start()
        val client = LlamaClient("http://127.0.0.1:${server.address.port}")
        try {
            assertEquals(
                "Dear team, kal meeting hai.",
                client.complete(system = "Write an email.", user = "kal meeting"),
            )
        } finally {
            client.close()
            server.stop(0)
        }
    }

    @Test
    fun complete_emptyOnError() = runBlocking {
        val client = LlamaClient("http://127.0.0.1:1")
        try {
            assertEquals("", client.complete("s", "u"))
        } finally {
            client.close()
        }
    }
}
