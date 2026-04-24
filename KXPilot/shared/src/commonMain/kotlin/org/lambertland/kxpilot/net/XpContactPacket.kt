package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// XPilot-NG contact-layer protocol
// ---------------------------------------------------------------------------
//
// The contact layer operates on the well-known port 15345.  A client sends a
// contact request to ask to join (or query) the server; the server replies
// with a status code and a per-player login port.
//
// Wire layout (from contact.c / pack.h):
//
//   Request:
//     uint32  magic   = VERSION2MAGIC(clientVersion)
//     string  user    (%s, max MAX_CHARS)
//     uint16  port    (client's preferred callback port — ignored by server)
//     uint8   packType
//     --- followed by pack-type-specific payload ---
//     ENTER_GAME_pack (0x00): string nick | string disp | string host | int32 team
//     ENTER_QUEUE_pack (0x01): (no extra payload)
//     REPORT_STATUS_pack (0x21): (no extra payload)
//     CONTACT_pack (0x31): (no extra payload)
//
//   Reply (ENTER_GAME_pack response):
//     uint32  magic   = VERSION2MAGIC(serverVersion)
//     uint8   packType  = ENTER_GAME_pack (0x00) [echo]
//     uint8   status    = SUCCESS/error code
//     uint16  loginPort  (per-player UDP socket the client should now connect to)

// ---------------------------------------------------------------------------
// Magic-word helpers  (pack.h)
// ---------------------------------------------------------------------------

/** The low-16-bit magic sentinel embedded in every contact packet. */
const val XP_MAGIC_WORD: Int = 0xF4ED

/** Pack a version into a 32-bit magic word: `(version << 16) | MAGIC_WORD`. */
fun version2magic(version: Int): Int = ((version and 0xFFFF) shl 16) or XP_MAGIC_WORD

/** Extract the version from a 32-bit magic word: `(magic >> 16) & 0xFFFF`. */
fun magic2version(magic: Int): Int = (magic ushr 16) and 0xFFFF

/** Validate that a 32-bit magic word carries the expected sentinel. */
fun isValidMagic(magic: Int): Boolean = (magic and 0xFFFF) == XP_MAGIC_WORD

// ---------------------------------------------------------------------------
// Contact pack-type constants  (pack.h)
// ---------------------------------------------------------------------------

object ContactPackType {
    const val ENTER_GAME: Int = 0x00
    const val ENTER_QUEUE: Int = 0x01
    const val REPLY: Int = 0x10
    const val REPORT_STATUS: Int = 0x21
    const val CONTACT: Int = 0x31
}

// ---------------------------------------------------------------------------
// Contact error/status codes  (pack.h)
// ---------------------------------------------------------------------------

object ContactStatus {
    const val SUCCESS: Int = 0x00
    const val E_GAME_FULL: Int = 0x02
    const val E_IN_USE: Int = 0x08
    const val E_VERSION: Int = 0x0C
}

// ---------------------------------------------------------------------------
// Sealed hierarchy for decoded contact requests
// ---------------------------------------------------------------------------

/**
 * A decoded contact-layer request from a client.
 *
 * All subtypes carry [user] (the client's login name), [clientVersion]
 * (extracted from the magic word), and [clientPort] (the port the client
 * declared — informational, not used for reply).
 */
sealed class XpContactRequest {
    abstract val user: String
    abstract val clientVersion: Int
    abstract val clientPort: Int

    /**
     * ENTER_GAME_pack (0x00) — the client wants to join the game.
     *
     * @param nick   Display name chosen by the player.
     * @param disp   X11 display string (legacy; ignored by this server).
     * @param host   Hostname declared by the client (legacy; use srcAddr instead).
     * @param team   Requested team number (-1 = no preference).
     */
    data class EnterGame(
        override val user: String,
        override val clientVersion: Int,
        override val clientPort: Int,
        val nick: String,
        val disp: String,
        val host: String,
        val team: Int,
    ) : XpContactRequest()

    /**
     * ENTER_QUEUE_pack (0x01) — the client wants to enter the spectator queue.
     */
    data class EnterQueue(
        override val user: String,
        override val clientVersion: Int,
        override val clientPort: Int,
    ) : XpContactRequest()

    /**
     * REPORT_STATUS_pack (0x21) — the client is polling server status.
     */
    data class ReportStatus(
        override val user: String,
        override val clientVersion: Int,
        override val clientPort: Int,
    ) : XpContactRequest()

    /**
     * CONTACT_pack (0x31) — a keep-alive / ping from a known client.
     */
    data class Contact(
        override val user: String,
        override val clientVersion: Int,
        override val clientPort: Int,
    ) : XpContactRequest()
}

// ---------------------------------------------------------------------------
// XpContactDecoder — ByteArray → XpContactRequest?
// ---------------------------------------------------------------------------

/**
 * Decodes a raw contact-port datagram into an [XpContactRequest].
 *
 * Returns `null` (and does not throw) for datagrams that are syntactically
 * valid but carry an unrecognised pack type.  Throws [XpBufferException] for
 * truncated / structurally invalid datagrams so the caller can count errors.
 *
 * @param data    Raw bytes of the UDP datagram.
 * @param srcAddr Source IP address string (for logging; not stored in result).
 */
object XpContactDecoder {
    fun decode(
        data: ByteArray,
        srcAddr: String = "",
    ): XpContactRequest? {
        if (data.size < 7) throw XpBufferException("Contact datagram too short: ${data.size} bytes from $srcAddr")
        val r = XpReader(data)

        val magic = r.readInt()
        if (!isValidMagic(magic)) {
            throw XpBufferException("Bad magic 0x${magic.toString(16)} from $srcAddr")
        }
        val clientVersion = magic2version(magic)

        val user = r.readString(XP_MAX_CHARS)
        val clientPort = r.readUShort()
        val packType = r.readByte()

        return when (packType) {
            ContactPackType.ENTER_GAME -> {
                val nick = r.readString(XP_MAX_CHARS)
                val disp = r.readString(XP_MAX_CHARS)
                val host = r.readString(XP_MAX_CHARS)
                val team = r.readInt()
                XpContactRequest.EnterGame(user, clientVersion, clientPort, nick, disp, host, team)
            }

            ContactPackType.ENTER_QUEUE -> {
                XpContactRequest.EnterQueue(user, clientVersion, clientPort)
            }

            ContactPackType.REPORT_STATUS -> {
                XpContactRequest.ReportStatus(user, clientVersion, clientPort)
            }

            ContactPackType.CONTACT -> {
                XpContactRequest.Contact(user, clientVersion, clientPort)
            }

            else -> {
                null
            } // unrecognised pack type
        }
    }
}

// ---------------------------------------------------------------------------
// XpContactEncoder — server → client contact replies
// ---------------------------------------------------------------------------

object XpContactEncoder {
    /**
     * Encode the server's reply to an ENTER_GAME or ENTER_QUEUE request.
     *
     * Wire layout: `uint32 magic | uint8 packType | uint8 status | uint16 loginPort`
     *
     * @param serverVersion  The server's protocol version (encoded in magic).
     * @param packType       Echo of the request pack type (ENTER_GAME or ENTER_QUEUE).
     * @param status         [ContactStatus.SUCCESS] or an error code.
     * @param loginPort      Per-player UDP port the client should connect to next.
     *                       Ignored when status != SUCCESS.
     */
    fun replyEnterGame(
        serverVersion: Int,
        packType: Int = ContactPackType.ENTER_GAME,
        status: Int,
        loginPort: Int,
    ): ByteArray {
        val w = XpWriter(8)
        w.writeInt(version2magic(serverVersion))
        w.writeByte(packType)
        w.writeByte(status)
        w.writeShort(loginPort)
        return w.toByteArray()
    }

    /**
     * Encode the server's reply to a REPORT_STATUS request.
     *
     * Wire layout:
     * ```
     * uint32  magic         = VERSION2MAGIC(serverVersion)
     * uint8   packType      = REPLY (0x10)
     * uint8   status        = SUCCESS (0x00)
     * uint16  loginPort     = 0 (not applicable for status replies)
     * string  serverName    (NUL-terminated, max XP_MAX_CHARS)
     * uint16  playerCount
     * string* playerNames   (one NUL-terminated string per player)
     * ```
     *
     * @param serverVersion  The server's protocol version.
     * @param serverName     Human-readable server name.
     * @param playerCount    Number of currently connected players.
     * @param playerNames    List of connected player names.
     */
    fun replyStatus(
        serverVersion: Int,
        serverName: String,
        playerCount: Int,
        playerNames: List<String>,
    ): ByteArray {
        val w = XpWriter(8 + serverName.length + 1 + 2 + playerNames.sumOf { it.length + 1 })
        w.writeInt(version2magic(serverVersion))
        w.writeByte(ContactPackType.REPLY)
        w.writeByte(ContactStatus.SUCCESS)
        w.writeShort(0) // loginPort = 0 for status replies
        w.writeString(serverName, XP_MAX_CHARS)
        w.writeShort(playerCount)
        for (name in playerNames) {
            w.writeString(name, XP_MAX_CHARS)
        }
        return w.toByteArray()
    }
}
