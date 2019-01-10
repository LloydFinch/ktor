package io.ktor.http.cio.websocket

/**
 * Create [DefaultWebSocketSession] from session.
 */
@UseExperimental(WebSocketInternalAPI::class)
actual fun DefaultWebSocketSession(
    session: WebSocketSession,
    pingInterval: Long,
    timeoutMillis: Long
): DefaultWebSocketSession = DefaultWebSocketSessionImpl(
    session , pingInterval, timeoutMillis
)
