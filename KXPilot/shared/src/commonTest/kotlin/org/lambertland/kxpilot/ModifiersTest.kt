package org.lambertland.kxpilot

import org.lambertland.kxpilot.server.Modifier
import org.lambertland.kxpilot.server.Modifiers
import org.lambertland.kxpilot.server.get
import org.lambertland.kxpilot.server.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModifiersTest {
    // -----------------------------------------------------------------------
    // Zero baseline
    // -----------------------------------------------------------------------

    @Test fun zeroModifiersHaveAllFieldsZero() {
        val m = Modifiers.ZERO
        for (mod in Modifier.entries) {
            assertEquals(0, m.get(mod), "Expected $mod == 0 in ZERO")
        }
    }

    // -----------------------------------------------------------------------
    // Roundtrip: set then get for each modifier field
    // -----------------------------------------------------------------------

    @Test fun roundtripNuclear() {
        val m = Modifiers.ZERO.set(Modifier.Nuclear, 2)
        assertEquals(2, m.get(Modifier.Nuclear))
    }

    @Test fun roundtripCluster() {
        // Cluster is a 1-bit flag (mask = 0x1); max valid value is 1.
        val m = Modifiers.ZERO.set(Modifier.Cluster, 1)
        assertEquals(1, m.get(Modifier.Cluster))
    }

    @Test fun roundtripImplosion() {
        val m = Modifiers.ZERO.set(Modifier.Implosion, 1)
        assertEquals(1, m.get(Modifier.Implosion))
    }

    @Test fun roundtripVelocity() {
        val m = Modifiers.ZERO.set(Modifier.Velocity, 3)
        assertEquals(3, m.get(Modifier.Velocity))
    }

    @Test fun roundtripMini() {
        val m = Modifiers.ZERO.set(Modifier.Mini, 2)
        assertEquals(2, m.get(Modifier.Mini))
    }

    @Test fun roundtripSpread() {
        val m = Modifiers.ZERO.set(Modifier.Spread, 3)
        assertEquals(3, m.get(Modifier.Spread))
    }

    @Test fun roundtripPower() {
        val m = Modifiers.ZERO.set(Modifier.Power, 1)
        assertEquals(1, m.get(Modifier.Power))
    }

    @Test fun roundtripLaser() {
        val m = Modifiers.ZERO.set(Modifier.Laser, Modifier.LASER_BLIND)
        assertEquals(Modifier.LASER_BLIND, m.get(Modifier.Laser))
    }

    // -----------------------------------------------------------------------
    // Writes to different fields do not bleed into each other
    // -----------------------------------------------------------------------

    @Test fun independentFields() {
        val m =
            Modifiers.ZERO
                .set(Modifier.Nuclear, 3)
                .set(Modifier.Velocity, 2)
                .set(Modifier.Laser, Modifier.LASER_STUN)
        assertEquals(3, m.get(Modifier.Nuclear))
        assertEquals(2, m.get(Modifier.Velocity))
        assertEquals(Modifier.LASER_STUN, m.get(Modifier.Laser))
        // All other fields must remain zero
        assertEquals(0, m.get(Modifier.Cluster))
        assertEquals(0, m.get(Modifier.Implosion))
        assertEquals(0, m.get(Modifier.Mini))
        assertEquals(0, m.get(Modifier.Spread))
        assertEquals(0, m.get(Modifier.Power))
    }

    @Test fun overwritingFieldClearsOldBits() {
        val m =
            Modifiers.ZERO
                .set(Modifier.Nuclear, 3)
                .set(Modifier.Nuclear, 1)
        assertEquals(1, m.get(Modifier.Nuclear))
    }

    // -----------------------------------------------------------------------
    // Max-value roundtrip (all fields at max simultaneously)
    // -----------------------------------------------------------------------

    @Test fun allFieldsAtMaxValue() {
        var m = Modifiers.ZERO
        for (mod in Modifier.entries) {
            m = m.set(mod, mod.mask)
        }
        for (mod in Modifier.entries) {
            assertEquals(mod.mask, m.get(mod), "Expected $mod at max ${mod.mask}")
        }
    }

    // -----------------------------------------------------------------------
    // Out-of-range values must throw
    // -----------------------------------------------------------------------

    @Test fun setAboveMaxThrows() {
        assertFailsWith<IllegalArgumentException> {
            Modifiers.ZERO.set(Modifier.Nuclear, 4) // mask = 3
        }
    }

    @Test fun setNegativeThrows() {
        assertFailsWith<IllegalArgumentException> {
            Modifiers.ZERO.set(Modifier.Laser, -1)
        }
    }
}
