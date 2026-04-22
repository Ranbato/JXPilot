package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [KeyState] press/release, justPressed edge detection,
 * and toPacket / fromPacket round-trip.
 */
class KeyStateTest {
    // -----------------------------------------------------------------------
    // 1. press / isDown
    // -----------------------------------------------------------------------
    @Test
    fun pressedKeyIsDown() {
        val ks = KeyState()
        ks.press(Key.KEY_THRUST)
        assertTrue(ks.isDown(Key.KEY_THRUST), "Pressed key should be down")
    }

    @Test
    fun unpressedKeyIsNotDown() {
        val ks = KeyState()
        assertFalse(ks.isDown(Key.KEY_THRUST), "Key should not be down before pressing")
    }

    @Test
    fun releasedKeyIsNoLongerDown() {
        val ks = KeyState()
        ks.press(Key.KEY_THRUST)
        ks.release(Key.KEY_THRUST)
        assertFalse(ks.isDown(Key.KEY_THRUST), "Released key should not be down")
    }

    // -----------------------------------------------------------------------
    // 2. justPressed edge detection
    // -----------------------------------------------------------------------
    @Test
    fun justPressedTrueOnFirstTick() {
        val ks = KeyState()
        ks.press(Key.KEY_FIRE_SHOT)
        assertTrue(ks.justPressed(Key.KEY_FIRE_SHOT), "Should be justPressed before advanceTick")
    }

    @Test
    fun justPressedFalseWhenHeld() {
        val ks = KeyState()
        ks.press(Key.KEY_FIRE_SHOT)
        ks.advanceTick() // previous = pressed
        assertFalse(ks.justPressed(Key.KEY_FIRE_SHOT), "Should not be justPressed when held across tick boundary")
    }

    @Test
    fun justPressedFalseWhenNotPressed() {
        val ks = KeyState()
        ks.advanceTick()
        assertFalse(ks.justPressed(Key.KEY_FIRE_SHOT), "Should not be justPressed when key was never pressed")
    }

    @Test
    fun justPressedTrueAfterReleaseAndRepress() {
        val ks = KeyState()
        ks.press(Key.KEY_FIRE_SHOT)
        ks.advanceTick()
        ks.release(Key.KEY_FIRE_SHOT)
        ks.advanceTick()
        ks.press(Key.KEY_FIRE_SHOT)
        assertTrue(ks.justPressed(Key.KEY_FIRE_SHOT), "Should be justPressed after re-press following release")
    }

    // -----------------------------------------------------------------------
    // 3. toPacket / fromPacket round-trip
    // -----------------------------------------------------------------------
    @Test
    fun toPacketRoundTripAllKeysOff() {
        val ks = KeyState()
        val packet = ks.toPacket()
        assertEquals(KeyState.KEY_PACKET_BYTES, packet.size, "Packet should be KEY_PACKET_BYTES bytes")
        assertTrue(packet.all { it == 0.toByte() }, "All-released state should produce all-zero packet")
    }

    @Test
    fun toPacketRoundTripSingleKey() {
        val key = Key.KEY_THRUST
        val ks = KeyState()
        ks.press(key)
        val packet = ks.toPacket()

        val ks2 = KeyState()
        ks2.fromPacket(packet)
        assertTrue(ks2.isDown(key), "fromPacket should restore pressed key")
    }

    @Test
    fun toPacketRoundTripMultipleKeys() {
        val keys = listOf(Key.KEY_THRUST, Key.KEY_TURN_LEFT, Key.KEY_FIRE_SHOT)
        val ks = KeyState()
        keys.forEach { ks.press(it) }
        val packet = ks.toPacket()

        val ks2 = KeyState()
        ks2.fromPacket(packet)
        keys.forEach { k ->
            assertTrue(ks2.isDown(k), "fromPacket should restore key $k")
        }
    }

    @Test
    fun fromPacketClearsUnpressedKeys() {
        val ks = KeyState()
        ks.press(Key.KEY_THRUST)
        ks.press(Key.KEY_TURN_LEFT)

        // Packet with only KEY_THRUST set
        val ks2 = KeyState()
        ks2.press(Key.KEY_THRUST)
        val packet = ks2.toPacket()

        ks.fromPacket(packet)
        assertTrue(ks.isDown(Key.KEY_THRUST), "KEY_THRUST should be set after fromPacket")
        assertFalse(ks.isDown(Key.KEY_TURN_LEFT), "KEY_TURN_LEFT should be cleared after fromPacket")
    }

    @Test
    fun packetSizeMatchesExpected() {
        assertEquals(9, KeyState.KEY_PACKET_BYTES, "KEY_PACKET_BYTES should be 9 for 72 keys")
    }
}
