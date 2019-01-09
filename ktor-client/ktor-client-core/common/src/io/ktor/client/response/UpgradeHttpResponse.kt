package io.ktor.client.response

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.io.*
import kotlin.coroutines.*

class UpgradeHttpResponse(
    override val call: HttpClientCall,
    override val requestTime: GMTDate,
    override val coroutineContext: CoroutineContext
) : HttpResponse {
    override val responseTime: GMTDate = GMTDate()

    override val status: HttpStatusCode = HttpStatusCode.SwitchingProtocols
    override val headers: Headers = Headers.Empty
    override val version: HttpProtocolVersion = HttpProtocolVersion.HTTP_1_1

    override val content: ByteReadChannel = ByteReadChannel.Empty
}
