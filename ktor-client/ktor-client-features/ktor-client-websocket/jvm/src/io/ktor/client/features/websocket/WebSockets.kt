package io.ktor.client.features.websocket

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.io.core.*
import kotlin.reflect.full.*

/**
 * Client WebSocket feature.
 *
 * @property maxFrameSize - max size of single websocket frame.
 */
@KtorExperimentalAPI
@UseExperimental(WebSocketInternalAPI::class)
class WebSockets(
    val maxFrameSize: Long = Int.MAX_VALUE.toLong()
) {

    companion object Feature : HttpClientFeature<Unit, WebSockets> {
        override val key: AttributeKey<WebSockets> = AttributeKey("Websocket")

        @InternalAPI
        val sessionKey: AttributeKey<WebSocketSession> = AttributeKey("WebsocketSession")

        override fun prepare(block: Unit.() -> Unit): WebSockets = WebSockets()

        override fun install(feature: WebSockets, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Render) { _ ->
                if (!context.url.protocol.isWebsocket()) return@intercept
                proceedWith(WebSocketContent())
            }

            scope.responsePipeline.intercept(HttpResponsePipeline.Transform) { (info, response) ->
                val content = context.request.content

                if (!info.type.isSubclassOf(WebSocketSession::class)
                    || response !is HttpResponse
                    || response.status.value != HttpStatusCode.SwitchingProtocols.value
                    || content !is WebSocketContent
                ) return@intercept

                val session = context.attributes.getOrNull(sessionKey) ?: return@intercept

                if (info.type.isSubclassOf(DefaultWebSocketSession::class)) {
                    val defaultSession = if (session is DefaultWebSocketSession) {
                        session
                    } else {
                        DefaultWebSocketSessionImpl(session, feature.maxFrameSize)
                    }

                    proceedWith(HttpResponseContainer(info, DefaultClientWebSocketSession(context, defaultSession)))
                    return@intercept
                }

                proceedWith(HttpResponseContainer(info, DelegatingClientWebSocketSession(context, session)))
            }
        }
    }
}
