@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("FrameKt")
package io.ktor.http.cio.websocket

import io.ktor.util.*
import kotlinx.coroutines.*
import java.nio.*

/**
 * Frame content
 */
val Frame.buffer: ByteBuffer
    get() = ByteBuffer.wrap(data)

/**
 * Represents an application level binary frame.
 * In a RAW web socket session a big text frame could be fragmented
 * (separated into several text frames so they have [fin] = false except the last one).
 * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
 */
fun Frame.Companion.Binary(fin: Boolean, buffer: ByteBuffer) = Frame.Binary(fin, buffer.moveToByteArray())

/**
 * Represents an application level text frame.
 * In a RAW web socket session a big text frame could be fragmented
 * (separated into several text frames so they have [fin] = false except the last one).
 * Please note that a boundary between fragments could be in the middle of multi-byte (unicode) character
 * so don't apply String constructor to every fragment but use decoder loop instead of concatenate fragments first.
 * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
 */
fun Frame.Companion.Text(fin: Boolean, buffer: ByteBuffer) = Frame.Text(fin, buffer.moveToByteArray())

/**
 * Represents a low-level level close frame. It could be sent to indicate web socket session end.
 * Usually there is no need to send/handle it unless you have a RAW web socket session.
 */
fun Frame.Companion.Close(buffer: ByteBuffer) = Frame.Close(buffer.moveToByteArray())

/**
 * Represents a low-level ping frame. Could be sent to test connection (peer should reply with [Pong]).
 * Usually there is no need to send/handle it unless you have a RAW web socket session.
 */
fun Frame.Companion.Ping(buffer: ByteBuffer) = Frame.Ping(buffer.moveToByteArray())

/**
 * Represents a low-level pong frame. Should be sent in reply to a [Ping] frame.
 * Usually there is no need to send/handle it unless you have a RAW web socket session.
 */
fun Frame.Companion.Pong(buffer: ByteBuffer, disposableHandle: DisposableHandle = NonDisposableHandle) =
    Frame.Pong(buffer.moveToByteArray(), disposableHandle)

/**
 * Create a particular [Frame] instance by frame type
 */
fun Frame.Companion.byType(fin: Boolean, frameType: FrameType, buffer: ByteBuffer): Frame =
    byType(fin, frameType, buffer.moveToByteArray())

/**
 * Read close reason from close frame or null if no close reason provided
 */
fun Frame.Close.readReason(): CloseReason? {
    if (data.size < 2) {
        return null
    }

    val buffer = ByteBuffer.wrap(data)!!
    buffer.mark()
    val code = buffer.short
    val message = buffer.decodeString(Charsets.UTF_8)

    buffer.reset()

    return CloseReason(code, message)
}
