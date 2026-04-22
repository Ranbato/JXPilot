package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// PacketCodecTest — round-trip encode ↔ decode for all XpPacket variants
// ---------------------------------------------------------------------------

class PacketCodecTest {
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun roundTrip(pkt: XpPacket): XpPacket = PacketDecoder.decode(PacketEncoder.encode(pkt))

    // -----------------------------------------------------------------------
    // PKT_VERIFY
    // -----------------------------------------------------------------------

    @Test
    fun `verify round-trip`() {
        val original = XpPacket.Verify(user = "alice", nick = "Alice")
        val decoded = roundTrip(original)
        assertEquals(original, decoded)
    }

    @Test
    fun `verify with max-length strings truncated to MAX_CHARS`() {
        val longStr = "a".repeat(200)
        val encoded = PacketEncoder.encode(XpPacket.Verify(longStr, longStr))
        val decoded = PacketDecoder.decode(encoded) as XpPacket.Verify
        // XP_MAX_CHARS = 80, NUL included → at most 79 visible chars
        assertTrue(decoded.user.length <= XP_MAX_CHARS - 1)
        assertTrue(decoded.nick.length <= XP_MAX_CHARS - 1)
    }

    // -----------------------------------------------------------------------
    // PKT_PLAY / PKT_QUIT
    // -----------------------------------------------------------------------

    @Test
    fun `play round-trip`() {
        assertEquals(XpPacket.Play, roundTrip(XpPacket.Play))
    }

    @Test
    fun `quit round-trip`() {
        assertEquals(XpPacket.Quit, roundTrip(XpPacket.Quit))
    }

    // -----------------------------------------------------------------------
    // PKT_ACK
    // -----------------------------------------------------------------------

    @Test
    fun `ack round-trip zero`() {
        assertEquals(XpPacket.Ack(0), roundTrip(XpPacket.Ack(0)))
    }

    @Test
    fun `ack round-trip positive`() {
        assertEquals(XpPacket.Ack(12345), roundTrip(XpPacket.Ack(12345)))
    }

    @Test
    fun `ack round-trip large value`() {
        assertEquals(XpPacket.Ack(Int.MAX_VALUE), roundTrip(XpPacket.Ack(Int.MAX_VALUE)))
    }

    /**
     * Verify the exact PKT_ACK wire layout matches the C Receive_ack format:
     *   %c%ld%ld  →  type(1) | bytePos(4) | loopTimestamp(4)
     *
     * The C client sends this; the server reads bytePos and discards loopTimestamp.
     */
    @Test
    fun `ack wire layout is type plus two int32s`() {
        val bytes = PacketEncoder.encode(XpPacket.Ack(bytePos = 0x0000_0064))
        // 1 (type) + 4 (bytePos) + 4 (loopTimestamp=0) = 9 bytes
        assertEquals(9, bytes.size)
        assertEquals(PktType.ACK, bytes[0].toInt() and 0xFF)
        // bytePos = 100 = 0x00000064
        assertEquals(0x00, bytes[1].toInt() and 0xFF)
        assertEquals(0x00, bytes[2].toInt() and 0xFF)
        assertEquals(0x00, bytes[3].toInt() and 0xFF)
        assertEquals(0x64, bytes[4].toInt() and 0xFF)
        // loopTimestamp = 0
        assertEquals(0x00, bytes[5].toInt() and 0xFF)
        assertEquals(0x00, bytes[6].toInt() and 0xFF)
        assertEquals(0x00, bytes[7].toInt() and 0xFF)
        assertEquals(0x00, bytes[8].toInt() and 0xFF)
    }

    // -----------------------------------------------------------------------
    // PKT_KEYBOARD
    // -----------------------------------------------------------------------

    @Test
    fun `keyboard round-trip`() {
        val bitmap = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09)
        val original = XpPacket.Keyboard(keyChangeId = 42, keyBitmap = bitmap)
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun `keyboard all-zeros bitmap`() {
        val bitmap = ByteArray(XpPacket.Keyboard.KEYBOARD_SIZE)
        val original = XpPacket.Keyboard(keyChangeId = 0, keyBitmap = bitmap)
        assertEquals(original, roundTrip(original))
    }

    // -----------------------------------------------------------------------
    // PKT_TALK
    // -----------------------------------------------------------------------

    @Test
    fun `talk round-trip`() {
        val original = XpPacket.Talk(seqNum = 7, message = "hello")
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun `talk with empty message`() {
        val original = XpPacket.Talk(seqNum = 0, message = "")
        assertEquals(original, roundTrip(original))
    }

    /**
     * The C Receive_talk reads the message with %s (MAX_CHARS = 80 per const.h).
     * Messages longer than 79 chars must be truncated to 79 visible chars.
     */
    @Test
    fun `talk message truncated to MAX_CHARS - 1 visible chars`() {
        val longMsg = "a".repeat(200)
        val encoded = PacketEncoder.encode(XpPacket.Talk(seqNum = 1, message = longMsg))
        val decoded = PacketDecoder.decode(encoded) as XpPacket.Talk
        assertTrue(
            decoded.message.length <= XP_MAX_CHARS - 1,
            "Expected message length <= ${XP_MAX_CHARS - 1}, got ${decoded.message.length}",
        )
    }

    // -----------------------------------------------------------------------
    // PKT_DISPLAY
    // -----------------------------------------------------------------------

    @Test
    fun `display round-trip`() {
        val original = XpPacket.Display(viewWidth = 1280, viewHeight = 720, debrisColors = 3, sparkRand = 128)
        assertEquals(original, roundTrip(original))
    }

    // -----------------------------------------------------------------------
    // PKT_MOTD
    // -----------------------------------------------------------------------

    @Test
    fun `motd request round-trip`() {
        val original = XpPacket.MotdRequest(offset = 1024)
        assertEquals(original, roundTrip(original))
    }

    // -----------------------------------------------------------------------
    // XpPacket.Raw
    // -----------------------------------------------------------------------

    @Test
    fun `raw round-trip`() {
        val payload = byteArrayOf(0x11, 0x22, 0x33)
        val original = XpPacket.Raw(typeId = 0xFF, payload = payload)
        assertEquals(original, roundTrip(original))
    }

    // -----------------------------------------------------------------------
    // Edge cases — decode errors
    // -----------------------------------------------------------------------

    @Test
    fun `decode empty bytes throws`() {
        assertFailsWith<XpBufferException> {
            PacketDecoder.decode(byteArrayOf())
        }
    }

    @Test
    fun `decode truncated keyboard throws`() {
        // Only the type byte + 3 bytes of keyChangeId — not enough
        assertFailsWith<XpBufferException> {
            PacketDecoder.decode(byteArrayOf(PktType.KEYBOARD.toByte(), 0, 0, 0))
        }
    }

    /**
     * A PKT_ACK with only one int32 (missing the loopTimestamp) must throw
     * because the C wire format requires two int32s after the type byte.
     */
    @Test
    fun `decode truncated ack missing loop timestamp throws`() {
        assertFailsWith<XpBufferException> {
            // type(1) + bytePos(4) = 5 bytes — missing the 4-byte loopTimestamp
            PacketDecoder.decode(byteArrayOf(PktType.ACK.toByte(), 0, 0, 0, 42))
        }
    }

    // -----------------------------------------------------------------------
    // Server-originated factory methods (spot-check byte layout)
    // -----------------------------------------------------------------------

    @Test
    fun `reply packet layout`() {
        val bytes = PacketEncoder.reply(PktType.VERIFY, PktType.SUCCESS)
        assertEquals(3, bytes.size)
        assertEquals(PktType.REPLY, bytes[0].toInt() and 0xFF)
        assertEquals(PktType.VERIFY, bytes[1].toInt() and 0xFF)
        assertEquals(PktType.SUCCESS, bytes[2].toInt() and 0xFF)
    }

    @Test
    fun `magic packet layout`() {
        val bytes = PacketEncoder.magic(0x12345678)
        assertEquals(5, bytes.size)
        assertEquals(PktType.MAGIC, bytes[0].toInt() and 0xFF)
        assertEquals(0x12, bytes[1].toInt() and 0xFF)
        assertEquals(0x34, bytes[2].toInt() and 0xFF)
        assertEquals(0x56, bytes[3].toInt() and 0xFF)
        assertEquals(0x78, bytes[4].toInt() and 0xFF)
    }

    /**
     * PKT_RELIABLE wire layout (C: %c%hd%ld%ld + data):
     *   byte   : PKT_RELIABLE
     *   int16  : len   (number of payload bytes)
     *   int32  : relOff (byte offset into the reliable stream)
     *   int32  : frameLoop
     *   byte[] : data
     *
     * Total header = 1 + 2 + 4 + 4 = 11 bytes.
     */
    @Test
    fun `reliable packet layout`() {
        val payload = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        val bytes = PacketEncoder.reliable(offset = 100, data = payload, frameLoop = 7)
        // 1 (type) + 2 (len int16) + 4 (relOff int32) + 4 (frameLoop int32) + 2 (data) = 13
        assertEquals(1 + 2 + 4 + 4 + 2, bytes.size)
        assertEquals(PktType.RELIABLE, bytes[0].toInt() and 0xFF)
        // len = 2 as int16 (big-endian)
        assertEquals(0x00, bytes[1].toInt() and 0xFF)
        assertEquals(0x02, bytes[2].toInt() and 0xFF)
        // relOff = 100 = 0x00000064 as int32
        assertEquals(0x00, bytes[3].toInt() and 0xFF)
        assertEquals(0x00, bytes[4].toInt() and 0xFF)
        assertEquals(0x00, bytes[5].toInt() and 0xFF)
        assertEquals(0x64, bytes[6].toInt() and 0xFF)
        // frameLoop = 7 as int32
        assertEquals(0x00, bytes[7].toInt() and 0xFF)
        assertEquals(0x00, bytes[8].toInt() and 0xFF)
        assertEquals(0x00, bytes[9].toInt() and 0xFF)
        assertEquals(0x07, bytes[10].toInt() and 0xFF)
        // payload
        assertContentEquals(payload, bytes.copyOfRange(11, 13))
    }
}
