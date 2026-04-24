package org.lambertland.kxpilot.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// PlayerPhysicsIsolationTest  (BL-21)
//
// Demonstrates that PhysicsState is independently constructible and testable
// without a full Player, and that PlayerStats holds score/bookkeeping state
// that is correctly delegated by Player.
// ---------------------------------------------------------------------------

class PlayerPhysicsIsolationTest {
    // -----------------------------------------------------------------------
    // PhysicsState isolation
    // -----------------------------------------------------------------------

    @Test
    fun physicsStateCanBeConstructedWithoutPlayer() {
        // Must be possible to create a PhysicsState with no Player at all.
        val physics = PhysicsState()
        assertEquals(PlayerState.UNDEFINED, physics.plState)
        assertFalse(physics.isAlive())
        assertFalse(physics.isCloaked())
        assertFalse(physics.isPhasing())
        assertFalse(physics.isSelfDestructing())
    }

    @Test
    fun physicsStateTracksPlayerStateIndependently() {
        val physics = PhysicsState()
        physics.plState = PlayerState.ALIVE
        assertTrue(physics.isAlive())
        physics.plState = PlayerState.KILLED
        assertTrue(physics.isKilled())
        assertFalse(physics.isAlive())
    }

    @Test
    fun physicsStateFuelAndPowerAreIndependent() {
        val physics = PhysicsState()
        physics.fuel.sum = 1000.0
        physics.fuel.max = 2000.0
        physics.power = 42.0
        physics.turnspeed = 3.14

        assertEquals(1000.0, physics.fuel.sum)
        assertEquals(2000.0, physics.fuel.max)
        assertEquals(42.0, physics.power)
        assertEquals(3.14, physics.turnspeed)
    }

    @Test
    fun physicsStateResetClearsAllFields() {
        val physics = PhysicsState()
        physics.plState = PlayerState.ALIVE
        physics.power = 99.0
        physics.fuel.sum = 500.0
        physics.used = 0xFF_FFFF_FFFFL

        // reset() is internal but accessible from within the same module.
        // Call via Player to exercise the delegation path.
        val player = Player()
        player.plState = PlayerState.ALIVE
        player.power = 99.0
        player.fuel.sum = 500.0
        player.used = 0xFF_FFFF_FFFFL
        player.reset()

        assertEquals(PlayerState.UNDEFINED, player.plState)
        assertEquals(0.0, player.power)
        assertEquals(0.0, player.fuel.sum)
        assertEquals(0L, player.used)
    }

    // -----------------------------------------------------------------------
    // PlayerStats delegation
    // -----------------------------------------------------------------------

    @Test
    fun playerStatsDelegationRoundTrips() {
        val player = Player()
        player.score = 42.5
        player.kills = 7
        player.deaths = 3
        player.survivalTime = 99.0
        player.check = 5
        player.bestLap = 1234

        // Verify delegation to stats sub-object
        assertEquals(42.5, player.stats.score)
        assertEquals(7, player.stats.kills)
        assertEquals(3, player.stats.deaths)
        assertEquals(99.0, player.stats.survivalTime)
        assertEquals(5, player.stats.check)
        assertEquals(1234, player.stats.bestLap)
    }

    @Test
    fun playerStatsResetClearsScoreFields() {
        val player = Player()
        player.score = 100.0
        player.kills = 10
        player.deaths = 5
        player.bestLap = 999
        player.snafuCount = 3.0

        player.reset()

        assertEquals(0.0, player.score)
        assertEquals(0, player.kills)
        assertEquals(0, player.deaths)
        assertEquals(0, player.bestLap)
        assertEquals(0.0, player.snafuCount)
    }

    @Test
    fun physicsStateResetDirectlyClearsAllFields() {
        // Issue #10: exercise PhysicsState.reset() without a Player to confirm
        // the reset logic is self-contained (internal visibility = accessible here).
        val physics = PhysicsState()
        physics.plState = PlayerState.ALIVE
        physics.power = 99.0
        physics.fuel.sum = 500.0
        physics.used = 0xFF_FFFF_FFFFL
        physics.shots = 5
        physics.selfDestructCount = 3.0
        physics.didShoot = true
        physics.lastWallTouch = 42L

        physics.reset()

        assertEquals(PlayerState.UNDEFINED, physics.plState)
        assertEquals(0.0, physics.power)
        assertEquals(0.0, physics.fuel.sum)
        assertEquals(0L, physics.used)
        assertEquals(0, physics.shots)
        assertEquals(0.0, physics.selfDestructCount)
        assertFalse(physics.didShoot)
        assertEquals(0L, physics.lastWallTouch)
    }

    @Test
    fun physicsStateAndStatsAreIndependentSubObjects() {
        val player = Player()
        player.score = 55.0
        player.plState = PlayerState.ALIVE
        player.power = 80.0

        // Verify each sub-object holds its own fields
        assertEquals(55.0, player.stats.score)
        assertEquals(PlayerState.ALIVE, player.physics.plState)
        assertEquals(80.0, player.physics.power)

        // Mutating stats does not affect physics
        player.score = 0.0
        assertEquals(PlayerState.ALIVE, player.physics.plState)
        assertEquals(80.0, player.physics.power)
    }
}
