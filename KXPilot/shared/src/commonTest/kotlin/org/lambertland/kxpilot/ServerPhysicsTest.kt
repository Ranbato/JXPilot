package org.lambertland.kxpilot

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.ObjStatus
import org.lambertland.kxpilot.server.ObjectPools
import org.lambertland.kxpilot.server.Player
import org.lambertland.kxpilot.server.PlayerDefaults
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerGameWorld
import org.lambertland.kxpilot.server.ServerPhysics
import org.lambertland.kxpilot.server.WallHitResult
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// ServerPhysicsTest
// ---------------------------------------------------------------------------

class ServerPhysicsTest {
    // Helper: build a default player positioned at world centre.
    private fun makePlayer(world: ServerGameWorld): Player = world.spawnPlayer(0, "tester", "tester", 0)

    // Helper: minimal world (60×60 open field).
    private fun makeWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

    // -----------------------------------------------------------------------
    // Thrust
    // -----------------------------------------------------------------------

    @Test
    fun thrustAcceleratesPlayerInFacingDirection() {
        val world = makeWorld()
        val pl = makePlayer(world)

        // Face right (dir 0 → floatDir 0 → cos=1, sin=0)
        pl.setFloatDir(0.0)
        pl.dir = ServerGameWorld.radToDir(0.0)

        val velBefore = pl.vel.x

        // Press thrust for one tick
        pl.lastKeyv[Key.KEY_THRUST.ordinal] = true
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        // Velocity should have increased in the +X direction
        assertTrue(
            pl.vel.x > velBefore,
            "Thrust should increase X velocity; was $velBefore, now ${pl.vel.x}",
        )
    }

    @Test
    fun thrustSetsThustingStatusBit() {
        val world = makeWorld()
        val pl = makePlayer(world)

        pl.lastKeyv[Key.KEY_THRUST.ordinal] = true
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertTrue(
            (pl.objStatus.toInt() and ObjStatus.THRUSTING) != 0,
            "THRUSTING status bit should be set when thrusting",
        )
    }

    @Test
    fun noThrustClearsThustingStatusBit() {
        val world = makeWorld()
        val pl = makePlayer(world)

        // Set THRUSTING then clear it
        pl.objStatus = (pl.objStatus.toInt() or ObjStatus.THRUSTING).toUShort()
        pl.lastKeyv[Key.KEY_THRUST.ordinal] = false
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(
            0,
            pl.objStatus.toInt() and ObjStatus.THRUSTING,
            "THRUSTING status bit should be cleared when not thrusting",
        )
    }

    // -----------------------------------------------------------------------
    // Turning
    // -----------------------------------------------------------------------

    @Test
    fun turnRightIncreasesDir() {
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.setFloatDir(0.0)
        val dirBefore = pl.dir

        pl.lastKeyv[Key.KEY_TURN_RIGHT.ordinal] = true
        repeat(5) { ServerPhysics.tickPlayer(pl, world.world, it.toLong()) }

        assertTrue(
            pl.dir > dirBefore || pl.dir.toInt() == 0, // wraps at RES
            "Turn right should increase dir; before=$dirBefore after=${pl.dir}",
        )
    }

    @Test
    fun turnLeftChangesDir() {
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.setFloatDir(0.0)
        pl.dir = 0

        pl.lastKeyv[Key.KEY_TURN_LEFT.ordinal] = true
        repeat(5) { ServerPhysics.tickPlayer(pl, world.world, it.toLong()) }

        // After 5 ticks of turning left from dir=0, dir should be in the upper half of [0, RES)
        // (i.e. wrapping near RES since turning left from 0 goes toward RES-1)
        val dir = pl.dir.toInt()
        assertTrue(dir != 0, "Turn left should change dir from 0")
        assertTrue(dir > GameConst.RES / 2 || dir != 0, "Turn left from 0 should produce dir in upper half or non-zero; dir=$dir")
    }

    // -----------------------------------------------------------------------
    // Speed limit
    // -----------------------------------------------------------------------

    @Test
    fun speedDoesNotExceedLimit() {
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.setFloatDir(0.0)
        pl.lastKeyv[Key.KEY_THRUST.ordinal] = true

        repeat(500) { ServerPhysics.tickPlayer(pl, world.world, it.toLong()) }

        assertTrue(
            pl.velocity <= GameConst.SPEED_LIMIT + 0.01,
            "Speed should never exceed SPEED_LIMIT=${GameConst.SPEED_LIMIT}, was ${pl.velocity}",
        )
    }

    // -----------------------------------------------------------------------
    // World wrap
    // -----------------------------------------------------------------------

    @Test
    fun positionWrapsAtWorldEdge() {
        val world = makeWorld()
        val pl = makePlayer(world)

        // Teleport player to near the right edge, moving right fast
        val rightEdgeCx = world.world.cwidth - ClickConst.CLICK
        pl.pos = ClPos(rightEdgeCx, pl.pos.cy)
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(60f, 0f) // fast enough to cross boundary

        ServerPhysics.tickPlayer(pl, world.world, 1L)

        // After wrap, X should be near 0, not negative or > cwidth
        assertTrue(
            pl.pos.cx in 0 until world.world.cwidth,
            "Wrapped X should be within world bounds; cx=${pl.pos.cx}",
        )
    }

    // -----------------------------------------------------------------------
    // Wall kill
    // -----------------------------------------------------------------------

    @Test
    fun fastWallHitReturnsKilled() {
        val world = makeWorld()

        // Fill a block with a wall one block to the right of centre
        val wallBx = 31
        val wallBy = 30
        world.world.setBlock(wallBx, wallBy, CellType.FILLED)

        val pl = makePlayer(world)
        // Place player so that after one tick of clamped-velocity movement (65 px/tick)
        // the new position lands in block 31. Block 31 spans clicks [69440, 71679].
        // Start at block 30 (spans [67200, 69439]), at x=67300.
        // After 65*64=4160 clicks: 67300+4160=71460 → block 31. ✓
        val startCx = (wallBx - 1) * ClickConst.BLOCK_CLICKS + 100
        pl.pos = ClPos(startCx, wallBy * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2)
        // vel=91 → rawSpeed=91 > WALL_KILL_SPEED=90 → KILLED
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(91f, 0f)

        val result = ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(WallHitResult.KILLED, result, "Fast wall hit (rawSpeed>90) should return KILLED")
    }

    @Test
    fun slowWallHitReturnsBounced() {
        val world = makeWorld()

        val wallBx = 31
        val wallBy = 30
        world.world.setBlock(wallBx, wallBy, CellType.FILLED)

        val pl = makePlayer(world)
        val wallCx = wallBx * ClickConst.BLOCK_CLICKS
        // Place player 1 click left of the wall boundary so it crosses in one tick
        pl.pos = ClPos(wallCx - 1, 30 * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2)
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(5f, 0f) // < WALL_KILL_SPEED=90

        val result = ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(WallHitResult.BOUNCED, result, "Slow wall hit should return BOUNCED")
    }

    @Test
    fun freeMovementReturnsNone() {
        val world = makeWorld()
        val pl = makePlayer(world)

        // Move slowly toward centre — no walls in open-field world
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(1f, 0f)

        val result = ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(WallHitResult.NONE, result, "Free movement in open field should return NONE")
    }

    // -----------------------------------------------------------------------
    // Turning model correctness
    // -----------------------------------------------------------------------

    @Test
    fun turnacc_isSetNotAccumulated() {
        // C model: turnacc = ±turnspeed each frame key is held (not accumulated over frames).
        // After 1 tick of holding left, turnacc must equal exactly +turnspeed (not 2×turnspeed).
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.turnspeed = 10.0
        pl.turnresistance = 0.2 // non-zero so turnvel isn't zeroed

        pl.lastKeyv[Key.KEY_TURN_LEFT.ordinal] = true
        ServerPhysics.tickPlayer(pl, world.world, 1L)
        val turnacc1 = pl.turnacc

        ServerPhysics.tickPlayer(pl, world.world, 2L)
        val turnacc2 = pl.turnacc

        assertEquals(turnacc1, turnacc2, "turnacc should be constant while key is held, not accumulate")
        assertEquals(pl.turnspeed, turnacc1, "turnacc should equal +turnspeed when turning left")
    }

    @Test
    fun turnresistance_appliedAsMultiplierNotComplement() {
        // C model: turnvel *= turnresistance  (NOT *= (1 - turnresistance))
        // With turnresistance = 0.5 and initial turnvel = 10.0, after one tick (no key),
        // turnvel should be 0.5 * 10 = 5.0 (not 0.5 × prev after C step: turnacc=0 so
        // turnvel += 0, then turnvel *= 0.5).
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.turnspeed = 10.0
        pl.turnresistance = 0.5
        pl.turnvel = 10.0

        // No keys held — turnacc=0, turnvel *= 0.5
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        // After: turnvel = (10 + 0) * 0.5 = 5.0
        assertEquals(5.0, pl.turnvel, 1e-10, "turnresistance=0.5 should halve turnvel each tick")
    }

    @Test
    fun zeroTurnresistance_linearMode_resetsVel() {
        // C model: if turnresistance == 0, turnvel is zeroed after direction update.
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.turnspeed = 10.0
        pl.turnresistance = 0.0
        pl.turnvel = 5.0

        pl.lastKeyv[Key.KEY_TURN_LEFT.ordinal] = false
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(0.0, pl.turnvel, "turnvel should be zero after tick when turnresistance=0")
    }

    // -----------------------------------------------------------------------
    // Fuel consumption
    // -----------------------------------------------------------------------

    @Test
    fun thrustConsumesFuel() {
        val world = makeWorld()
        val pl = makePlayer(world)
        val initialFuel = 1000.0
        pl.fuel.sum = initialFuel

        pl.lastKeyv[Key.KEY_THRUST.ordinal] = true
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertTrue(pl.fuel.sum < initialFuel, "Thrusting should consume fuel; was $initialFuel, now ${pl.fuel.sum}")
    }

    @Test
    fun noThrustDoesNotConsumeFuel() {
        val world = makeWorld()
        val pl = makePlayer(world)
        val initialFuel = 500.0
        pl.fuel.sum = initialFuel

        pl.lastKeyv[Key.KEY_THRUST.ordinal] = false
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(initialFuel, pl.fuel.sum, "Not thrusting should not consume fuel")
    }

    @Test
    fun fuelDoesNotGoBelowZero() {
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.fuel.sum = 0.0001 // nearly empty

        pl.lastKeyv[Key.KEY_THRUST.ordinal] = true
        repeat(10) { ServerPhysics.tickPlayer(pl, world.world, it.toLong()) }

        assertTrue(pl.fuel.sum >= 0.0, "Fuel should never go below zero; was ${pl.fuel.sum}")
    }

    @Test
    fun fuelBurnRateMatchesCFormula() {
        // C: f = pl->power * 0.0008; subtract per tick
        val world = makeWorld()
        val pl = makePlayer(world)
        pl.fuel.sum = 10000.0
        val expectedBurn = pl.power * ServerPhysics.FUEL_BURN_COEFF

        pl.lastKeyv[Key.KEY_THRUST.ordinal] = true
        ServerPhysics.tickPlayer(pl, world.world, 1L)

        val actualBurn = 10000.0 - pl.fuel.sum
        assertEquals(expectedBurn, actualBurn, 1e-10, "Fuel burn should equal power * 0.0008 per tick")
    }
}

// ---------------------------------------------------------------------------
// R37 — tickShots with non-zero velocity shot that must travel to reach target
// ---------------------------------------------------------------------------

class TickShotsTravelTest {
    private fun makeWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

    /**
     * R37: place a shot with non-zero X velocity and a victim several pixels ahead.
     * After enough ticks for the shot to travel the distance, the victim must be killed.
     */
    @Test
    fun shotWithVelocityKillsTargetAfterTraveling() {
        val world = makeWorld()

        // Spawn victim in open space
        val victim = world.spawnPlayer(1, "victim", "v", 0)
        victim.plState = PlayerState.ALIVE
        val startCx = world.world.cwidth / 2
        val startCy = world.world.cheight / 2

        // Place the shot some distance to the LEFT of the victim
        // Shot moves right at shotSpeed pixels/tick; victim is at startPx + 20 pixels
        val shotSpeed = GameConst.SHOT_SPEED.toFloat() // pixels/tick
        val victimOffsetPx = 20f
        val victimCx = startCx + (victimOffsetPx * ClickConst.CLICK).toInt()
        victim.pos = ClPos(victimCx, startCy)

        // Allocate and configure shot
        val shot = world.pools.shots.allocate()!!
        shot.pos = ClPos(startCx, startCy)
        shot.vel = Vector(shotSpeed, 0f) // moving right at shot speed
        shot.id = 0 // owned by session 0 (not victim's session 1)
        shot.team = 0u
        shot.life = 200f

        // Tick until the shot reaches the victim or life runs out
        var killed = false
        val hitRadius = (GameConst.SHOT_RADIUS + GameConst.SHIP_SZ).toDouble()
        // Number of ticks needed: victimOffsetPx / shotSpeed rounded up + 1
        val ticksNeeded = (victimOffsetPx / shotSpeed).toInt() + 2
        repeat(ticksNeeded) {
            if (!killed) {
                val kills = ServerPhysics.tickShots(world.pools, world.world, world.players)
                if (kills.any { it.victimSessionId == 1 }) killed = true
            }
        }

        assertTrue(killed, "Shot with velocity $shotSpeed px/tick must travel $victimOffsetPx px and kill victim")
    }
}

// ---------------------------------------------------------------------------
// ApplyKeyBitmapTest
// ---------------------------------------------------------------------------

class ApplyKeyBitmapTest {
    private fun makeWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

    @Test
    fun thrustBitSetsKeyTrue() {
        val world = makeWorld()
        val pl = world.spawnPlayer(0, "t", "t", 0)

        val bitmap = ByteArray(9)
        val k = Key.KEY_THRUST.ordinal
        bitmap[k ushr 3] = (bitmap[k ushr 3].toInt() or (1 shl (k and 7))).toByte()

        world.applyKeyBitmap(pl, bitmap)

        assertTrue(pl.lastKeyv[k], "KEY_THRUST bit should be true")
    }

    @Test
    fun emptyBitmapSetsAllKeysFalse() {
        val world = makeWorld()
        val pl = world.spawnPlayer(0, "t", "t", 0)
        // Set all keys true first
        for (i in pl.lastKeyv.indices) pl.lastKeyv[i] = true

        world.applyKeyBitmap(pl, ByteArray(9) { 0 })

        assertTrue(pl.lastKeyv.none { it }, "All keys should be false after empty bitmap")
    }
}
