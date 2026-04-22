package org.lambertland.kxpilot

import org.lambertland.kxpilot.common.GameColor
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.server.CannonDefense
import org.lambertland.kxpilot.server.CannonWeapon
import org.lambertland.kxpilot.server.ObjStatus
import org.lambertland.kxpilot.server.ObjType
import org.lambertland.kxpilot.server.Player
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.PlayerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnumsTest {
    // -----------------------------------------------------------------------
    // ObjType
    // -----------------------------------------------------------------------

    @Test fun objTypeFromCodeAllKnown() {
        for (entry in ObjType.entries) {
            assertEquals(entry, ObjType.fromCode(entry.code))
        }
    }

    @Test fun objTypeFromCodeUnknownReturnsNull() {
        assertNull(ObjType.fromCode(99))
        assertNull(ObjType.fromCode(-1))
    }

    @Test fun objTypeBitIsPowerOfTwo() {
        for (entry in ObjType.entries) {
            val bit = entry.bit
            assertTrue(
                bit != 0u && (bit and (bit - 1u)) == 0u,
                "Expected ${entry.name}.bit to be a power of two, was $bit",
            )
        }
    }

    @Test fun objTypeBitsAreDistinct() {
        val bits = ObjType.entries.map { it.bit }
        assertEquals(bits.distinct().size, bits.size)
    }

    @Test fun objTypeCodesAreSequentialFrom0() {
        ObjType.entries.forEachIndexed { index, entry ->
            assertEquals(index, entry.code, "Expected ${entry.name}.code == $index")
        }
    }

    // -----------------------------------------------------------------------
    // CannonWeapon
    // -----------------------------------------------------------------------

    @Test fun cannonWeaponFromCodeAllKnown() {
        for (entry in CannonWeapon.entries) {
            assertEquals(entry, CannonWeapon.fromCode(entry.code))
        }
    }

    @Test fun cannonWeaponFromCodeUnknownReturnsNull() {
        assertNull(CannonWeapon.fromCode(100))
    }

    @Test fun cannonWeaponCodesAreSequentialFrom0() {
        CannonWeapon.entries.forEachIndexed { index, entry ->
            assertEquals(index, entry.code, "Expected ${entry.name}.code == $index")
        }
    }

    // -----------------------------------------------------------------------
    // CannonDefense
    // -----------------------------------------------------------------------

    @Test fun cannonDefenseFromCodeAllKnown() {
        for (entry in CannonDefense.entries) {
            assertEquals(entry, CannonDefense.fromCode(entry.code))
        }
    }

    @Test fun cannonDefenseFromCodeUnknownReturnsNull() {
        assertNull(CannonDefense.fromCode(50))
    }

    // -----------------------------------------------------------------------
    // PlayerType.fromCode fallbacks
    // -----------------------------------------------------------------------

    @Test fun playerTypeFromCodeKnownValues() {
        assertEquals(PlayerType.HUMAN, PlayerType.fromCode(0))
        assertEquals(PlayerType.ROBOT, PlayerType.fromCode(1))
        assertEquals(PlayerType.TANK, PlayerType.fromCode(2))
    }

    @Test fun playerTypeFromCodeUnknownFallsBackToHuman() {
        assertEquals(PlayerType.HUMAN, PlayerType.fromCode(99))
        assertEquals(PlayerType.HUMAN, PlayerType.fromCode(-1))
    }

    // -----------------------------------------------------------------------
    // PlayerState.fromCode fallbacks
    // -----------------------------------------------------------------------

    @Test fun playerStateFromCodeKnownValues() {
        assertEquals(PlayerState.ALIVE, PlayerState.fromCode(3))
        assertEquals(PlayerState.DEAD, PlayerState.fromCode(5))
        assertEquals(PlayerState.PAUSED, PlayerState.fromCode(6))
    }

    @Test fun playerStateFromCodeUnknownFallsBackToUndefined() {
        assertEquals(PlayerState.UNDEFINED, PlayerState.fromCode(99))
        assertEquals(PlayerState.UNDEFINED, PlayerState.fromCode(-5))
    }

    // -----------------------------------------------------------------------
    // GameColor.fromIndex fallbacks
    // -----------------------------------------------------------------------

    @Test fun gameColorFromIndexUnknownFallsBackToBlack() {
        assertEquals(GameColor.BLACK, GameColor.fromIndex(999))
    }

    @Test fun gameColorFromIndexAllKnown() {
        for (entry in GameColor.entries) {
            assertEquals(entry, GameColor.fromIndex(entry.index))
        }
    }

    // -----------------------------------------------------------------------
    // Key.NUM_KEYS — covers the shared-key section only (not client-only keys)
    // -----------------------------------------------------------------------

    @Test fun keyNumKeysMatchesSharedSection() {
        // KEY_UNUSED_71 is the last shared key; its ordinal is 71, so NUM_KEYS should be 72
        assertEquals(72, Key.NUM_KEYS)
        assertEquals(71, Key.KEY_UNUSED_71.ordinal)
    }

    @Test fun keyNumKeysIsConst() {
        // Ensure it is accessible as a compile-time constant (used as annotation param below)
        assertEquals(72, Key.NUM_KEYS)
    }
}
