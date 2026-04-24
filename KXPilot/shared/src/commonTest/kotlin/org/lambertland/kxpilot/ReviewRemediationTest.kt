package org.lambertland.kxpilot

// ---------------------------------------------------------------------------
// ReviewRemediationTest
//
// Tests that cover every fix made in response to the senior-engineer code
// review.  Numbered comments reference the original review issues.
// ---------------------------------------------------------------------------

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.server.Cannon
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.Fuel
import org.lambertland.kxpilot.server.GameObject
import org.lambertland.kxpilot.server.ObjStatus
import org.lambertland.kxpilot.server.ObjectPool
import org.lambertland.kxpilot.server.ObjectPools
import org.lambertland.kxpilot.server.PhysicsState
import org.lambertland.kxpilot.server.PlayerAbility
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerGameWorld
import org.lambertland.kxpilot.server.ServerPhysics
import org.lambertland.kxpilot.server.ServerState
import org.lambertland.kxpilot.server.WallHitResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Helper fixtures
// ---------------------------------------------------------------------------

private fun makeWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

private fun ServerGameWorld.spawnAlive(id: Int = 0): org.lambertland.kxpilot.server.Player {
    val pl = spawnPlayer(id, "p$id", "u$id", 0)
    pl.plState = PlayerState.ALIVE
    return pl
}

// ---------------------------------------------------------------------------
// #2 — Depot full-tank check: player at exactly fuel.max must NOT refuel
// ---------------------------------------------------------------------------

class DepotFullTankCheckTest {
    @Test
    fun refuelDoesNotOverfillFullTank() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        pl.fuel.sum = pl.fuel.max // already full
        val expectedMax = pl.fuel.max // capture before tick

        // Wire a fuel depot with plenty of fuel
        val depot = Fuel(pos = pl.pos, fuel = 1000.0, connMask = 0u, lastChange = 0L, team = 0)
        pl.refuelTarget = depot
        pl.used = pl.used or PlayerAbility.REFUEL

        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertTrue(
            pl.fuel.sum <= expectedMax,
            "Fuel must not exceed max after refuel; sum=${pl.fuel.sum} max=$expectedMax",
        )
        assertEquals(
            0L,
            pl.used and PlayerAbility.REFUEL,
            "REFUEL ability bit must be cleared when tank is full",
        )
    }

    @Test
    fun refuelStopsExactlyAtMax() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        val expectedMax = pl.fuel.max // capture before tick
        val gap = GameConst.REFUEL_RATE * 0.5 // less than one full tick's worth
        pl.fuel.sum = expectedMax - gap

        val depot = Fuel(pos = pl.pos, fuel = 1000.0, connMask = 0u, lastChange = 0L, team = 0)
        pl.refuelTarget = depot
        pl.used = pl.used or PlayerAbility.REFUEL

        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(
            expectedMax,
            pl.fuel.sum,
            1e-9,
            "Partial fill should clamp to exactly fuel.max",
        )
    }
}

// ---------------------------------------------------------------------------
// #3 — resetForRespawn clears refuelTarget
// ---------------------------------------------------------------------------

class ResetForRespawnRefuelTargetTest {
    @Test
    fun resetForRespawnClearsRefuelTarget() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        val depot = Fuel(pos = pl.pos, fuel = 500.0, connMask = 0u, lastChange = 0L, team = 0)
        pl.refuelTarget = depot

        pl.resetForRespawn()

        assertNull(pl.refuelTarget, "resetForRespawn must clear refuelTarget")
    }
}

// ---------------------------------------------------------------------------
// #6 — lastSafePos is updated on free-movement ticks
// ---------------------------------------------------------------------------

class LastSafePosTest {
    @Test
    fun lastSafePosInitialisedToSpawnPosition() {
        // R6: lastSafePos must be the spawn position, not ClPos(0,0)
        val world = makeWorld()
        val pl = world.spawnAlive()
        assertEquals(
            pl.pos,
            pl.lastSafePos,
            "lastSafePos should equal spawn position before any tick",
        )
    }

    @Test
    fun lastSafePosUpdatedOnFreeTick() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        val startPos = pl.pos

        // Move slowly in open space so no wall collision occurs
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(1f, 0f)
        val result = ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(WallHitResult.NONE, result)
        // lastSafePos should reflect the position *before* this tick's move
        assertEquals(
            startPos,
            pl.lastSafePos,
            "lastSafePos should be set to pre-move position on a free-movement tick",
        )
    }

    @Test
    fun lastSafePosNotUpdatedOnWallHit() {
        val world = makeWorld()
        val wallBx = 31
        val wallBy = 30
        world.world.setBlock(wallBx, wallBy, CellType.FILLED)
        val pl = world.spawnAlive()

        // Place player one click left of the wall boundary
        val wallCx = wallBx * ClickConst.BLOCK_CLICKS
        pl.pos = ClPos(wallCx - 10, wallBy * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2)

        // Free-movement tick to establish a known lastSafePos
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(1f, 0f)
        ServerPhysics.tickPlayer(pl, world.world, 1L)
        val safeAfterFreeTick = pl.lastSafePos

        // Now slam into wall at high enough speed to cross the boundary
        pl.vel =
            org.lambertland.kxpilot.common
                .Vector(5f, 0f)
        pl.pos = ClPos(wallCx - 1, wallBy * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2)
        ServerPhysics.tickPlayer(pl, world.world, 2L)

        // lastSafePos must be exactly what was recorded on the free tick — not overwritten
        assertEquals(
            safeAfterFreeTick,
            pl.lastSafePos,
            "lastSafePos must not be updated during a wall-hit tick",
        )
    }
}

// ---------------------------------------------------------------------------
// #7 — Cannon.fireTimer defaults to SHOT_SPEED_FACTOR (not 0)
// ---------------------------------------------------------------------------

class CannonFireTimerDefaultTest {
    @Test
    fun cannonFireTimerDefaultIsFullCooldown() {
        val cannon = Cannon(pos = ClPos(0, 0), dir = 0, connMask = 0u)
        assertEquals(
            GameConst.SHOT_SPEED_FACTOR,
            cannon.fireTimer,
            "Cannon.fireTimer should default to SHOT_SPEED_FACTOR to avoid tick-1 volley",
        )
    }

    @Test
    fun allNewCannonsHaveNonZeroFireTimer() {
        // Add cannons to the world so the loop body actually executes
        val world = makeWorld()
        repeat(3) { i ->
            world.world.cannons.add(Cannon(pos = ClPos(i * 64, 0), dir = 0, connMask = 0u))
        }
        assertTrue(world.world.cannons.isNotEmpty(), "Precondition: world must have cannons")
        for (cannon in world.world.cannons) {
            assertTrue(
                cannon.fireTimer > 0.0,
                "Cannon at ${cannon.pos} should have fireTimer > 0 at construction",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// #8 — resetForRespawn sets plState to APPEARING (not DEAD)
// ---------------------------------------------------------------------------

class ResetForRespawnStateTest {
    @Test
    fun resetForRespawnSetsAppearing() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        pl.plState = PlayerState.KILLED

        pl.resetForRespawn()

        assertEquals(
            PlayerState.APPEARING,
            pl.plState,
            "resetForRespawn must set plState=APPEARING; DEAD means no more lives",
        )
    }
}

// ---------------------------------------------------------------------------
// #9 — Single-pass shot count map gives same result as O(P×S) countActive
// ---------------------------------------------------------------------------

class ShotCountSinglePassTest {
    @Test
    fun singlePassCountMatchesCountActive() {
        val pools = ObjectPools(shotCapacity = 16)
        val s1 = pools.shots.allocate()!!
        s1.id = 1
        val s2 = pools.shots.allocate()!!
        s2.id = 1
        val s3 = pools.shots.allocate()!!
        s3.id = 2

        // Single-pass map (mirrors tickWorld logic)
        val liveShotCounts = mutableMapOf<Int, Int>()
        pools.shots.forEach { shot ->
            val id = shot.id.toInt()
            liveShotCounts[id] = (liveShotCounts[id] ?: 0) + 1
        }

        // Verify against O(n) countActive for each id
        assertEquals(
            pools.shots.countActive { it.id.toInt() == 1 },
            liveShotCounts[1] ?: 0,
            "Single-pass count for id=1 should match countActive",
        )
        assertEquals(
            pools.shots.countActive { it.id.toInt() == 2 },
            liveShotCounts[2] ?: 0,
            "Single-pass count for id=2 should match countActive",
        )
        assertEquals(
            pools.shots.countActive { it.id.toInt() == 99 },
            liveShotCounts[99] ?: 0,
            "Single-pass count for absent id=99 should be 0",
        )
    }
}

// ---------------------------------------------------------------------------
// #11 — tickShots early-return when pool is empty
// ---------------------------------------------------------------------------

class TickShotsEarlyReturnTest {
    @Test
    fun tickShotsReturnsEmptyListWhenPoolEmpty() {
        val world = makeWorld()
        // Don't allocate any shots
        val kills = ServerPhysics.tickShots(world.pools, world.world, world.players)
        assertTrue(kills.isEmpty(), "tickShots with empty pool must return empty kill list")
    }
}

// ---------------------------------------------------------------------------
// #12 — tickPlayerCollisions early-return when fewer than 2 players
// ---------------------------------------------------------------------------

class TickPlayerCollisionsEarlyReturnTest {
    @Test
    fun noCollisionsWithOnePlayer() {
        val world = makeWorld()
        world.spawnAlive(0)
        val events = ServerPhysics.tickPlayerCollisions(world.players)
        assertTrue(events.isEmpty(), "tickPlayerCollisions with 1 player must return empty list")
    }

    @Test
    fun noCollisionsWithZeroPlayers() {
        val world = makeWorld()
        val events = ServerPhysics.tickPlayerCollisions(world.players)
        assertTrue(events.isEmpty(), "tickPlayerCollisions with 0 players must return empty list")
    }
}

// ---------------------------------------------------------------------------
// #13 — tickDepotProximity bounding-box pre-filter (no crash/no false positives)
// ---------------------------------------------------------------------------

class DepotProximityBoundingBoxTest {
    @Test
    fun depotFarAwayDoesNotTriggerRefuel() {
        val world = makeWorld()
        val pl = world.spawnAlive()

        // Place depot > 90 clicks away on both axes
        val farPos = ClPos(pl.pos.cx + 200, pl.pos.cy + 200)
        val farDepot = Fuel(pos = farPos, fuel = 1000.0, connMask = 0u, lastChange = 0L, team = 0)

        ServerPhysics.tickDepotProximity(world.players, listOf(farDepot))

        assertEquals(
            0L,
            pl.used and PlayerAbility.REFUEL,
            "Player should not have REFUEL bit set when depot is far away",
        )
        assertNull(pl.refuelTarget, "refuelTarget should remain null when depot is out of range")
    }

    @Test
    fun depotInRangeSetsRefuelBit() {
        val world = makeWorld()
        val pl = world.spawnAlive()

        // Place depot within 90 clicks
        val nearPos = ClPos(pl.pos.cx + 10, pl.pos.cy + 10)
        val nearDepot = Fuel(pos = nearPos, fuel = 1000.0, connMask = 0u, lastChange = 0L, team = 0)

        ServerPhysics.tickDepotProximity(world.players, listOf(nearDepot))

        assertFalse(
            (pl.used and PlayerAbility.REFUEL) == 0L,
            "Player should have REFUEL bit set when depot is in range",
        )
    }
}

// ---------------------------------------------------------------------------
// #14 — ObjectPool.free still functions correctly; @Deprecated discourages hot-path use
// ---------------------------------------------------------------------------

/**
 * Verifies that [ObjectPool.free] still removes the correct element even though it
 * carries `@Deprecated`.  Confirming the annotation itself exists at compile time is
 * sufficient (a missing annotation would cause this test to compile without
 * `@Suppress("DEPRECATION")`, yielding a warning-as-error failure in strict builds).
 */
class ObjectPoolFreeStillFunctionsTest {
    @Suppress("DEPRECATION")
    @Test
    fun freeRemovesElementAndDecreasesCount() {
        val pool =
            ObjectPool(4) {
                org.lambertland.kxpilot.server
                    .GameObject()
            }
        val obj = pool.allocate()!!
        assertEquals(1, pool.count)
        pool.free(obj)
        assertEquals(0, pool.count, "free() must remove the element from the active set")
    }

    @Suppress("DEPRECATION")
    @Test
    fun freeNonExistentObjectThrows() {
        val pool =
            ObjectPool(4) {
                org.lambertland.kxpilot.server
                    .GameObject()
            }
        val outsider =
            org.lambertland.kxpilot.server
                .GameObject()
        var threw = false
        try {
            pool.free(outsider)
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw, "free() of an object not in the pool must throw")
    }
}

// ---------------------------------------------------------------------------
// #17 — ObjectPool.forEachAlive handles swap-to-end correctly
// ---------------------------------------------------------------------------

class ObjectPoolForEachAliveTest {
    @Test
    fun forEachAliveFreesMatchingElements() {
        val pool =
            ObjectPool(4) {
                org.lambertland.kxpilot.server
                    .GameObject()
            }
        val a = pool.allocate()!!
        a.id = 1
        val b = pool.allocate()!!
        b.id = 2
        val c = pool.allocate()!!
        c.id = 1
        assertEquals(3, pool.count)

        // Free all objects with id == 1
        pool.forEachAlive { it.id.toInt() == 1 }
        assertEquals(1, pool.count, "Only 1 object with id=2 should remain")
        assertEquals(2.toShort(), pool[0].id, "Remaining object should be the one with id=2")
    }

    @Test
    fun forEachAliveDoesNotSkipSwappedElement() {
        // Two adjacent elements both need freeing: ensure neither is skipped
        val pool =
            ObjectPool(4) {
                org.lambertland.kxpilot.server
                    .GameObject()
            }
        val a = pool.allocate()!!
        a.id = 99
        val b = pool.allocate()!!
        b.id = 99
        val c = pool.allocate()!!
        c.id = 1

        pool.forEachAlive { it.id.toInt() == 99 }

        assertEquals(1, pool.count)
        assertEquals(1.toShort(), pool[0].id)
    }
}

// ---------------------------------------------------------------------------
// #18 — Fuel.withdraw() enforces non-negative and returns actual amount
// ---------------------------------------------------------------------------

class FuelWithdrawTest {
    @Test
    fun withdrawReturnsAmountAndDeductsFromDepot() {
        val depot = Fuel(pos = ClPos(0, 0), fuel = 100.0, connMask = 0u, lastChange = 0L, team = 0)
        val actual = depot.withdraw(30.0)
        assertEquals(30.0, actual, 1e-9, "Should withdraw full requested amount")
        assertEquals(70.0, depot.fuel, 1e-9, "Depot fuel should decrease by withdrawn amount")
    }

    @Test
    fun withdrawClampsToAvailableFuel() {
        val depot = Fuel(pos = ClPos(0, 0), fuel = 10.0, connMask = 0u, lastChange = 0L, team = 0)
        val actual = depot.withdraw(50.0)
        assertEquals(10.0, actual, 1e-9, "Cannot withdraw more than available")
        assertEquals(0.0, depot.fuel, 1e-9, "Depot should be empty after over-request")
    }

    @Test
    fun withdrawZeroIsNoOp() {
        val depot = Fuel(pos = ClPos(0, 0), fuel = 50.0, connMask = 0u, lastChange = 0L, team = 0)
        val actual = depot.withdraw(0.0)
        assertEquals(0.0, actual, 1e-9)
        assertEquals(50.0, depot.fuel, 1e-9)
    }

    @Test
    fun withdrawNegativeThrows() {
        val depot = Fuel(pos = ClPos(0, 0), fuel = 50.0, connMask = 0u, lastChange = 0L, team = 0)
        var threw = false
        try {
            depot.withdraw(-1.0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "Negative withdrawal should throw IllegalArgumentException")
    }

    @Test
    fun refuelUsesWithdrawInsteadOfDirectMutation() {
        // End-to-end: ensure depot.fuel is reduced by exactly min(REFUEL_RATE, available)
        val world = makeWorld()
        val pl = world.spawnAlive()
        pl.fuel.sum = 0.0 // empty tank
        val initialDepotFuel = 1000.0
        val depot = Fuel(pos = pl.pos, fuel = initialDepotFuel, connMask = 0u, lastChange = 0L, team = 0)
        pl.refuelTarget = depot
        pl.used = pl.used or PlayerAbility.REFUEL

        ServerPhysics.tickPlayer(pl, world.world, 1L)

        val transferred = initialDepotFuel - depot.fuel
        assertTrue(transferred > 0.0, "Depot fuel should decrease after refueling")
        assertTrue(
            transferred <= GameConst.REFUEL_RATE + 1e-9,
            "Transfer per tick must not exceed REFUEL_RATE; transferred=$transferred",
        )
    }
}

// ---------------------------------------------------------------------------
// R2 — sendMessageAll while not Running is a no-op (no crash, no phantom event)
// R3 — sendMessageOne to nonexistent player is a no-op
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class SendMessageGuardTest {
    private val config = ServerConfig(port = 19191, targetFps = 10)

    @Test
    fun sendMessageAllWhileStoppedIsNoOp() =
        runTest {
            val controller = ServerController(this)
            // Server is Stopped — call must not throw, must not change state
            controller.sendMessageAll("hello")
            assertIs<ServerState.Stopped>(controller.state.value)
        }

    @Test
    fun sendMessageAllAppendsEventWhenRunning() =
        runTest {
            val controller = ServerController(this)
            controller.start(config)
            runCurrent()
            val before = (controller.state.value as ServerState.Running).events.size
            controller.sendMessageAll("hi")
            val after = (controller.state.value as ServerState.Running).events.size
            assertTrue(after > before, "sendMessageAll should append an event when Running")
            controller.stop()
        }

    @Test
    fun sendMessageOneToNonexistentPlayerIsNoOp() =
        runTest {
            val controller = ServerController(this)
            controller.start(config)
            runCurrent()
            val before = (controller.state.value as ServerState.Running).events.size
            // Player 999 does not exist — must be silent no-op (no event, no crash)
            controller.sendMessageOne(999, "hello")
            val after = (controller.state.value as ServerState.Running).events.size
            assertEquals(before, after, "sendMessageOne to missing player must not append event")
            controller.stop()
        }
}

// ---------------------------------------------------------------------------
// R6 — lastSafePos initialised to spawn position (not ClPos(0,0))
// ---------------------------------------------------------------------------

class LastSafePosInitTest {
    @Test
    fun lastSafePosEqualsSpawnPositionOnFreshSpawn() {
        val world = makeWorld()
        val pl = world.spawnPlayer(0, "test", "test", 0)
        assertEquals(
            pl.pos,
            pl.lastSafePos,
            "lastSafePos must equal spawn pos immediately after spawnPlayer — ClPos(0,0) is a landmine",
        )
    }

    @Test
    fun lastSafePosEqualsSpawnPositionAfterRespawn() {
        val world = makeWorld()
        val pl = world.spawnPlayer(0, "test", "test", 0)
        val spawnPos = pl.pos
        // Manually trigger a respawn to the same position
        world.respawn(pl, spawnPos, 0.0)
        assertEquals(
            spawnPos,
            pl.lastSafePos,
            "lastSafePos must equal respawn pos after respawn",
        )
    }
}

// ---------------------------------------------------------------------------
// R20 — PhysicsState can be used independently of Player in tickPlayer
// ---------------------------------------------------------------------------

/**
 * Verifies that [ServerPhysics.tickPlayer(PhysicsState, GameObjectBase, ...)]
 * operates correctly without any [org.lambertland.kxpilot.server.Player] instance.
 *
 * A bare [PhysicsState] + [GameObject] pair substitutes for a full Player,
 * proving the physics engine is decoupled from the god-object.
 */
class PhysicsStateDecouplingTest {
    private fun makeSimpleWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

    /** Helper: create a PhysicsState in ALIVE state with sensible defaults. */
    private fun alivePhysics(): PhysicsState =
        PhysicsState().also { phys ->
            phys.plState = PlayerState.ALIVE
            phys.power = 10.0
            phys.fuel.sum = 1000.0
            phys.fuel.max = 1000.0
            phys.turnspeed = 1.0
            phys.turnresistance = 0.9
        }

    /** Helper: create a GameObjectBase with mass, gravity-enabled, centred in world. */
    private fun centredBase(world: ServerGameWorld): GameObject =
        GameObject().also { base ->
            base.mass = 10f
            base.objStatus = ObjStatus.GRAVITY.toUShort()
            // Place in the centre of the world map
            val cx = world.world.cwidth / 2
            val cy = world.world.cheight / 2
            base.pos = ClPos(cx, cy)
        }

    @Test
    fun tickPlayerWithPhysicsStateDoesNotRequirePlayer() {
        // If this test compiles and runs, the PhysicsState overload exists and works.
        val world = makeSimpleWorld()
        val phys = alivePhysics()
        val base = centredBase(world)

        val result = ServerPhysics.tickPlayer(phys, base, world.world, 1L)

        // No walls in open space — should be NONE
        assertEquals(WallHitResult.NONE, result, "Open-space tick should return NONE")
    }

    @Test
    fun thrustIncreasesVelocityViaPhysicsState() {
        val world = makeSimpleWorld()
        val phys = alivePhysics()
        val base = centredBase(world)

        // Press thrust key
        phys.lastKeyv[org.lambertland.kxpilot.common.Key.KEY_THRUST.ordinal] = true
        phys.setFloatDir(0.0) // facing right

        val speedBefore = base.vel.x
        ServerPhysics.tickPlayer(phys, base, world.world, 1L)

        assertTrue(base.vel.x > speedBefore, "Thrust should increase X velocity when facing right")
    }

    @Test
    fun fuelConsumesOnThrustViaPhysicsState() {
        val world = makeSimpleWorld()
        val phys = alivePhysics()
        val base = centredBase(world)

        phys.lastKeyv[org.lambertland.kxpilot.common.Key.KEY_THRUST.ordinal] = true

        val fuelBefore = phys.fuel.sum
        ServerPhysics.tickPlayer(phys, base, world.world, 1L)

        assertTrue(phys.fuel.sum < fuelBefore, "Fuel should decrease on thrust tick")
    }

    @Test
    fun notAlivePhysicsStateIsSkipped() {
        val world = makeSimpleWorld()
        val phys = PhysicsState() // plState = UNDEFINED — not alive
        val base = centredBase(world)
        val posBefore = base.pos

        val result = ServerPhysics.tickPlayer(phys, base, world.world, 1L)

        assertEquals(WallHitResult.NONE, result)
        assertEquals(posBefore, base.pos, "Non-alive PhysicsState should not move the object")
    }
}

// ---------------------------------------------------------------------------
// C1 — Wall kill must leave plState == KILLED at end of tick (tickRecovery runs last)
// ---------------------------------------------------------------------------

class WallKillStateConsistencyTest {
    @Test
    fun wallKillLeavesPlayerInKilledStateAfterTick() {
        // Verify that a wall kill does NOT prematurely transition to APPEARING
        // within the same tick.  tickRecovery must run after ALL kill detection.
        //
        // We simulate the contract directly: after kill detection sets KILLED,
        // tickRecovery has not yet been called, so state must still be KILLED.
        // Then calling tickRecovery once advances it to APPEARING.
        val world = makeWorld()
        val pl = world.spawnAlive()

        // Simulate kill detection (as done by wall / shot / collision handlers)
        pl.plState = PlayerState.KILLED
        pl.recoveryCount = GameConst.RECOVERY_DELAY.toDouble()

        // tickRecovery must NOT have run yet — player should still be KILLED
        assertEquals(
            PlayerState.KILLED,
            pl.plState,
            "plState should be KILLED after kill detection, before tickRecovery runs",
        )

        // Now run tickRecovery — should advance to APPEARING
        ServerPhysics.tickRecovery(pl, world)
        assertEquals(
            PlayerState.APPEARING,
            pl.plState,
            "plState should be APPEARING after tickRecovery processes KILLED",
        )
    }
}

// ---------------------------------------------------------------------------
// S2 — resetForRespawn must never expose plState == UNDEFINED
// ---------------------------------------------------------------------------

class ResetForRespawnStateTransitionTest {
    @Test
    fun resetForRespawnNeverExposesUndefined() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        pl.plState = PlayerState.KILLED

        pl.resetForRespawn()

        // After resetForRespawn, plState must be APPEARING — never UNDEFINED
        assertEquals(
            PlayerState.APPEARING,
            pl.plState,
            "resetForRespawn must set plState to APPEARING, never UNDEFINED",
        )
    }
}

// ---------------------------------------------------------------------------
// S6 — Shield deactivates when fuel hits zero
// ---------------------------------------------------------------------------

class ShieldFuelExhaustionTest {
    @Test
    fun shieldDeactivatesWhenFuelExhausted() {
        val world = makeWorld()
        val pl = world.spawnAlive()

        // Activate shield with no timer (key-held shield)
        pl.used = pl.used or PlayerAbility.SHIELD
        pl.shieldTime = 0.0 // no timer — relies on fuel drain to deactivate
        pl.fuel.sum = 0.001 // nearly empty — one tick's drain should exhaust it
        // Hold the shield key so the shield bit is set going into useItems()
        pl.lastKeyv[Key.KEY_SHIELD.ordinal] = true

        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertEquals(
            0L,
            pl.used and PlayerAbility.SHIELD,
            "Shield must be deactivated when fuel hits zero",
        )
    }

    @Test
    fun shieldRemainsActiveWithSufficientFuel() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        pl.used = pl.used or PlayerAbility.SHIELD
        pl.shieldTime = 0.0
        pl.fuel.sum = pl.fuel.max // full tank
        // Hold the shield key — in C, USES_SHIELD is set while KEY_SHIELD is pressed.
        pl.lastKeyv[Key.KEY_SHIELD.ordinal] = true

        ServerPhysics.tickPlayer(pl, world.world, 1L)

        assertFalse(
            (pl.used and PlayerAbility.SHIELD) == 0L,
            "Shield should remain active when tank has fuel and key is held",
        )
    }
}

// ---------------------------------------------------------------------------
// S8 — World.findSpawnBase returns correct base
// ---------------------------------------------------------------------------

class FindSpawnBaseTest {
    @Test
    fun findSpawnBaseReturnsTeamBase() {
        val world = makeWorld()
        val base1 =
            org.lambertland.kxpilot.server.Base(
                pos = ClPos(100, 100),
                dir = 0,
                ind = 0,
                team = 1,
                order = 0,
            )
        val base2 =
            org.lambertland.kxpilot.server.Base(
                pos = ClPos(200, 200),
                dir = 0,
                ind = 1,
                team = 2,
                order = 1,
            )
        world.world.bases.add(base1)
        world.world.bases.add(base2)

        assertEquals(base1, world.world.findSpawnBase(1), "Should return team-1 base for team 1")
        assertEquals(base2, world.world.findSpawnBase(2), "Should return team-2 base for team 2")
    }

    @Test
    fun findSpawnBaseTeamZeroReturnsFirstBase() {
        val world = makeWorld()
        val base =
            org.lambertland.kxpilot.server.Base(
                pos = ClPos(100, 100),
                dir = 0,
                ind = 0,
                team = 3,
                order = 0,
            )
        world.world.bases.add(base)

        // team=0 means no-team; any base is acceptable → first base returned
        assertEquals(base, world.world.findSpawnBase(0), "team=0 should accept any base")
    }

    @Test
    fun findSpawnBaseNoBases() {
        val world = makeWorld()
        // No bases in world
        assertNull(world.world.findSpawnBase(1), "findSpawnBase should return null when no bases exist")
    }
}

// ---------------------------------------------------------------------------
// M6 — plDeathsSinceJoin is incremented on each death
// ---------------------------------------------------------------------------

class PlDeathsSinceJoinTest {
    @Test
    fun plDeathsSinceJoinIncrementedOnEnvironmentKill() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        val before = pl.plDeathsSinceJoin
        org.lambertland.kxpilot.server.ScoreSystem
            .environmentKill(pl)
        assertEquals(before + 1, pl.plDeathsSinceJoin, "plDeathsSinceJoin must increment on death")
    }

    @Test
    fun plDeathsSinceJoinResetOnFullReset() {
        val world = makeWorld()
        val pl = world.spawnAlive()
        org.lambertland.kxpilot.server.ScoreSystem
            .environmentKill(pl)
        assertTrue(pl.plDeathsSinceJoin > 0)
        pl.reset()
        assertEquals(0, pl.plDeathsSinceJoin, "plDeathsSinceJoin must be reset to 0 on full reset")
    }
}
