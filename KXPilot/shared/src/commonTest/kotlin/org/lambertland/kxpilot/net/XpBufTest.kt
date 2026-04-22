package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// XpBufTest — unit tests for XpReader and XpWriter
// ---------------------------------------------------------------------------

class XpBufTest {
    // -----------------------------------------------------------------------
    // XpWriter / XpReader round-trip helpers
    // -----------------------------------------------------------------------

    private fun writer() = XpWriter()

    // -----------------------------------------------------------------------
    // Byte
    // -----------------------------------------------------------------------

    @Test
    fun `write and read single byte`() {
        val w = writer()
        w.writeByte(0xAB)
        assertEquals(0xAB, XpReader(w.toByteArray()).readByte())
    }

    @Test
    fun `write byte masks to unsigned 8 bits`() {
        val w = writer()
        w.writeByte(0x1FF) // should store only 0xFF
        assertEquals(0xFF, XpReader(w.toByteArray()).readByte())
    }

    // -----------------------------------------------------------------------
    // Short (signed)
    // -----------------------------------------------------------------------

    @Test
    fun `write and read positive short`() {
        val w = writer()
        w.writeShort(1000)
        assertEquals(1000, XpReader(w.toByteArray()).readShort())
    }

    @Test
    fun `write and read negative short round-trips`() {
        val w = writer()
        w.writeShort(-1)
        assertEquals(-1, XpReader(w.toByteArray()).readShort())
    }

    @Test
    fun `write and read short min value`() {
        val w = writer()
        w.writeShort(-32768)
        assertEquals(-32768, XpReader(w.toByteArray()).readShort())
    }

    @Test
    fun `write and read short max value`() {
        val w = writer()
        w.writeShort(32767)
        assertEquals(32767, XpReader(w.toByteArray()).readShort())
    }

    // -----------------------------------------------------------------------
    // UShort
    // -----------------------------------------------------------------------

    @Test
    fun `write and read unsigned short`() {
        val w = writer()
        w.writeShort(65535)
        assertEquals(65535, XpReader(w.toByteArray()).readUShort())
    }

    @Test
    fun `ushort zero`() {
        val w = writer()
        w.writeShort(0)
        assertEquals(0, XpReader(w.toByteArray()).readUShort())
    }

    // -----------------------------------------------------------------------
    // Int
    // -----------------------------------------------------------------------

    @Test
    fun `write and read positive int`() {
        val w = writer()
        w.writeInt(0x01020304)
        assertEquals(0x01020304, XpReader(w.toByteArray()).readInt())
    }

    @Test
    fun `write and read negative int`() {
        val w = writer()
        w.writeInt(-1)
        assertEquals(-1, XpReader(w.toByteArray()).readInt())
    }

    @Test
    fun `write and read int min value`() {
        val w = writer()
        w.writeInt(Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, XpReader(w.toByteArray()).readInt())
    }

    @Test
    fun `big-endian byte order for int`() {
        val w = writer()
        w.writeInt(0x11223344)
        val bytes = w.toByteArray()
        assertEquals(4, bytes.size)
        assertEquals(0x11, bytes[0].toInt() and 0xFF)
        assertEquals(0x22, bytes[1].toInt() and 0xFF)
        assertEquals(0x33, bytes[2].toInt() and 0xFF)
        assertEquals(0x44, bytes[3].toInt() and 0xFF)
    }

    // -----------------------------------------------------------------------
    // String
    // -----------------------------------------------------------------------

    @Test
    fun `write and read simple string`() {
        val w = writer()
        w.writeString("hello")
        assertEquals("hello", XpReader(w.toByteArray()).readString())
    }

    @Test
    fun `write and read empty string`() {
        val w = writer()
        w.writeString("")
        assertEquals("", XpReader(w.toByteArray()).readString())
    }

    @Test
    fun `string is NUL-terminated`() {
        val w = writer()
        w.writeString("hi")
        val bytes = w.toByteArray()
        assertEquals(3, bytes.size) // 'h' + 'i' + NUL
        assertEquals(0, bytes[2].toInt())
    }

    @Test
    fun `string truncated at maxBytes-1`() {
        val w = writer()
        w.writeString("abcdefghijklmnopq", maxBytes = 5) // max 4 chars + NUL
        val s = XpReader(w.toByteArray()).readString(5)
        assertEquals("abcd", s)
    }

    @Test
    fun `read string stops at NUL mid-buffer`() {
        val buf = byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 0, 'c'.code.toByte())
        val result = XpReader(buf).readString(10)
        assertEquals("ab", result)
    }

    // -----------------------------------------------------------------------
    // Bytes
    // -----------------------------------------------------------------------

    @Test
    fun `write and read raw bytes`() {
        val w = writer()
        val data = byteArrayOf(0x01, 0x02, 0x03)
        w.writeBytes(data)
        val result = XpReader(w.toByteArray()).readBytes(3)
        assertEquals(data.toList(), result.toList())
    }

    // -----------------------------------------------------------------------
    // Skip
    // -----------------------------------------------------------------------

    @Test
    fun `skip advances position`() {
        val buf = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val r = XpReader(buf)
        r.skip(2)
        assertEquals(0x03, r.readByte())
    }

    // -----------------------------------------------------------------------
    // Underflow / overflow error handling
    // -----------------------------------------------------------------------

    @Test
    fun `read byte underflow throws`() {
        assertFailsWith<XpBufferException> {
            XpReader(byteArrayOf()).readByte()
        }
    }

    @Test
    fun `read short underflow throws`() {
        assertFailsWith<XpBufferException> {
            XpReader(byteArrayOf(0x01)).readShort() // only 1 byte, need 2
        }
    }

    @Test
    fun `read int underflow throws`() {
        assertFailsWith<XpBufferException> {
            XpReader(byteArrayOf(0x01, 0x02, 0x03)).readInt() // only 3 bytes, need 4
        }
    }

    @Test
    fun `skip underflow throws`() {
        assertFailsWith<XpBufferException> {
            XpReader(byteArrayOf(0x01)).skip(5)
        }
    }

    // -----------------------------------------------------------------------
    // Remaining / isExhausted
    // -----------------------------------------------------------------------

    @Test
    fun `remaining tracks position`() {
        val r = XpReader(byteArrayOf(0x01, 0x02, 0x03))
        assertEquals(3, r.remaining)
        r.readByte()
        assertEquals(2, r.remaining)
    }

    @Test
    fun `isExhausted after reading all bytes`() {
        val r = XpReader(byteArrayOf(0x01))
        r.readByte()
        assertTrue(r.isExhausted)
    }

    // -----------------------------------------------------------------------
    // XpContactPacket magic helpers
    // -----------------------------------------------------------------------

    @Test
    fun `version2magic embeds magic word`() {
        val magic = version2magic(0x4F15)
        assertEquals(XP_MAGIC_WORD, magic and 0xFFFF)
        assertEquals(0x4F15, magic2version(magic))
    }

    @Test
    fun `isValidMagic accepts correct magic`() {
        assertTrue(isValidMagic(version2magic(0x4F15)))
    }

    @Test
    fun `isValidMagic rejects bad magic`() {
        assert(!isValidMagic(0xDEADBEEF.toInt()))
    }
}
