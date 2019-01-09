package io.ktor.client.engine.okhttp

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.cio.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlinx.coroutines.io.jvm.javaio.*
import okhttp3.*
import kotlin.coroutines.*

class OkHttpEngine(override val config: OkHttpConfig) : HttpClientJvmEngine("ktor-okhttp") {
    private val engine = OkHttpClient.Builder()
        .apply(config.config)
        .build()!!

    override suspend fun execute(call: HttpClientCall, data: HttpRequestData): HttpEngineCall {
        val request = DefaultHttpRequest(call, data)
        val callContext = createCallContext()

        val builder = Request.Builder()

        with(builder) {
            url(request.url.toString())

            mergeHeaders(request.headers, request.content) { key, value ->
                addHeader(key, value)
            }

            method(request.method.value, request.content.convertToOkHttpBody(callContext))
        }

        val engineRequest = builder.build()!!

        val response = if (request.content !is ClientUpgradeContent) {
            executeHttpRequest(engineRequest, callContext, call)

        } else {
            if (request.url.protocol.isWebsocket()) {
                startWebsocketSession(engineRequest, callContext, call, request)
            } else {
                throw UnsupportedUpgradeProtocolException(request.url)
            }
        }

        return HttpEngineCall(request, response)
    }

    private suspend fun executeHttpRequest(
        engineRequest: Request,
        callContext: CoroutineContext,
        call: HttpClientCall
    ): HttpResponse {
        val requestTime = GMTDate()
        val response = engine.execute(engineRequest)

        val body = response.body()
        callContext[Job]?.invokeOnCompletion { body?.close() }

        val responseContent = withContext(callContext) {
            body?.byteStream()?.toByteReadChannel(
                context = callContext,
                pool = KtorDefaultPool
            ) ?: ByteReadChannel.Empty
        }

        return OkHttpResponse(response, call, requestTime, responseContent, callContext)
    }

    private fun startWebsocketSession(
        engineRequest: Request,
        callContext: CoroutineContext,
        call: HttpClientCall,
        request: DefaultHttpRequest
    ): HttpResponse {
        val pingInterval = engine.pingIntervalMillis().toLong()
        val requestTime = GMTDate()

        val session = OkHttpWebsocketSession(
            engine, engineRequest,
            callContext,
            pingInterval, pingInterval,
            masking = true, maxFrameSize = Long.MAX_VALUE
        )

        request.attributes.put(WebSockets.sessionKey, session)
        return UpgradeHttpResponse(call, requestTime, callContext)
    }
}


internal fun OutgoingContent.convertToOkHttpBody(callContext: CoroutineContext): RequestBody? = when (this) {
    is OutgoingContent.ByteArrayContent -> RequestBody.create(null, bytes())
    is OutgoingContent.ReadChannelContent -> StreamRequestBody(contentLength) { readFrom() }
    is OutgoingContent.WriteChannelContent -> {
        StreamRequestBody(contentLength) { GlobalScope.writer(callContext) { writeTo(channel) }.channel }
    }
    is OutgoingContent.NoContent -> null
    else -> throw UnsupportedContentTypeException(this)
}
