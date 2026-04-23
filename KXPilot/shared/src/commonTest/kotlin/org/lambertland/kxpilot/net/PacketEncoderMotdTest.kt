package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertEquals

/** N3 — PacketEncoder.motd wire format tests. */
class PacketEncoderMotdTest {
    @Test
    fun `motd first byte is PKT_MOTD (58)`() {
        val bytes = PacketEncoder.motd(0, "hello")
        assertEquals(58, bytes[0].toInt() and 0xFF)
    }

    @Test
    fun `motd totalLen field equals text length plus NUL`() {
        val text = "hello"
        val bytes = PacketEncoder.motd(0, text)
        // bytes 1-4 = totalLen (big-endian int32)
        val totalLen =
            ((bytes[1].toInt() and 0xFF) shl 24) or
                ((bytes[2].toInt() and 0xFF) shl 16) or
                ((bytes[3].toInt() and 0xFF) shl 8) or
                (bytes[4].toInt() and 0xFF)
        assertEquals(text.length + 1, totalLen)
    }

    @Test
    fun `motd offset field is encoded correctly`() {
        val bytes = PacketEncoder.motd(42, "hello")
        // bytes 5-8 = offset (big-endian int32)
        val offset =
            ((bytes[5].toInt() and 0xFF) shl 24) or
                ((bytes[6].toInt() and 0xFF) shl 16) or
                ((bytes[7].toInt() and 0xFF) shl 8) or
                (bytes[8].toInt() and 0xFF)
        assertEquals(42, offset)
    }
}
