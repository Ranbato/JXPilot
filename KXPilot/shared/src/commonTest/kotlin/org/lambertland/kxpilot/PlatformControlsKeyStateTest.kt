package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.Key
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Structural smoke tests for [KeyState] press/release semantics used by
 * [PlatformControls] touch buttons.
 *
 * These tests verify the key-state contract without requiring a Compose
 * test runner — the composable itself is not instantiated here.
 */
class PlatformControlsKeyStateTest {
    @Test
    fun pressThrust_isDownReturnsTrue() {
        val keys = KeyState()
        keys.press(Key.KEY_THRUST)
        assertTrue(keys.isDown(Key.KEY_THRUST), "KEY_THRUST should be down after press()")
    }

    @Test
    fun releaseThrust_afterAdvanceTick_isDownReturnsFalse() {
        val keys = KeyState()
        keys.press(Key.KEY_THRUST)
        keys.release(Key.KEY_THRUST)
        keys.advanceTick()
        assertFalse(keys.isDown(Key.KEY_THRUST), "KEY_THRUST should not be down after release() + advanceTick()")
    }

    @Test
    fun pressFireShot_isDownReturnsTrue() {
        val keys = KeyState()
        keys.press(Key.KEY_FIRE_SHOT)
        assertTrue(keys.isDown(Key.KEY_FIRE_SHOT))
    }

    @Test
    fun releaseFireShot_isDownReturnsFalse() {
        val keys = KeyState()
        keys.press(Key.KEY_FIRE_SHOT)
        keys.release(Key.KEY_FIRE_SHOT)
        assertFalse(keys.isDown(Key.KEY_FIRE_SHOT))
    }

    @Test
    fun pressMultipleKeys_independentState() {
        val keys = KeyState()
        keys.press(Key.KEY_TURN_LEFT)
        keys.press(Key.KEY_SHIELD)
        assertTrue(keys.isDown(Key.KEY_TURN_LEFT))
        assertTrue(keys.isDown(Key.KEY_SHIELD))
        keys.release(Key.KEY_TURN_LEFT)
        assertFalse(keys.isDown(Key.KEY_TURN_LEFT))
        assertTrue(keys.isDown(Key.KEY_SHIELD), "KEY_SHIELD should still be down after releasing KEY_TURN_LEFT")
    }
}
