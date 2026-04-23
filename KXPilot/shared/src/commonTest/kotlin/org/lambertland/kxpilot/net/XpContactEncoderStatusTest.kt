package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertTrue

/** N2/S2 — XpContactEncoder.replyStatus encodes player names. */
class XpContactEncoderStatusTest {
    @Test
    fun `replyStatus encodes non-empty player names into contact packet`() {
        val bytes =
            XpContactEncoder.replyStatus(
                serverVersion = 0x4F15,
                serverName = "TestServer",
                playerCount = 2,
                playerNames = listOf("Alice", "Bob"),
            )
        // Convert to string to check names are present
        val asString = bytes.decodeToString()
        assertTrue(asString.contains("TestServer"), "serverName should be in packet")
        assertTrue(asString.contains("Alice"), "player Alice should be in packet")
        assertTrue(asString.contains("Bob"), "player Bob should be in packet")
    }

    @Test
    fun `replyStatus with empty player list does not throw`() {
        val bytes =
            XpContactEncoder.replyStatus(
                serverVersion = 0x4F15,
                serverName = "EmptyServer",
                playerCount = 0,
                playerNames = emptyList(),
            )
        assertTrue(bytes.isNotEmpty())
    }
}
