package org.lambertland.kxpilot.net

import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// PacketEncoder — XpPacket → ByteArray
// ---------------------------------------------------------------------------

/**
 * Encodes an [XpPacket] (or a raw server-originated packet) into a
 * [ByteArray] suitable for sending as a UDP datagram to a client.
 *
 * ### Server-originated packets
 * Most packets the *server sends* are not represented as typed [XpPacket]
 * subclasses (those subclasses are for client→server direction).  Instead,
 * the factory methods on this object produce the correct byte sequences for
 * common server→client packets so that [ClientSession] and future
 * [ServerGameLoop] code can call them directly.
 */
object PacketEncoder {
    // -----------------------------------------------------------------------
    // Generic encode dispatch (round-trip helper, used in tests)
    // -----------------------------------------------------------------------

    /**
     * Encode an [XpPacket] back to bytes.  Only client→server packet types
     * are handled here (for test round-tripping).  [XpPacket.Raw] is
     * re-serialised as-is.
     *
     * @throws IllegalArgumentException for server-only packet types.
     */
    fun encode(packet: XpPacket): ByteArray {
        val w = XpWriter()
        when (packet) {
            is XpPacket.Verify -> {
                w.writeByte(PktType.VERIFY)
                w.writeString(packet.user)
                w.writeString(packet.nick)
            }

            is XpPacket.Play -> {
                w.writeByte(PktType.PLAY)
            }

            is XpPacket.Quit -> {
                w.writeByte(PktType.QUIT)
            }

            is XpPacket.Ack -> {
                // Wire format: %c%ld%ld — type | bytePos | loopTimestamp
                // loopTimestamp is the main_loops value echoed from the PKT_RELIABLE
                // header; for test round-trips we write 0 since there is no live loop.
                w.writeByte(PktType.ACK)
                w.writeInt(packet.bytePos)
                w.writeInt(0) // loopTimestamp — 0 in test context
            }

            is XpPacket.Keyboard -> {
                w.writeByte(PktType.KEYBOARD)
                w.writeInt(packet.keyChangeId)
                w.writeBytes(packet.keyBitmap)
            }

            is XpPacket.Talk -> {
                // Wire format: %c%ld%s — type | seqNum | NUL-terminated string (max XP_MAX_CHARS)
                w.writeByte(PktType.TALK)
                w.writeInt(packet.seqNum)
                w.writeString(packet.message, XP_MAX_CHARS)
            }

            is XpPacket.Display -> {
                w.writeByte(PktType.DISPLAY)
                w.writeShort(packet.viewWidth)
                w.writeShort(packet.viewHeight)
                w.writeByte(packet.debrisColors)
                w.writeByte(packet.sparkRand)
            }

            is XpPacket.MotdRequest -> {
                w.writeByte(PktType.MOTD)
                w.writeInt(packet.offset)
            }

            is XpPacket.Raw -> {
                w.writeByte(packet.typeId)
                w.writeBytes(packet.payload)
            }
        }
        return w.toByteArray()
    }

    // -----------------------------------------------------------------------
    // Server → client factory methods
    // -----------------------------------------------------------------------

    /**
     * PKT_REPLY packet: `0x02 | replyTo | status`
     *
     * Sent by the server to acknowledge a client request.
     * @param replyTo  The PKT_* type the client sent.
     * @param status   [PktType.SUCCESS] or [PktType.FAILURE].
     */
    fun reply(
        replyTo: Int,
        status: Int,
    ): ByteArray {
        val w = XpWriter(3)
        w.writeByte(PktType.REPLY)
        w.writeByte(replyTo)
        w.writeByte(status)
        return w.toByteArray()
    }

    /**
     * PKT_MAGIC packet: `0x29 | uint32 magic`
     *
     * Sent reliably after a successful PKT_VERIFY to give the client
     * its per-connection magic cookie.
     */
    fun magic(magicValue: Int): ByteArray {
        val w = XpWriter(5)
        w.writeByte(PktType.MAGIC)
        w.writeInt(magicValue)
        return w.toByteArray()
    }

    /**
     * PKT_RELIABLE wrapper.
     *
     * Wire format (C: `%c%hd%ld%ld` + raw data):
     * ```
     * byte   : PKT_RELIABLE (0x2A)
     * int16  : len    — number of payload bytes in this chunk
     * int32  : relOff — byte offset of this chunk in the reliable stream
     * int32  : frameLoop — server frame-loop counter; client echoes this
     *                      in the next PKT_ACK for RTT measurement
     * byte[] : data
     * ```
     *
     * @param offset    Byte offset in the reliable data stream for this chunk.
     * @param data      The reliable payload bytes.
     * @param frameLoop Current server frame-loop counter (default 0).
     */
    fun reliable(
        offset: Int,
        data: ByteArray,
        frameLoop: Int = 0,
    ): ByteArray {
        val w = XpWriter(7 + data.size)
        w.writeByte(PktType.RELIABLE)
        w.writeShort(data.size)   // %hd len
        w.writeInt(offset)        // %ld rel_off
        w.writeInt(frameLoop)     // %ld main_loops
        w.writeBytes(data)
        return w.toByteArray()
    }

    /**
     * PKT_LEAVE packet: `0x21 | int16 playerId`
     *
     * Tells all clients that [playerId] has left the game.
     */
    fun leave(playerId: Int): ByteArray {
        val w = XpWriter(3)
        w.writeByte(PktType.LEAVE)
        w.writeShort(playerId)
        return w.toByteArray()
    }

    /**
     * PKT_SHUTDOWN packet: `0x13`
     *
     * Tells all clients the server is shutting down.
     */
    fun shutdown(): ByteArray {
        val w = XpWriter(1)
        w.writeByte(PktType.SHUTDOWN)
        return w.toByteArray()
    }

    /**
     * PKT_MESSAGE packet: `0x05 | NUL-terminated message`
     *
     * Sends a server-to-client chat message.
     */
    fun message(text: String): ByteArray {
        val w = XpWriter(2 + text.length)
        w.writeByte(PktType.MESSAGE)
        w.writeString(text, XP_MSG_LEN)
        return w.toByteArray()
    }

    /**
     * PKT_TALK_ACK packet: `0x34 | int32 seqNum`
     *
     * Acknowledges receipt of a PKT_TALK from the client.
     */
    fun talkAck(seqNum: Int): ByteArray {
        val w = XpWriter(5)
        w.writeByte(PktType.TALK_ACK)
        w.writeInt(seqNum)
        return w.toByteArray()
    }

    // -----------------------------------------------------------------------
    // Game-frame packets (M5 — sent every tick to PLAYING clients)
    // -----------------------------------------------------------------------

    /**
     * PKT_START packet: `0x06 | int32 frameLoop | int32 lastKeyChange`
     *
     * Sent at the start of each frame update.  The client uses [frameLoop] to
     * detect missed frames, and [lastKeyChange] to know which keyboard event
     * the server has processed.
     */
    fun start(
        frameLoop: Int,
        lastKeyChange: Int,
    ): ByteArray {
        val w = XpWriter(9)
        w.writeByte(PktType.START)
        w.writeInt(frameLoop)
        w.writeInt(lastKeyChange)
        return w.toByteArray()
    }

    /**
     * PKT_END packet: `0x07 | int32 frameLoop`
     *
     * Sent at the end of each frame update to signal the client that all
     * frame data has been delivered.
     */
    fun end(frameLoop: Int): ByteArray {
        val w = XpWriter(5)
        w.writeByte(PktType.END)
        w.writeInt(frameLoop)
        return w.toByteArray()
    }

    /**
     * PKT_SELF packet — ship state for the owning client.
     *
     * Wire format (version ≥ 0x4203):
     * ```
     * %c                              type = PKT_SELF (8)
     * %hd %hd %hd %hd %c             posX, posY (pixels), velX, velY (int), dir
     * %c %c %c                        power, turnspeed, turnresistance*255
     * %hd %hd %c %c                   lockId, lockDist, lockDir, check
     * %c %hd %hd                      fuel.current, fuel.sum (int), fuel.max (int)
     * %hd %hd %c                      viewWidth, viewHeight, debrisColors
     * %c %c                           status, autopilotLight
     * ```
     * Followed by PKT_SELF_ITEMS and PKT_MODIFIERS (omitted until M6).
     *
     * @param posX         Ship X position in pixels.
     * @param posY         Ship Y position in pixels.
     * @param velX         Velocity X (integer, pixels/tick).
     * @param velY         Velocity Y (integer, pixels/tick).
     * @param dir          Ship direction (0..RES-1).
     * @param power        Engine power (rounded to int).
     * @param turnspeed    Turn speed (rounded to int).
     * @param turnresistance Turn resistance scaled to 0..255.
     * @param lockId       Locked target player ID, or 0 if none.
     * @param lockDist     Distance to lock target in pixels, or 0.
     * @param lockDir      Direction to lock target (0..RES-1), or 0.
     * @param check        Race checkpoint index.
     * @param fuelCurrent  Active tank index.
     * @param fuelSum      Total fuel (rounded to int).
     * @param fuelMax      Maximum fuel (rounded to int).
     * @param viewWidth    Client view width in pixels.
     * @param viewHeight   Client view height in pixels.
     * @param debrisColors Debris colour settings.
     * @param status       Player status bits (uint8).
     * @param autopilotLight Autopilot indicator (0 or 1).
     */
    fun self(
        posX: Int,
        posY: Int,
        velX: Int,
        velY: Int,
        dir: Int,
        power: Int,
        turnspeed: Int,
        turnresistance: Int,
        lockId: Int = 0,
        lockDist: Int = 0,
        lockDir: Int = 0,
        check: Int = 0,
        fuelCurrent: Int = 0,
        fuelSum: Int = 0,
        fuelMax: Int = 0,
        viewWidth: Int = 800,
        viewHeight: Int = 600,
        debrisColors: Int = 0,
        status: Int = 0,
        autopilotLight: Int = 0,
    ): ByteArray {
        val w = XpWriter(31)
        w.writeByte(PktType.SELF)
        w.writeShort(posX)
        w.writeShort(posY)
        w.writeShort(velX)
        w.writeShort(velY)
        w.writeByte(dir)
        w.writeByte(power)
        w.writeByte(turnspeed)
        w.writeByte(turnresistance)
        w.writeShort(lockId)
        w.writeShort(lockDist)
        w.writeByte(lockDir)
        w.writeByte(check)
        w.writeByte(fuelCurrent)
        w.writeShort(fuelSum)
        w.writeShort(fuelMax)
        w.writeShort(viewWidth)
        w.writeShort(viewHeight)
        w.writeByte(debrisColors)
        w.writeByte(status)
        w.writeByte(autopilotLight)
        return w.toByteArray()
    }

    /**
     * PKT_SCORE packet (float-score variant for version ≥ 0x4500):
     * `0x1D | int16 id | int32 score*100 | int16 life | char mychar | char allchar`
     *
     * @param id       Player id.
     * @param score    Score as a double (encoded × 100 as int32).
     * @param lives    Remaining lives (deaths count).
     * @param myChar   The player's character token (e.g. letter).
     * @param allChar  Alliance character (' ' if none).
     */
    fun score(
        id: Int,
        score: Double,
        lives: Int,
        myChar: Char = ' ',
        allChar: Char = ' ',
    ): ByteArray {
        val w = XpWriter(11)
        w.writeByte(PktType.SCORE)
        w.writeShort(id)
        w.writeInt((score * 100).roundToInt())
        w.writeShort(lives)
        w.writeByte(myChar.code)
        w.writeByte(allChar.code)
        return w.toByteArray()
    }
}
