package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// XPilot-NG wire encoding helpers
// ---------------------------------------------------------------------------
//
// The XPilot-NG network layer uses a simple big-endian binary encoding defined
// by Packet_printf / Packet_scanf in net.c.  This file provides pure-Kotlin
// equivalents that operate on ByteArray without any JVM-specific APIs.
//
// Format specifier summary (from net.c):
//   %c   → 1 byte  (unsigned, stored as Int/Byte)
//   %hd  → 2 bytes big-endian signed short
//   %hu  → 2 bytes big-endian unsigned short
//   %d   → 4 bytes big-endian signed int
//   %u   → 4 bytes big-endian unsigned int (read as Int in Kotlin, wraps on overflow)
//   %ld  → 4 bytes big-endian signed long (C long = 32-bit)
//   %lu  → 4 bytes big-endian unsigned long
//   %s   → NUL-terminated string, max MAX_CHARS (80) bytes including NUL
//              (const.h: #define MAX_CHARS 80; used for nick, talk, modifier, etc.)
//   %S   → NUL-terminated string, max MSG_LEN (256) bytes including NUL
//              (const.h: #define MSG_LEN 256; used for chat messages, MOTD, etc.)

/**
 * Maximum buffer size for `%s` strings (= `MAX_CHARS` in `common/const.h`).
 *
 * Used for: talk strings, nick/user names, modifier bank strings.
 * Value: 80 (includes the NUL terminator, so max visible chars = 79).
 *
 * NOTE: `MAX_NAME_LEN = 16` in `pack.h` is a *contact-layer* constant for
 * the UDP contact handshake; it is NOT the game-layer string limit.
 */
const val XP_MAX_CHARS: Int = 80

/**
 * Maximum buffer size for `%S` strings (= `MSG_LEN` in `common/const.h`).
 *
 * Used for: PKT_MESSAGE (server→client chat), ship-shape uploads, MOTD lines.
 * Value: 256 (includes the NUL terminator).
 */
const val XP_MSG_LEN: Int = 256

// ---------------------------------------------------------------------------
// XpReader — sequential big-endian reader over a ByteArray
// ---------------------------------------------------------------------------

/**
 * Sequential big-endian binary reader over a [ByteArray].
 *
 * Mirrors the read side of Packet_scanf.  All reads advance [pos].
 * If the buffer is exhausted a [BufferUnderflowException] is thrown so the
 * caller can catch decode failures cleanly.
 */
class XpReader(
    private val data: ByteArray,
    startPos: Int = 0,
) {
    var pos: Int = startPos
        private set

    val remaining: Int get() = data.size - pos
    val isExhausted: Boolean get() = pos >= data.size

    /** Read 1 unsigned byte as Int (0..255). */
    fun readByte(): Int {
        checkRemaining(1)
        return data[pos++].toInt() and 0xFF
    }

    /** Read 2-byte big-endian signed short. */
    fun readShort(): Int {
        checkRemaining(2)
        val hi = data[pos++].toInt() and 0xFF
        val lo = data[pos++].toInt() and 0xFF
        val unsigned = (hi shl 8) or lo
        // sign-extend from 16 bits
        return if (unsigned >= 0x8000) unsigned - 0x10000 else unsigned
    }

    /** Read 2-byte big-endian unsigned short (returned as Int 0..65535). */
    fun readUShort(): Int {
        checkRemaining(2)
        val hi = data[pos++].toInt() and 0xFF
        val lo = data[pos++].toInt() and 0xFF
        return (hi shl 8) or lo
    }

    /** Read 4-byte big-endian signed int. */
    fun readInt(): Int {
        checkRemaining(4)
        return ((data[pos++].toInt() and 0xFF) shl 24) or
            ((data[pos++].toInt() and 0xFF) shl 16) or
            ((data[pos++].toInt() and 0xFF) shl 8) or
            (data[pos++].toInt() and 0xFF)
    }

    /** Read 4-byte big-endian unsigned int (returned as Int, wraps negative). */
    fun readUInt(): Int = readInt()

    /**
     * Read a NUL-terminated string (C-string).
     * @param maxBytes Maximum number of bytes to scan (including the NUL terminator).
     *                 Matching Packet_scanf semantics: after [maxBytes] we stop.
     */
    fun readString(maxBytes: Int = XP_MAX_CHARS): String {
        val sb = StringBuilder()
        var count = 0
        while (count < maxBytes) {
            if (pos >= data.size) break
            val b = data[pos++].toInt() and 0xFF
            count++
            if (b == 0) break
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    /** Read exactly [n] raw bytes into a new ByteArray. */
    fun readBytes(n: Int): ByteArray {
        checkRemaining(n)
        val result = data.copyOfRange(pos, pos + n)
        pos += n
        return result
    }

    /** Skip [n] bytes. */
    fun skip(n: Int) {
        checkRemaining(n)
        pos += n
    }

    private fun checkRemaining(n: Int) {
        if (pos + n > data.size) {
            throw XpBufferException("XpReader underflow: need $n bytes but only $remaining remain at pos $pos")
        }
    }
}

// ---------------------------------------------------------------------------
// XpWriter — sequential big-endian writer into a growable buffer
// ---------------------------------------------------------------------------

/**
 * Sequential big-endian binary writer.
 *
 * Mirrors the write side of Packet_printf.  Call [toByteArray] when done.
 * Uses a [ByteArray] + write cursor internally to avoid boxing individual bytes.
 */
class XpWriter(
    initialCapacity: Int = 64,
) {
    private var buf = ByteArray(initialCapacity)
    private var cursor = 0

    private fun ensureCapacity(needed: Int) {
        val required = cursor + needed
        if (required > buf.size) {
            val newSize = maxOf(buf.size * 2, required)
            buf = buf.copyOf(newSize)
        }
    }

    /** Current number of bytes written. */
    val size: Int get() = cursor

    /** Write 1 byte. */
    fun writeByte(v: Int) {
        ensureCapacity(1)
        buf[cursor++] = (v and 0xFF).toByte()
    }

    /** Write 2-byte big-endian short (signed or unsigned, same bits). */
    fun writeShort(v: Int) {
        ensureCapacity(2)
        buf[cursor++] = ((v ushr 8) and 0xFF).toByte()
        buf[cursor++] = (v and 0xFF).toByte()
    }

    /** Write 4-byte big-endian int. */
    fun writeInt(v: Int) {
        ensureCapacity(4)
        buf[cursor++] = ((v ushr 24) and 0xFF).toByte()
        buf[cursor++] = ((v ushr 16) and 0xFF).toByte()
        buf[cursor++] = ((v ushr 8) and 0xFF).toByte()
        buf[cursor++] = (v and 0xFF).toByte()
    }

    /**
     * Write a NUL-terminated C-string.
     * Truncates to [maxBytes]-1 characters and always appends NUL.
     */
    fun writeString(
        s: String,
        maxBytes: Int = XP_MAX_CHARS,
    ) {
        var written = 0
        for (ch in s) {
            if (written >= maxBytes - 1) break
            ensureCapacity(1)
            buf[cursor++] = ch.code.toByte()
            written++
        }
        ensureCapacity(1)
        buf[cursor++] = 0 // NUL terminator
    }

    /** Write a raw byte array. */
    fun writeBytes(bytes: ByteArray) {
        ensureCapacity(bytes.size)
        bytes.copyInto(buf, cursor)
        cursor += bytes.size
    }

    /** Return all written bytes as a new ByteArray. */
    fun toByteArray(): ByteArray = buf.copyOf(cursor)
}

// ---------------------------------------------------------------------------
// Exception
// ---------------------------------------------------------------------------

/** Thrown when an XpReader encounters a buffer underflow or malformed data. */
class XpBufferException(
    message: String,
) : Exception(message)
