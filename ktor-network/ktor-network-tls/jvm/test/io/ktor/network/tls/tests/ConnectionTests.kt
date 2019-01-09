package io.ktor.network.tls.tests

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.network.tls.certificates.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import org.junit.*
import java.io.*
import java.net.*
import java.security.*
import java.security.cert.*

class ConnectionTests {

    @Test
    fun tlsWithoutCloseTest(): Unit = runBlocking {

        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager)
            .tcp()
            .connect("www.google.com", port = 443)
            .tls(Dispatchers.Default, randomAlgorithm = SecureRandom.getInstanceStrong().algorithm)

        val channel = socket.openWriteChannel()

        channel.apply {
            writeStringUtf8("GET / HTTP/1.1\r\n")
            writeStringUtf8("Host: www.google.com\r\n")
            writeStringUtf8("Connection: close\r\n\r\n")
            flush()
        }

        socket.openReadChannel().readRemaining()
        Unit
    }

    @Test
    fun certificateTest(): Unit = runBlocking {
        val keyStore = generateCertificate(File.createTempFile("test", "certificate"))
        val clientCertificates = generateCertificate(File.createTempFile("test", "certificate"))
            .aliases()
            .asSequence()
            .mapNotNull { keyStore.getCertificate(it) }
            .filterIsInstance<X509Certificate>()
            .toList()

        val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            .connect(InetSocketAddress("chat.freenode.net", 6697))
            .tls(Dispatchers.IO, certificates = clientCertificates)

        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)
        output.close()
        socket.close()
    }
}
