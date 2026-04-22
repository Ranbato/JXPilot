package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// ClientSession — per-client connection state machine
// ---------------------------------------------------------------------------
//
// Models the lifecycle of a single connected client from first contact through
// active play, mirroring the connection_t / CONN_* state machine in the C
// source (connection.h / netserver.c).
//
// States:
//   FREE       — slot unused
//   LISTENING  — contact received; waiting for PKT_VERIFY on loginPort
//   SETUP      — PKT_VERIFY received; waiting for PKT_PLAY
//   LOGIN      — PKT_PLAY received; server setting up game slot (M3)
//   READY      — game slot ready; waiting for first keyboard packet
//   PLAYING    — fully in game
//   DRAIN      — graceful disconnect in progress (draining reliable queue)

// ---------------------------------------------------------------------------
// ConnState enum
// ---------------------------------------------------------------------------

enum class ConnState {
    FREE,
    LISTENING,
    SETUP,
    LOGIN,
    READY,
    PLAYING,
    DRAIN,
}

// ---------------------------------------------------------------------------
// ClientSession
// ---------------------------------------------------------------------------

/**
 * Per-client connection state held by the server while a client is connected.
 *
 * This is a mutable class intentionally: it is always accessed from within
 * the server's single coroutine/dispatcher, so no extra synchronisation is
 * needed.  If the threading model changes this will need a review.
 *
 * @param id        Slot index (0-based) in the server's connection table.
 * @param addr      Source IP address string from the first contact datagram.
 * @param loginPort The per-player UDP port this session listens on.
 * @param magic     The 32-bit magic word sent back to the client in PKT_MAGIC.
 */
class ClientSession(
    val id: Int,
    val addr: String,
    val loginPort: Int,
    val magic: Int,
) {
    companion object {
        /** Timeout for LISTENING state (ms): how long to wait for PKT_VERIFY. */
        const val LISTEN_TIMEOUT_MS: Long = 4_000L

        /** Timeout for SETUP state (ms): how long to wait for PKT_PLAY. */
        const val SETUP_TIMEOUT_MS: Long = 15_000L

        /** Timeout for LOGIN state (ms): how long to allow server setup. */
        const val LOGIN_TIMEOUT_MS: Long = 30_000L

        /** Maximum bytes held in the reliable-retransmit buffer. */
        const val RELIABLE_BUF_SIZE: Int = 65_536
    }

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------

    var user: String = ""
    var nick: String = ""
    var team: Int = -1
    var clientVersion: Int = 0

    // -----------------------------------------------------------------------
    // Connection state
    // -----------------------------------------------------------------------

    var state: ConnState = ConnState.LISTENING

    /** Monotonic timestamp (ms) when this session entered the current [state]. */
    var stateEnteredMs: Long = 0L
        internal set

    // -----------------------------------------------------------------------
    // View / display preferences (filled by PKT_DISPLAY)
    // -----------------------------------------------------------------------

    var viewWidth: Int = 800
    var viewHeight: Int = 600
    var debrisColors: Int = 0
    var sparkRand: Int = 0

    // -----------------------------------------------------------------------
    // Reliable data stream
    // -----------------------------------------------------------------------

    /**
     * Ring buffer holding unacknowledged reliable data.
     * New data is appended at [reliableWritePos]; the client acks up to
     * [reliableAckPos].
     */
    private val reliableBuffer: ByteArray = ByteArray(RELIABLE_BUF_SIZE)

    /**
     * Absolute byte offset of the *next* byte to be written into
     * [reliableBuffer].  Wraps at [RELIABLE_BUF_SIZE].
     */
    var reliableWritePos: Int = 0
        private set

    /**
     * Highest byte offset the *client* has acknowledged (from PKT_ACK).
     */
    var reliableAckPos: Int = 0
        private set

    /** How many unacknowledged bytes are currently queued. */
    val reliablePending: Int get() = reliableWritePos - reliableAckPos

    // -----------------------------------------------------------------------
    // Keyboard tracking
    // -----------------------------------------------------------------------

    var lastKeyChangeId: Int = -1

    // -----------------------------------------------------------------------
    // Lifecycle handlers
    // -----------------------------------------------------------------------

    /**
     * Process a PKT_VERIFY packet.
     *
     * Validates the user/nick from the packet, transitions state to SETUP,
     * and returns the two server→client packets to send back:
     * 1. `PKT_REPLY | PKT_VERIFY | SUCCESS`
     * 2. `PKT_RELIABLE( PKT_MAGIC | magic )` — queued on the reliable channel
     *
     * @return Pair of byte arrays: (replyPacket, reliableMagicPacket).
     *         Both must be sent to the client on [loginPort].
     */
    fun handleVerify(pkt: XpPacket.Verify): Pair<ByteArray, ByteArray> {
        user = pkt.user
        nick = pkt.nick
        state = ConnState.SETUP

        val replyBytes = PacketEncoder.reply(PktType.VERIFY, PktType.SUCCESS)

        // Build reliable PKT_MAGIC payload and queue it
        val magicPayload = PacketEncoder.magic(magic)
        queueReliable(magicPayload)

        // Build the PKT_RELIABLE wrapper for the magic payload
        val reliableBytes = PacketEncoder.reliable(reliableAckPos, magicPayload)

        return replyBytes to reliableBytes
    }

    /**
     * Process a PKT_ACK from the client.
     *
     * Advances [reliableAckPos] if [pkt].bytePos is ahead of the current
     * position and within the range of written data.
     */
    fun handleAck(pkt: XpPacket.Ack) {
        val newAck = pkt.bytePos
        if (newAck > reliableAckPos && newAck <= reliableWritePos) {
            reliableAckPos = newAck
        }
    }

    /**
     * Process a PKT_PLAY from the client.
     *
     * Transitions state to LOGIN (server will complete game-slot setup in M3).
     * Returns `true` if the transition was valid (i.e. state was SETUP).
     */
    fun handlePlay(): Boolean {
        if (state != ConnState.SETUP) return false
        state = ConnState.LOGIN
        return true
    }

    /**
     * Process a PKT_DISPLAY from the client.
     */
    fun handleDisplay(pkt: XpPacket.Display) {
        viewWidth = pkt.viewWidth
        viewHeight = pkt.viewHeight
        debrisColors = pkt.debrisColors
        sparkRand = pkt.sparkRand
    }

    // -----------------------------------------------------------------------
    // Reliable data helpers
    // -----------------------------------------------------------------------

    /**
     * Append [data] to the reliable-transmit buffer.
     *
     * @throws IllegalStateException if the buffer would overflow.
     */
    fun queueReliable(data: ByteArray) {
        if (reliablePending + data.size > RELIABLE_BUF_SIZE) {
            throw IllegalStateException("Reliable buffer overflow for session $id (${data.size} bytes, $reliablePending pending)")
        }
        for (b in data) {
            reliableBuffer[reliableWritePos % RELIABLE_BUF_SIZE] = b
            reliableWritePos++
        }
    }

    /**
     * Build the next PKT_RELIABLE retransmit packet for any un-acked data.
     *
     * Returns `null` if everything has been acknowledged.
     * The returned packet covers all pending bytes in one shot (the caller
     * may need to fragment if the datagram would be too large — deferred to M3).
     */
    fun buildReliablePacket(): ByteArray? {
        if (reliablePending <= 0) return null
        val pendingLen = reliablePending
        val payload = ByteArray(pendingLen)
        for (i in 0 until pendingLen) {
            payload[i] = reliableBuffer[(reliableAckPos + i) % RELIABLE_BUF_SIZE]
        }
        return PacketEncoder.reliable(reliableAckPos, payload)
    }

    // -----------------------------------------------------------------------
    // Timeout checking
    // -----------------------------------------------------------------------

    /**
     * Returns `true` if this session has been in its current state longer
     * than the allowed timeout.
     *
     * @param nowMs Current monotonic time in milliseconds.
     */
    fun isTimedOut(nowMs: Long): Boolean {
        val elapsed = nowMs - stateEnteredMs
        return when (state) {
            ConnState.LISTENING -> elapsed > LISTEN_TIMEOUT_MS
            ConnState.SETUP -> elapsed > SETUP_TIMEOUT_MS
            ConnState.LOGIN -> elapsed > LOGIN_TIMEOUT_MS
            else -> false
        }
    }

    override fun toString(): String = "ClientSession(id=$id, addr=$addr, state=$state, user=$user, nick=$nick)"
}
