package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// PacketDecoder — ByteArray → XpPacket
// ---------------------------------------------------------------------------

/**
 * Decodes a raw UDP datagram received from a client into an [XpPacket].
 *
 * This decoder handles the **game-layer** packets (i.e. datagrams received on
 * the per-player socket after the contact handshake).  Contact-layer packets
 * (received on port 15345) are handled separately by [XpContactDecoder].
 *
 * ### Design
 * - [decode] is pure and allocation-light: it reads from a [XpReader] and
 *   returns a sealed [XpPacket] variant.
 * - Unknown packet types return [XpPacket.Raw] rather than throwing, so the
 *   server can log and discard them without crashing.
 * - Truncated/malformed packets throw [XpBufferException] so the caller can
 *   count the error and discard the datagram.
 */
object PacketDecoder {
    /**
     * Decode [data] (a full UDP datagram payload) into an [XpPacket].
     *
     * @throws XpBufferException if the packet is truncated or structurally invalid.
     */
    fun decode(data: ByteArray): XpPacket {
        if (data.isEmpty()) throw XpBufferException("Empty datagram")
        val r = XpReader(data)
        return when (val typeId = r.readByte()) {
            PktType.VERIFY -> decodeVerify(r)
            PktType.PLAY -> XpPacket.Play
            PktType.QUIT -> XpPacket.Quit
            PktType.ACK -> decodeAck(r)
            PktType.KEYBOARD -> decodeKeyboard(r)
            PktType.TALK -> decodeTalk(r)
            PktType.DISPLAY -> decodeDisplay(r)
            PktType.MOTD -> XpPacket.MotdRequest(r.readInt())
            else -> XpPacket.Raw(typeId, data.copyOfRange(1, data.size))
        }
    }

    // -----------------------------------------------------------------------
    // Per-type decode helpers
    // -----------------------------------------------------------------------

    private fun decodeVerify(r: XpReader): XpPacket.Verify {
        val user = r.readString(XP_MAX_CHARS)
        val nick = r.readString(XP_MAX_CHARS)
        return XpPacket.Verify(user, nick)
    }

    /**
     * PKT_ACK wire format (C: `%c%ld%ld`):
     *   - byte  : packet type (already consumed by [decode])
     *   - int32 : bytePos — how far the client has consumed the reliable stream
     *   - int32 : loopTimestamp — server frame-loop value echoed from PKT_RELIABLE
     *             header; used by the C server for RTT estimation.  We read and
     *             discard it since KXPilot uses a simpler reliable channel.
     */
    private fun decodeAck(r: XpReader): XpPacket.Ack {
        val bytePos = r.readInt()
        r.readInt() // loopTimestamp — discard; used for RTT in C server
        return XpPacket.Ack(bytePos)
    }

    private fun decodeKeyboard(r: XpReader): XpPacket.Keyboard {
        val keyChangeId = r.readInt()
        val keyBitmap = r.readBytes(XpPacket.Keyboard.KEYBOARD_SIZE)
        return XpPacket.Keyboard(keyChangeId, keyBitmap)
    }

    /**
     * PKT_TALK wire format (C: `%c%ld%s`):
     *   - byte  : packet type (already consumed)
     *   - int32 : sequence number
     *   - %s    : NUL-terminated string, max [XP_MAX_CHARS] bytes (= 16, incl NUL)
     *
     * Note: the C server uses `%s` (MAX_CHARS = 16), NOT `%S` (MSG_LEN = 4096).
     */
    private fun decodeTalk(r: XpReader): XpPacket.Talk {
        val seqNum = r.readInt()
        val message = r.readString(XP_MAX_CHARS)
        return XpPacket.Talk(seqNum, message)
    }

    private fun decodeDisplay(r: XpReader): XpPacket.Display {
        val viewWidth = r.readShort()
        val viewHeight = r.readShort()
        val debrisColors = r.readByte()
        val sparkRand = r.readByte()
        return XpPacket.Display(viewWidth, viewHeight, debrisColors, sparkRand)
    }
}
