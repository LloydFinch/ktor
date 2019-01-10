package io.ktor.client.features.websocket

import io.ktor.application.*
import io.ktor.client.tests.utils.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import java.nio.*
import kotlin.test.*

class WebSocketTest : TestWithKtor() {
    override val server: ApplicationEngine = embeddedServer(Netty, serverPort) {
        install(io.ktor.websocket.WebSockets)
        routing {
            webSocket("/ws") {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> send(frame)
                        is Frame.Binary -> send(frame)
                        else -> assert(false)
                    }
                }
            }
        }
    }

    @Test
    fun testPingPong() = clientsTest {
        config {
            install(WebSockets)
        }

        test { client ->
            client.ws(port = serverPort, path = "ws") {
                assertTrue(masking)

                repeat(10) {
                    ping(it.toString())
                }
            }
        }
    }

    @Test
    fun testRemotePingPong(): Unit = clientsTest {
        val remote = "echo.websocket.org"

        config {
            install(WebSockets)
        }

        test { client ->
            client.ws(host = remote) {
                repeat(10) {
                    ping(it.toString())
                }
            }
        }
    }

    @Test
    fun testSecureRemotePingPong(): Unit = clientsTest {
        val remote = "echo.websocket.org"

        config {
            install(WebSockets)
        }

        test { client ->
            client.wss(host = remote) {
                repeat(10) {
                    ping(it.toString())
                }
            }
        }
    }

    private suspend fun WebSocketSession.ping(salt: String) {
        outgoing.send(Frame.Text("text: $salt"))
        val frame = incoming.receive()
        assert(frame is Frame.Text)
        assertEquals("text: $salt", (frame as Frame.Text).readText())

        val data = "text: $salt".toByteArray()
        outgoing.send(Frame.Binary(true, ByteBuffer.wrap(data)))
        val binaryFrame = incoming.receive()
        assert(binaryFrame is Frame.Binary)

        val buffer = (binaryFrame as Frame.Binary).buffer
        val received = buffer.moveToByteArray()
        assertEquals(data.contentToString(), received.contentToString())
    }
}
