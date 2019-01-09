@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("FrameKt")
package io.ktor.http.cio.websocket

import kotlinx.coroutines.*
import kotlinx.io.charsets.*
import kotlinx.io.core.*

/**
 * A frame received or ready to be sent. It is not reusable and not thread-safe
 * @property fin is it final fragment, should be always `true` for control frames and if no fragmentation is used
 * @property frameType enum value
 * @property data - a frame content or fragment content
 * @property disposableHandle could be invoked when the frame is processed
 */
sealed class Frame(
    val fin: Boolean,
    val frameType: FrameType,
    val data: ByteArray,
    val disposableHandle: DisposableHandle = NonDisposableHandle
) {

    /**
     * Represents an application level binary frame.
     * In a RAW web socket session a big text frame could be fragmented
     * (separated into several text frames so they have [fin] = false except the last one).
     * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
     */
    class Binary(fin: Boolean, data: ByteArray) : Frame(fin, FrameType.BINARY, data) {
        constructor(fin: Boolean, packet: ByteReadPacket) : this(fin, packet.readBytes())
    }

    /**
     * Represents an application level text frame.
     * In a RAW web socket session a big text frame could be fragmented
     * (separated into several text frames so they have [fin] = false except the last one).
     * Please note that a boundary between fragments could be in the middle of multi-byte (unicode) character
     * so don't apply String constructor to every fragment but use decoder loop instead of concatenate fragments first.
     * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
     */
    class Text(fin: Boolean, data: ByteArray) : Frame(fin, FrameType.TEXT, data) {
        constructor(text: String) : this(true, text.toByteArray(Charsets.UTF_8))
        constructor(fin: Boolean, packet: ByteReadPacket) : this(fin, packet.readBytes())
    }

    /**
     * Represents a low-level level close frame. It could be sent to indicate web socket session end.
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     */
    class Close(data: ByteArray) : Frame(true, FrameType.CLOSE, data) {
        constructor(reason: CloseReason) : this(buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeShort(reason.code)
            writeStringUtf8(reason.message)
        })

        constructor(packet: ByteReadPacket) : this(packet.readBytes())
        constructor() : this(Empty)
    }

    /**
     * Represents a low-level ping frame. Could be sent to test connection (peer should reply with [Pong]).
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     */
    class Ping(data: ByteArray) : Frame(true, FrameType.PING, data) {
        constructor(packet: ByteReadPacket) : this(packet.readBytes())
    }

    /**
     * Represents a low-level pong frame. Should be sent in reply to a [Ping] frame.
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     */
    class Pong(
        data: ByteArray, disposableHandle: DisposableHandle = NonDisposableHandle
    ) : Frame(true, FrameType.PONG, data, disposableHandle) {
        constructor(packet: ByteReadPacket) : this(packet.readBytes())
    }

    override fun toString() = "Frame $frameType (fin=$fin, buffer len = ${data.size})"

    /**
     * Creates a frame copy
     */
    fun copy() = byType(fin, frameType, data.copyOf())

    companion object {
        private val Empty = ByteArray(0)

        /**
         * Create a particular [Frame] instance by frame type
         */
        fun byType(fin: Boolean, frameType: FrameType, data: ByteArray): Frame = when (frameType) {
            FrameType.BINARY -> Binary(fin, data)
            FrameType.TEXT -> Text(fin, data)
            FrameType.CLOSE -> Close(data)
            FrameType.PING -> Ping(data)
            FrameType.PONG -> Pong(data)
        }
    }
}

/**
 * Read text content from text frame. Shouldn't be used for fragmented frames: such frames need to be reassembled first
 */
fun Frame.Text.readText(): String {
    require(fin) { "Text could be only extracted from non-fragmented frame" }
    return Charsets.UTF_8.newDecoder().decode(buildPacket { writeFully(data) })
}

/**
 * Read binary content from a frame. For fragmented frames only returns this fragment.
 */
fun Frame.readBytes(): ByteArray {
    return data.copyOf()
}

internal object NonDisposableHandle : DisposableHandle {
    override fun dispose() {}
    override fun toString(): String = "NonDisposableHandle"
}

