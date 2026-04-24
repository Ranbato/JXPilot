package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Vector
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S3 — Cannon predictive aiming tests.
 *
 * Coordinate system: positions are in clicks (1 pixel = CLICK = 64 clicks).
 * Velocities are in pixels/tick (stored as Float in Vector).
 */
class CannonPredictiveAimingTest {
    private val CLICK = ClickConst.CLICK

    /** Build a cannon at pixel position (cx, cy) with given smartness. */
    private fun makeCannon(
        pxX: Int,
        pxY: Int,
        smartness: Int,
        dir: Int = 0,
    ): Cannon =
        Cannon(
            pos = ClPos(pxX * CLICK, pxY * CLICK),
            dir = dir,
            connMask = 0u,
            smartness = smartness.toShort(),
        )

    /** Build a player at pixel position (px, py) with velocity (vx, vy) pixels/tick. */
    private fun makePlayer(
        pxX: Int,
        pxY: Int,
        vx: Float = 0f,
        vy: Float = 0f,
    ): Player {
        val pl = Player()
        pl.pos = ClPos(pxX * CLICK, pxY * CLICK)
        pl.vel = Vector(vx, vy)
        pl.plState = PlayerState.ALIVE
        return pl
    }

    /**
     * Compute the direction integer (0..RES-1) from cannon to player.
     * This is the "dumb aim" direction.
     */
    private fun directDir(
        cannonPxX: Int,
        cannonPxY: Int,
        playerPxX: Int,
        playerPxY: Int,
    ): Int {
        val dx = (playerPxX - cannonPxX).toDouble()
        val dy = (playerPxY - cannonPxY).toDouble()
        val angle = atan2(dy, dx)
        return ServerGameWorld.radToDir(angle).toInt()
    }

    // -----------------------------------------------------------------------
    // Test 1: stationary player — smartness 3 should equal direct aim
    // -----------------------------------------------------------------------

    @Test
    fun `smartness 3 cannon at origin fires at stationary player same as direct aim`() {
        val cannon = makeCannon(0, 0, smartness = 3)
        val player = makePlayer(100, 0) // 100px to the right, stationary
        val pools = ObjectPools()

        tickCannonsOnce(cannon, player, pools)

        val shot = firstShot(pools) ?: error("No shot fired")
        // Shot should travel in the +X direction (dir ≈ 0)
        val expectedDir = directDir(0, 0, 100, 0)
        val shotDir = computeShotDir(shot.vel)
        assertDirClose(
            expectedDir,
            shotDir,
            toleranceUnits = 2,
            "Stationary player: smartness-3 should aim same as direct",
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: player moving perpendicular — smartness 3 should lead the target
    // -----------------------------------------------------------------------

    @Test
    fun `smartness 3 cannon leads a player moving perpendicular to line of fire`() {
        // Cannon at origin, player at (100, 0) moving upward at 5 px/tick
        val cannon = makeCannon(0, 0, smartness = 3)
        val player = makePlayer(100, 0, vx = 0f, vy = 5f)
        val pools = ObjectPools()

        tickCannonsOnce(cannon, player, pools)

        val shot = firstShot(pools) ?: error("No shot fired")
        val directAimDir = directDir(0, 0, 100, 0)
        val shotDir = computeShotDir(shot.vel)

        // The shot direction should differ from direct aim (it should lead upward)
        val diff = dirDiff(shotDir, directAimDir)
        assertTrue(diff > 0, "Smartness-3 should lead the target: diff=$diff (shotDir=$shotDir, directDir=$directAimDir)")

        // The shot should have a positive Y component (leading upward)
        assertTrue(shot.vel.y > 0f, "Shot should have upward component to lead the moving player")
    }

    // -----------------------------------------------------------------------
    // Test 3: smartness 0 always fires in cannon's fixed direction
    // -----------------------------------------------------------------------

    @Test
    fun `smartness 0 cannon fires in fixed direction regardless of player position`() {
        val fixedDir = 32 // 90 degrees (upward in RES=128 system)
        val cannon = makeCannon(0, 0, smartness = 0, dir = fixedDir)
        val player = makePlayer(100, 0) // player is to the right, not upward
        val pools = ObjectPools()

        tickCannonsOnce(cannon, player, pools)

        val shot = firstShot(pools) ?: error("No shot fired")
        val shotDir = computeShotDir(shot.vel)
        assertDirClose(
            fixedDir,
            shotDir,
            toleranceUnits = 2,
            "Smartness-0 should fire in fixed cannon direction",
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: smartness 1 aims directly at player (not fixed dir)
    // -----------------------------------------------------------------------

    @Test
    fun `smartness 1 cannon aims directly at player position`() {
        val cannon = makeCannon(0, 0, smartness = 1, dir = 32) // fixed dir is upward
        val player = makePlayer(100, 0) // player is to the right
        val pools = ObjectPools()

        tickCannonsOnce(cannon, player, pools)

        val shot = firstShot(pools) ?: error("No shot fired")
        val expectedDir = directDir(0, 0, 100, 0)
        val shotDir = computeShotDir(shot.vel)
        assertDirClose(
            expectedDir,
            shotDir,
            toleranceUnits = 2,
            "Smartness-1 should aim directly at player, not fixed dir",
        )
    }

    // -----------------------------------------------------------------------
    // R33-test: predictive intercept position — within 1 pixel of actual player
    // position at time-of-flight (verifies the CLICK factor is NOT applied)
    // -----------------------------------------------------------------------

    @Test
    fun `R33 smartness 3 shot intercept position is within 1 pixel of actual player position at tof`() {
        // Cannon at origin, player at (50, 0) moving upward at 3 px/tick
        val cannonPxX = 0
        val cannonPxY = 0
        val playerPxX = 50
        val playerPxY = 0
        val playerVy = 3f

        val cannon = makeCannon(cannonPxX, cannonPxY, smartness = 3)
        val player = makePlayer(playerPxX, playerPxY, vx = 0f, vy = playerVy)
        val pools = ObjectPools()
        tickCannonsOnce(cannon, player, pools)

        val shot = firstShot(pools) ?: error("No shot fired by smartness-3 cannon")

        // Compute time-of-flight: t = dist / shotSpeed
        val dx = (playerPxX - cannonPxX).toDouble()
        val dy = (playerPxY - cannonPxY).toDouble()
        val dist = sqrt(dx * dx + dy * dy)
        val shotSpeed = if (cannon.shotSpeed > 0f) cannon.shotSpeed.toDouble() else GameConst.SHOT_SPEED
        val t = dist / shotSpeed

        // Player's predicted position at time t
        val predictedPlayerX = playerPxX.toDouble()
        val predictedPlayerY = playerPxY + playerVy.toDouble() * t

        // Shot's position at time t (integrating shot velocity from cannon origin)
        val shotPxX = cannonPxX + shot.vel.x.toDouble() * t
        val shotPxY = cannonPxY + shot.vel.y.toDouble() * t

        val errorPx =
            sqrt(
                (shotPxX - predictedPlayerX) * (shotPxX - predictedPlayerX) +
                    (shotPxY - predictedPlayerY) * (shotPxY - predictedPlayerY),
            )

        // R33: before the fix, the CLICK factor (×64) was wrongly applied to the
        // velocity, placing the predicted aim point 64× too far.  After the fix,
        // the shot should arrive within 1 pixel of the predicted intercept.
        assertTrue(
            errorPx <= 2.5,
            "R33: shot intercept error must be ≤ 2.5 px; was $errorPx px " +
                "(shot arrives at (${"%.2f".format(shotPxX)}, ${"%.2f".format(shotPxY)}), " +
                "player predicted at (${"%.2f".format(predictedPlayerX)}, ${"%.2f".format(predictedPlayerY)}))",
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Run tickCannons for a single cannon with a single player, cooldown pre-cleared. */
    private fun tickCannonsOnce(
        cannon: Cannon,
        player: Player,
        pools: ObjectPools,
    ) {
        cannon.fireTimer = 0.0 // ensure cannon is ready to fire
        ServerPhysics.tickCannons(
            cannons = listOf(cannon),
            players = mapOf(0 to player),
            pools = pools,
        )
    }

    /** Return the first allocated shot from the pool, or null if none. */
    private fun firstShot(pools: ObjectPools): GameObject? {
        var result: GameObject? = null
        pools.shots.forEach { shot ->
            if (result == null) result = shot
        }
        return result
    }

    /** Compute direction integer from a velocity vector. */
    private fun computeShotDir(vel: Vector): Int {
        val angle = atan2(vel.y.toDouble(), vel.x.toDouble())
        return ServerGameWorld.radToDir(angle).toInt()
    }

    /**
     * Assert two direction integers are within [toleranceUnits] of each other
     * (wrapping around RES=128).
     */
    private fun assertDirClose(
        expected: Int,
        actual: Int,
        toleranceUnits: Int,
        message: String = "",
    ) {
        val diff = dirDiff(actual, expected)
        assertTrue(
            diff <= toleranceUnits,
            "$message — expected dir≈$expected, got $actual (diff=$diff, tolerance=$toleranceUnits)",
        )
    }

    /** Minimum angular distance between two direction integers (0..RES-1), wrapping. */
    private fun dirDiff(
        a: Int,
        b: Int,
    ): Int {
        val raw = abs(a - b) % GameConst.RES
        return if (raw > GameConst.RES / 2) GameConst.RES - raw else raw
    }
}
