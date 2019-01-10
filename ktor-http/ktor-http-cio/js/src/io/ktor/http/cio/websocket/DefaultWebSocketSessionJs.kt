package io.ktor.http.cio.websocket

/**
 * Create [DefaultWebSocketSession] from session.
 */
actual fun DefaultWebSocketSession(
    session: WebSocketSession,
    pingInterval: Long,
    timeoutMillis: Long
): DefaultWebSocketSession {
    error("CIO [DefaultWebSocketSession] is not supported on js platform.")
}
