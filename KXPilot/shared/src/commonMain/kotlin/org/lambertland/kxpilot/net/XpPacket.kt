package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// XPilot-NG wire protocol — packet type constants
// ---------------------------------------------------------------------------
//
// These mirror the PKT_* defines in xpilot-ng-4.7.3/src/common/packet.h.
// Only the packets that the SERVER needs to *receive* from clients (or that
// the server sends to clients during game-play) are included here.
// Contact-layer packets live in XpContactPacket.kt.

object PktType {
    // Handshake / session control
    const val UNDEFINED: Int = 0
    const val VERIFY: Int = 1 // client sends user+nick to new per-player socket
    const val REPLY: Int = 2 // server replies PKT_REPLY | replyTo | status
    const val PLAY: Int = 3 // client: ready to receive frames
    const val QUIT: Int = 4 // client disconnecting
    const val MESSAGE: Int = 5 // server → client chat message
    const val START: Int = 6 // server → client: frame-update start
    const val END: Int = 7 // server → client: frame-update end

    // State / player info
    const val SELF: Int = 8
    const val KEYBOARD: Int = 24 // client → server: key bitmap
    const val TALK: Int = 51 // client → server: chat message
    const val TALK_ACK: Int = 52 // server → client: chat ack
    const val DISPLAY: Int = 55 // client → server: view-size / debris prefs
    const val MOTD: Int = 58 // client → server: request MOTD chunk
    const val SHAPE: Int = 57 // client → server: ship shape upload

    // Reliability layer
    const val RELIABLE: Int = 42 // server → client: reliable data block
    const val ACK: Int = 43 // client → server: reliable ack (byte pos)

    // Score / timing (server → client)
    const val SCORE: Int = 29
    const val TIMING: Int = 76
    const val LEAVE: Int = 33
    const val MAGIC: Int = 41

    // Misc
    const val SHUTDOWN: Int = 19
    const val FAILURE: Int = 101
    const val SUCCESS: Int = 102
}

// ---------------------------------------------------------------------------
// Sealed class hierarchy for parsed game-layer packets
// ---------------------------------------------------------------------------

/**
 * A decoded XPilot-NG game-layer UDP packet (i.e. after the per-player TCP-
 * handshake analogue is complete).
 *
 * The server **receives** these from clients; the variants below cover all
 * packets a client can send during the connection lifecycle.
 *
 * Packets the server only *sends* (SELF, SHIP, SCORE, …) are represented as
 * [Raw] so the decoder can echo them back in tests without knowing their
 * internal structure.
 */
sealed class XpPacket {
    // -----------------------------------------------------------------------
    // Handshake packets (received during CONN_LISTENING state)
    // -----------------------------------------------------------------------

    /**
     * PKT_VERIFY — first packet sent by the client on the per-player socket.
     *
     * Wire layout: `0x01 | NUL-terminated user | NUL-terminated nick`
     */
    data class Verify(
        val user: String,
        val nick: String,
    ) : XpPacket()

    // -----------------------------------------------------------------------
    // Session-control packets (received while CONN_PLAYING)
    // -----------------------------------------------------------------------

    /**
     * PKT_PLAY — client signals it is ready to receive frame updates.
     * Wire layout: `0x03`
     */
    object Play : XpPacket()

    /**
     * PKT_QUIT — client is disconnecting.
     * Wire layout: `0x04`
     */
    object Quit : XpPacket()

    // -----------------------------------------------------------------------
    // Reliability layer (received any time after verify)
    // -----------------------------------------------------------------------

    /**
     * PKT_ACK — client acknowledges reliable data up to [bytePos].
     * Wire layout: `0x2B | int32 bytePos` (big-endian)
     */
    data class Ack(
        val bytePos: Int,
    ) : XpPacket()

    // -----------------------------------------------------------------------
    // Gameplay packets (received while CONN_PLAYING)
    // -----------------------------------------------------------------------

    /**
     * PKT_KEYBOARD — key-state bitmap from the client.
     * Wire layout: `0x18 | int32 keyChangeId | byte[KEYBOARD_SIZE] keyBitmap`
     * KEYBOARD_SIZE = 9 (since 3.8.0)
     */
    data class Keyboard(
        val keyChangeId: Int,
        val keyBitmap: ByteArray,
    ) : XpPacket() {
        companion object {
            const val KEYBOARD_SIZE: Int = 9
        }

        override fun equals(other: Any?) = other is Keyboard && keyChangeId == other.keyChangeId && keyBitmap.contentEquals(other.keyBitmap)

        override fun hashCode() = 31 * keyChangeId + keyBitmap.contentHashCode()
    }

    /**
     * PKT_TALK — chat message from the client.
     * Wire layout: `0x33 | int32 seqNum | NUL-terminated message`
     */
    data class Talk(
        val seqNum: Int,
        val message: String,
    ) : XpPacket()

    /**
     * PKT_DISPLAY — client reports its view-size and rendering capabilities.
     * Wire layout: `0x37 | int16 viewWidth | int16 viewHeight | byte debrisColors | byte sparkRand`
     */
    data class Display(
        val viewWidth: Int,
        val viewHeight: Int,
        val debrisColors: Int,
        val sparkRand: Int,
    ) : XpPacket()

    /**
     * PKT_MOTD — client requests a chunk of the MOTD starting at [offset].
     * Wire layout: `0x3A | int32 offset`
     */
    data class MotdRequest(
        val offset: Int,
    ) : XpPacket()

    // -----------------------------------------------------------------------
    // Unknown / unimplemented / server-only packets
    // -----------------------------------------------------------------------

    /**
     * A packet type that this decoder does not (yet) interpret.
     * [typeId] is the raw PKT_* byte; [payload] is the remaining bytes of
     * the datagram after the type byte.
     */
    data class Raw(
        val typeId: Int,
        val payload: ByteArray,
    ) : XpPacket() {
        override fun equals(other: Any?) = other is Raw && typeId == other.typeId && payload.contentEquals(other.payload)

        override fun hashCode() = 31 * typeId + payload.contentHashCode()
    }
}
