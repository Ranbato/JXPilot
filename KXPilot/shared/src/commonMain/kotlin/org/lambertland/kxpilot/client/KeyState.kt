package org.lambertland.kxpilot.client

import org.lambertland.kxpilot.common.Key

// ---------------------------------------------------------------------------
// KeyState — live key-press bitvector for the XPilot shared-key protocol
// ---------------------------------------------------------------------------

/**
 * Holds the current pressed/released state for all [Key.NUM_KEYS] shared keys
 * (those with ordinal < [Key.NUM_KEYS]).  Client-only keys (ordinal ≥ NUM_KEYS)
 * are stored separately and never serialised into the server packet.
 *
 * Thread-safety: not thread-safe.  Mutate from a single coroutine/thread only
 * (the UI event thread) and read from the physics tick on the same thread.
 */
class KeyState {
    /** Bit state for server-shared keys (indices 0 until [Key.NUM_KEYS]). */
    val current: BooleanArray = BooleanArray(Key.NUM_KEYS)

    /** Previous tick's state — used to detect edge events (press vs. hold). */
    private val previous: BooleanArray = BooleanArray(Key.NUM_KEYS)

    // -----------------------------------------------------------------------
    // Mutation helpers
    // -----------------------------------------------------------------------

    /** Mark key [k] as pressed (idempotent). Only affects shared keys. */
    fun press(k: Key) {
        if (k.ordinal < Key.NUM_KEYS) current[k.ordinal] = true
    }

    /** Mark key [k] as released (idempotent). Only affects shared keys. */
    fun release(k: Key) {
        if (k.ordinal < Key.NUM_KEYS) current[k.ordinal] = false
    }

    /** Returns true while key [k] is held down this tick. */
    fun isDown(k: Key): Boolean = k.ordinal < Key.NUM_KEYS && current[k.ordinal]

    /** Returns true only on the first tick a key transitions from up → down. */
    fun justPressed(k: Key): Boolean = k.ordinal < Key.NUM_KEYS && current[k.ordinal] && !previous[k.ordinal]

    /**
     * Advance the snapshot: copy [current] into [previous].
     * Call once per physics tick, after reading key state.
     */
    fun advanceTick() {
        current.copyInto(previous)
    }

    // -----------------------------------------------------------------------
    // XPilot wire-protocol serialisation
    // -----------------------------------------------------------------------

    /**
     * Serialise the 72-bit ([Key.NUM_KEYS]) key bitvector into 9 bytes, LSB-first,
     * matching the XPilot network key-packet format from `common/packet.h`.
     *
     * Bit layout:
     *   byte[i/8] bit (i%8) = current[i], for i in 0 until NUM_KEYS.
     *
     * The returned array is freshly allocated on each call and safe to hand to
     * a network write without further copying.
     */
    fun toPacket(): ByteArray {
        val bytes = ByteArray(KEY_PACKET_BYTES)
        for (i in 0 until Key.NUM_KEYS) {
            if (current[i]) {
                bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (i % 8))).toByte()
            }
        }
        return bytes
    }

    /**
     * Deserialise a key-packet byte array (received from server or replayed)
     * back into [current].  Silently ignores arrays shorter than [KEY_PACKET_BYTES].
     */
    fun fromPacket(packet: ByteArray) {
        val limit = minOf(packet.size * 8, Key.NUM_KEYS)
        for (i in 0 until limit) {
            current[i] = (packet[i / 8].toInt() ushr (i % 8)) and 1 != 0
        }
    }

    companion object {
        /** Byte count of the serialised key bitvector: ceil(NUM_KEYS / 8). */
        const val KEY_PACKET_BYTES: Int = (Key.NUM_KEYS + 7) / 8 // = 9
    }
}
