package io.ktor.client.tests.utils

import io.ktor.client.*
import io.ktor.client.engine.*
import kotlinx.coroutines.*
import java.util.*

/**
 * Perform test with selected client engine [factory].
 */
fun <T : HttpClientEngineConfig> clientTest(
    factory: HttpClientEngineFactory<T>,
    block: suspend TestClientBuilder<T>.() -> Unit
): Unit = runBlocking {
    val builder = TestClientBuilder<T>().apply { block() }
    val client = HttpClient(factory, block = builder.config)

    client.use {
        builder.test(it)
    }

    client.coroutineContext[Job]!!.join()
}

/**
 * Perform test with selected client [engine].
 */
fun clientTest(
    engine: HttpClientEngine,
    block: suspend TestClientBuilder<*>.() -> Unit
): Unit = clientTest(HttpClient(engine), block)

/**
 * Perform test with selected [client] or client loaded by service loader.
 */
fun clientTest(
    client: HttpClient = HttpClient(),
    block: suspend TestClientBuilder<HttpClientEngineConfig>.() -> Unit
): Unit = runBlocking {
    val builder = TestClientBuilder<HttpClientEngineConfig>().also { it.block() }

    @Suppress("UNCHECKED_CAST")
    client
        .config { builder.config(this as HttpClientConfig<HttpClientEngineConfig>) }
        .use { client -> builder.test(client) }
}

/**
 * Perform test against all clients from dependencies.
 */
fun clientsTest(
    block: suspend TestClientBuilder<HttpClientEngineConfig>.() -> Unit
): Unit {
    val engines: List<HttpClientEngineContainer> = HttpClientEngineContainer::class.java.let {
        ServiceLoader.load(it, it.classLoader).toList()
    }

    engines.forEach {
        clientTest(it.factory, block)
    }
}

class TestClientBuilder<T : HttpClientEngineConfig>(
    var config: HttpClientConfig<T>.() -> Unit = {},
    var test: suspend (client: HttpClient) -> Unit = {}
)

fun <T : HttpClientEngineConfig> TestClientBuilder<T>.config(block: HttpClientConfig<T>.() -> Unit): Unit {
    config = block
}

fun TestClientBuilder<*>.test(block: suspend (client: HttpClient) -> Unit): Unit {
    test = block
}
