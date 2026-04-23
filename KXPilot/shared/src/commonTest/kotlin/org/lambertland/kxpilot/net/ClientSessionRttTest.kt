package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** N4, S1 — ClientSession RTT measurement and reliable retransmit tests. */
class ClientSessionRttTest {
    private fun makeSession(): ClientSession = ClientSession(id = 0, addr = "127.0.0.1", loginPort = 9000, magic = 0)

    // -----------------------------------------------------------------------
    // N4 — RTT measurement
    // -----------------------------------------------------------------------

    @Test
    fun `handleAck updates rttMs to non-negative value after recordReliableSent`() {
        val session = makeSession()
        // Queue some reliable data so reliableWritePos > 0
        session.queueReliable(byteArrayOf(1, 2, 3))
        val sentMs = 1000L
        session.recordReliableSent(sentMs)
        val ackMs = 1050L
        session.handleAck(XpPacket.Ack(bytePos = 3), nowMs = ackMs)
        val rtt = session.rttMs()
        assertNotNull(rtt)
        assertTrue(rtt >= 0, "rttMs must be non-negative, was $rtt")
        assertEquals(50, rtt)
    }

    @Test
    fun `handleAck does not update rttMs when lastReliableSentMs is 0`() {
        val session = makeSession()
        session.queueReliable(byteArrayOf(1))
        session.handleAck(XpPacket.Ack(bytePos = 1), nowMs = 2000L)
        assertNull(session.rttMs(), "rttMs should remain null when lastReliableSentMs was never set")
    }

    // -----------------------------------------------------------------------
    // S1 — shouldRetransmit
    // -----------------------------------------------------------------------

    @Test
    fun `shouldRetransmit returns false before timeout elapses`() {
        val session = makeSession()
        session.queueReliable(byteArrayOf(1, 2))
        val sentMs = 1000L
        session.recordReliableSent(sentMs)
        // 100ms later — well within RETRANSMIT_TIMEOUT_MS (500ms)
        assertFalse(session.shouldRetransmit(sentMs + 100L))
    }

    @Test
    fun `shouldRetransmit returns true after timeout elapses`() {
        val session = makeSession()
        session.queueReliable(byteArrayOf(1, 2))
        val sentMs = 1000L
        session.recordReliableSent(sentMs)
        // 600ms later — past RETRANSMIT_TIMEOUT_MS (500ms)
        assertTrue(session.shouldRetransmit(sentMs + 600L))
    }

    @Test
    fun `shouldRetransmit returns false when nothing is pending`() {
        val session = makeSession()
        // No data queued — reliablePending == 0
        session.recordReliableSent(1000L)
        assertFalse(session.shouldRetransmit(2000L))
    }

    // -----------------------------------------------------------------------
    // S1 — buildReliablePacket
    // -----------------------------------------------------------------------

    @Test
    fun `buildReliablePacket returns null when nothing is pending`() {
        val session = makeSession()
        assertNull(session.buildReliablePacket())
    }

    @Test
    fun `buildReliablePacket returns non-null with correct prefix when data is pending`() {
        val session = makeSession()
        session.queueReliable(byteArrayOf(0x42, 0x43))
        val packet = session.buildReliablePacket()
        assertNotNull(packet)
        // First byte must be PKT_RELIABLE (42)
        assertEquals(PktType.RELIABLE, packet[0].toInt() and 0xFF)
    }
}
