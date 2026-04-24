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
        w.writeShort(data.size) // %hd len
        w.writeInt(offset) // %ld rel_off
        w.writeInt(frameLoop) // %ld main_loops
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
     * Followed by PKT_SELF_ITEMS and PKT_MODIFIERS (both emitted every frame).
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

    /**
     * PKT_PLAYER packet — announces a new player to all clients.
     *
     * Wire format:
     * ```
     * byte   : PKT_PLAYER (14)
     * int16  : id
     * int16  : team
     * byte   : myChar
     * string : nick (NUL-terminated, max XP_MAX_CHARS)
     * ```
     *
     * Sent to every PLAYING session when a new player joins, so existing
     * clients know to render the new ship.
     *
     * @param id     The new player's session id.
     * @param team   Team number (0 = free-for-all).
     * @param myChar Player's char token.
     * @param nick   Player's nick name.
     */
    fun player(
        id: Int,
        team: Int,
        myChar: Char = ' ',
        nick: String,
    ): ByteArray {
        val w = XpWriter(6 + nick.length)
        w.writeByte(PktType.PLAYER)
        w.writeShort(id)
        w.writeShort(team)
        w.writeByte(myChar.code)
        w.writeString(nick, XP_MAX_CHARS)
        return w.toByteArray()
    }

    /**
     * PKT_SELF_ITEMS packet — item counts for the owning player.
     *
     * Wire format:
     * ```
     * byte      : PKT_SELF_ITEMS (11)
     * byte[NUM] : item counts (one byte per item type, clamped to 0..255)
     * ```
     *
     * NUM = [org.lambertland.kxpilot.common.Item.NUM_ITEMS].
     * Sent immediately after PKT_SELF every frame.
     *
     * @param items Item count array (indexed by [org.lambertland.kxpilot.common.Item]).
     */
    fun selfItems(items: IntArray): ByteArray {
        val w = XpWriter(1 + items.size)
        w.writeByte(PktType.SELF_ITEMS)
        for (count in items) w.writeByte(count.coerceIn(0, 255))
        return w.toByteArray()
    }

    /**
     * PKT_MODIFIERS packet — active modifier bank for the owning player.
     *
     * Wire format:
     * ```
     * byte : PKT_MODIFIERS (70)
     * byte : mini (modifier bank mini-flags)
     * byte : nuclear
     * byte : cluster
     * byte : implosion
     * byte : velocity
     * byte : spread
     * byte : front
     * byte : laser
     * byte : target
     * byte : itempf
     * ```
     *
     * Values come from the active [org.lambertland.kxpilot.server.Modifiers] bank.
     * Sent immediately after PKT_SELF_ITEMS every frame.
     *
     * @param mini        Mini modifier flag.
     * @param nuclear     Nuclear modifier flag.
     * @param cluster     Cluster modifier flag.
     * @param implosion   Implosion modifier flag.
     * @param velocity    Velocity modifier value.
     * @param spread      Spread modifier value.
     * @param front       Front modifier flag.
     * @param laser       Laser modifier flag.
     * @param target      Target modifier flag.
     * @param itempf      Item modifier flag.
     */
    fun modifiers(
        mini: Int = 0,
        nuclear: Int = 0,
        cluster: Int = 0,
        implosion: Int = 0,
        velocity: Int = 0,
        spread: Int = 0,
        front: Int = 0,
        laser: Int = 0,
        target: Int = 0,
        itempf: Int = 0,
    ): ByteArray {
        val w = XpWriter(11)
        w.writeByte(PktType.MODIFIERS)
        w.writeByte(mini)
        w.writeByte(nuclear)
        w.writeByte(cluster)
        w.writeByte(implosion)
        w.writeByte(velocity)
        w.writeByte(spread)
        w.writeByte(front)
        w.writeByte(laser)
        w.writeByte(target)
        w.writeByte(itempf)
        return w.toByteArray()
    }

    // -----------------------------------------------------------------------
    // (No private packet type constants — SELF_ITEMS and MODIFIERS are in PktType)
    // -----------------------------------------------------------------------

    /**
     * PKT_BALL packet — position and carrier-player id of one live ball.
     *
     * Mirrors C `netserver.c Send_ball`.  Two wire formats exist depending on
     * the `F_BALLSTYLE` capability negotiated with the client (version ≥ 0x4F14):
     *
     * **Long form** (`hasBallStyle = true`, 8 bytes — default for modern clients):
     * ```
     * byte   : PKT_BALL (17)
     * int16  : posX  (pixels, CLICK_TO_PIXEL(pos.cx))
     * int16  : posY  (pixels, CLICK_TO_PIXEL(pos.cy))
     * int16  : id    (carrier player id, or NO_ID = -1 if ball is free)
     * byte   : style (BallObject.ballStyle, or 0xFF when options.ballStyles is false)
     * ```
     *
     * **Short form** (`hasBallStyle = false`, 7 bytes — for clients < 0x4F14):
     * ```
     * byte   : PKT_BALL (17)
     * int16  : posX
     * int16  : posY
     * int16  : id
     * ```
     *
     * **`id` semantics:** this is `OBJECT_BASE.id` on the ball, which stores the
     * **player id of whoever is carrying the ball** (i.e. `pl->id` from C).
     * It is set to `NO_ID (-1)` when no player holds the ball.  It is *not* a
     * stable ball index.
     *
     * @param posX        Ball X position in pixels.
     * @param posY        Ball Y position in pixels.
     * @param id          Carrier player id, or -1 (`NO_ID`) if the ball is free.
     * @param style       Ball poly-style byte (ignored when [hasBallStyle] is false).
     * @param hasBallStyle True (default) for clients with `F_BALLSTYLE` (version ≥ 0x4F14).
     *                     False sends the 7-byte short form without the style byte.
     */
    fun ball(
        posX: Int,
        posY: Int,
        id: Int,
        style: Int,
        hasBallStyle: Boolean = true,
    ): ByteArray =
        if (hasBallStyle) {
            val w = XpWriter(8)
            w.writeByte(PktType.BALL)
            w.writeShort(posX)
            w.writeShort(posY)
            w.writeShort(id)
            w.writeByte(style)
            w.toByteArray()
        } else {
            val w = XpWriter(7)
            w.writeByte(PktType.BALL)
            w.writeShort(posX)
            w.writeShort(posY)
            w.writeShort(id)
            w.toByteArray()
        }

    /**
     * PKT_MOTD packet — a chunk of the server's message-of-the-day.
     *
     * Wire format (from C `netserver.c Send_motd`):
     * ```
     * byte   : PKT_MOTD (0x1A = 26)
     * int32  : totalLen  — total byte length of the full MOTD text (including NUL)
     * int32  : offset    — byte offset of this chunk in the full text
     * string : NUL-terminated chunk text
     * ```
     *
     * @param offset  Byte offset of this chunk in the full MOTD text.
     * @param text    The MOTD text (sent as a single chunk here).
     */
    fun motd(
        offset: Int,
        text: String,
    ): ByteArray {
        val totalLen = text.length + 1 // include NUL terminator
        val w = XpWriter(9 + text.length + 1)
        w.writeByte(PktType.MOTD)
        w.writeInt(totalLen)
        w.writeInt(offset)
        w.writeString(text)
        return w.toByteArray()
    }
}
