package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertEquals

// ---------------------------------------------------------------------------
// PacketEncoderFrameTest — byte-layout tests for game-frame packets
// ---------------------------------------------------------------------------

class PacketEncoderFrameTest {
    // -----------------------------------------------------------------------
    // PKT_START (type=0x06, 9 bytes: 1+4+4)
    // -----------------------------------------------------------------------

    @Test
    fun startPacketSize() {
        val pkt = PacketEncoder.start(frameLoop = 1, lastKeyChange = 2)
        assertEquals(9, pkt.size, "PKT_START should be 9 bytes")
    }

    @Test
    fun startPacketTypeId() {
        val pkt = PacketEncoder.start(0, 0)
        assertEquals(PktType.START, pkt[0].toInt() and 0xFF, "First byte should be PKT_START type")
    }

    @Test
    fun startPacketFrameLoop() {
        val pkt = PacketEncoder.start(frameLoop = 0x01020304, lastKeyChange = 0)
        assertEquals(0x01, pkt[1].toInt() and 0xFF)
        assertEquals(0x02, pkt[2].toInt() and 0xFF)
        assertEquals(0x03, pkt[3].toInt() and 0xFF)
        assertEquals(0x04, pkt[4].toInt() and 0xFF)
    }

    @Test
    fun startPacketLastKeyChange() {
        val pkt = PacketEncoder.start(frameLoop = 0, lastKeyChange = 0xAABBCCDD.toInt())
        assertEquals(0xAA, pkt[5].toInt() and 0xFF)
        assertEquals(0xBB, pkt[6].toInt() and 0xFF)
        assertEquals(0xCC, pkt[7].toInt() and 0xFF)
        assertEquals(0xDD, pkt[8].toInt() and 0xFF)
    }

    // -----------------------------------------------------------------------
    // PKT_END (type=0x07, 5 bytes: 1+4)
    // -----------------------------------------------------------------------

    @Test
    fun endPacketSize() {
        val pkt = PacketEncoder.end(0)
        assertEquals(5, pkt.size, "PKT_END should be 5 bytes")
    }

    @Test
    fun endPacketTypeId() {
        val pkt = PacketEncoder.end(0)
        assertEquals(PktType.END, pkt[0].toInt() and 0xFF, "First byte should be PKT_END type")
    }

    @Test
    fun endPacketFrameLoop() {
        val pkt = PacketEncoder.end(0x00112233)
        assertEquals(0x00, pkt[1].toInt() and 0xFF)
        assertEquals(0x11, pkt[2].toInt() and 0xFF)
        assertEquals(0x22, pkt[3].toInt() and 0xFF)
        assertEquals(0x33, pkt[4].toInt() and 0xFF)
    }

    // -----------------------------------------------------------------------
    // PKT_SELF (type=0x08, 32 bytes: 1+2+2+2+2+1+1+1+1+2+2+1+1+1+2+2+2+2+1+1+1)
    // -----------------------------------------------------------------------

    @Test
    fun selfPacketSize() {
        val pkt =
            PacketEncoder.self(
                posX = 100,
                posY = 200,
                velX = 3,
                velY = -1,
                dir = 64,
                power = 35,
                turnspeed = 30,
                turnresistance = 30,
            )
        assertEquals(31, pkt.size, "PKT_SELF should be 31 bytes")
    }

    @Test
    fun selfPacketTypeId() {
        val pkt = PacketEncoder.self(0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(PktType.SELF, pkt[0].toInt() and 0xFF, "First byte should be PKT_SELF type")
    }

    @Test
    fun selfPacketDir() {
        val pkt = PacketEncoder.self(0, 0, 0, 0, dir = 99, power = 0, turnspeed = 0, turnresistance = 0)
        // dir is at offset 9 (type(1)+posX(2)+posY(2)+velX(2)+velY(2))
        assertEquals(99, pkt[9].toInt() and 0xFF, "dir byte should match")
    }

    // -----------------------------------------------------------------------
    // PKT_SCORE (type=0x1D, 11 bytes: 1+2+4+2+1+1)
    // -----------------------------------------------------------------------

    @Test
    fun scorePacketSize() {
        val pkt = PacketEncoder.score(id = 1, score = 5.0, lives = 2)
        assertEquals(11, pkt.size, "PKT_SCORE should be 11 bytes")
    }

    @Test
    fun scorePacketTypeId() {
        val pkt = PacketEncoder.score(0, 0.0, 0)
        assertEquals(PktType.SCORE, pkt[0].toInt() and 0xFF, "First byte should be PKT_SCORE type")
    }

    @Test
    fun scorePacketScoreEncoding() {
        // score = 1.5 → 150 as int32 big-endian at offset 3
        val pkt = PacketEncoder.score(id = 0, score = 1.5, lives = 0)
        val encoded =
            ((pkt[3].toInt() and 0xFF) shl 24) or
                ((pkt[4].toInt() and 0xFF) shl 16) or
                ((pkt[5].toInt() and 0xFF) shl 8) or
                (pkt[6].toInt() and 0xFF)
        assertEquals(150, encoded, "score 1.5 should encode as 150 (×100)")
    }
}
