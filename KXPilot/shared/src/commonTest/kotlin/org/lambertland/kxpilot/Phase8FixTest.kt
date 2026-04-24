package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.pixelToClick
import org.lambertland.kxpilot.engine.EngineConst
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.WeaponConst
import org.lambertland.kxpilot.server.Target
import org.lambertland.kxpilot.server.World
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun makeEngine(
    blocks: Int = 40,
    rng: Random = Random.Default,
): GameEngine =
    GameEngine(
        world =
            run {
                val w = World()
                w.x = blocks
                w.y = blocks
                w.bwidthFloor = blocks
                w.bheightFloor = blocks
                w.width = blocks * GameConst.BLOCK_SZ
                w.height = blocks * GameConst.BLOCK_SZ
                w.cwidth = w.width * 64
                w.cheight = w.height * 64
                w.block =
                    Array(blocks) {
                        Array(blocks) { org.lambertland.kxpilot.server.CellType.SPACE }
                    }
                w.gravity =
                    Array(blocks) {
                        Array(blocks) {
                            org.lambertland.kxpilot.common
                                .Vector(0f, 0f)
                        }
                    }
                w
            },
        rng = rng,
    )

private fun makeEngineSeeded(seed: Int = 42): GameEngine = makeEngine(rng = Random(seed))

/** Add a damaged target to the engine's world. Returns the target index. */
private fun GameEngine.addDamagedTarget(damage: Double): Int {
    val t = Target(pos = ClPos(0, 0), damage = damage)
    world.targets.add(t)
    return world.targets.lastIndex
}

private fun GameEngine.holdKey(
    key: Key,
    n: Int = 1,
) {
    val ks = KeyState()
    ks.press(key)
    repeat(n) {
        tick(ks)
        ks.advanceTick()
    }
}

// ---------------------------------------------------------------------------
// F1 — Target passive repair rate
// ---------------------------------------------------------------------------

class TargetRepairRateTest {
    @Test
    fun targetPassiveRepairRateIsSlowOnly() {
        // Passive repair must use only TARGET_REPAIR_PER_FRAME (not the fast fuel rate).
        val engine = makeEngine()
        val initialDamage = 100.0
        engine.addDamagedTarget(initialDamage)

        val ticks = 600
        // No input — just tick with empty keys
        val emptyKeys = KeyState()
        repeat(ticks) {
            engine.tick(emptyKeys)
            emptyKeys.advanceTick()
        }

        val expected = (initialDamage - ticks * WeaponConst.TARGET_REPAIR_PER_FRAME).coerceAtLeast(0.0)
        val actual = engine.world.targets[0].damage
        // Allow floating-point rounding up to 1e-6
        assertTrue(
            kotlin.math.abs(actual - expected) < 1e-6,
            "Passive repair should use only TARGET_REPAIR_PER_FRAME. Expected ≈$expected, got $actual",
        )
    }

    @Test
    fun playerRepairDrainsFuelAndReducesDamage() {
        val engine = makeEngine()
        val damage = 50.0
        engine.addDamagedTarget(damage)
        val fuelBefore = engine.fuel

        engine.playerActivatesRepair(0)

        val fuelAfter = engine.fuel
        val newDamage = engine.world.targets[0].damage

        assertEquals(
            fuelBefore - WeaponConst.REFUEL_RATE,
            fuelAfter,
            absoluteTolerance = 1e-9,
            message = "Repair should drain REFUEL_RATE fuel",
        )
        assertEquals(
            (damage - WeaponConst.TARGET_FUEL_REPAIR_PER_FRAME).coerceAtLeast(0.0),
            newDamage,
            absoluteTolerance = 1e-9,
            message = "Repair should reduce damage by TARGET_FUEL_REPAIR_PER_FRAME",
        )
    }

    @Test
    fun playerRepairBlockedWhenInsufficientFuel() {
        val engine = makeEngine()
        val damage = 50.0
        engine.addDamagedTarget(damage)
        engine.fuel = WeaponConst.REFUEL_RATE - 0.001 // just below threshold

        engine.playerActivatesRepair(0)

        assertEquals(
            damage,
            engine.world.targets[0].damage,
            absoluteTolerance = 1e-9,
            message = "Repair must not fire when fuel < REFUEL_RATE",
        )
    }
}

// ---------------------------------------------------------------------------
// F9 — confusedTicks off-by-one
// ---------------------------------------------------------------------------

class EcmBlindDurationTest {
    @Test
    fun ecmBlindsMissileForExactDuration() {
        val engine = makeEngine()
        // Place a missile heading directly away from the player (leftward = heading π),
        // owner = same as player so it never hits the player (ownerId = player.id).
        // No target; guidance is irrelevant for this test.
        val cx = engine.player.pos.cx
        val cy = engine.player.pos.cy
        val missile =
            org.lambertland.kxpilot.engine.MissileData(
                pos = ClPos(cx, cy),
                headingRad = PI, // heading left — away from player
                life = 10_000f,
                targetNpcId = -1,
                ownerId = engine.player.id, // owned by player — won't hit player
            )
        // Simulate ECM by setting confusedTicks directly
        val confusedDuration = WeaponConst.CONFUSED_TIME // e.g. ≈14.4 ticks
        missile.confusedTicks = confusedDuration
        engine.missiles.add(missile)

        val durationTicks = confusedDuration.toInt() // floor: missile still confused at this point
        val totalTicks = kotlin.math.ceil(confusedDuration.toDouble()).toInt() // ceil: missile unconfused after this many ticks
        val emptyKeys = KeyState()

        // After (durationTicks - 1) ticks the missile should still be confused
        repeat(durationTicks - 1) {
            engine.tick(emptyKeys)
            emptyKeys.advanceTick()
        }
        // Missile must still be alive and confused after floor(CONFUSED_TIME)-1 ticks
        assertTrue(engine.missiles.isNotEmpty(), "Missile should still be alive after ${durationTicks - 1} ticks")
        assertTrue(engine.missiles[0].confusedTicks > 0f, "Missile should still be confused after ${durationTicks - 1} ticks")

        // Tick until ceil(CONFUSED_TIME) → confusedTicks must be ≤ 0
        val remaining = totalTicks - (durationTicks - 1)
        repeat(remaining) {
            engine.tick(emptyKeys)
            emptyKeys.advanceTick()
        }
        assertTrue(engine.missiles.isNotEmpty(), "Missile should still be alive after $totalTicks ticks")
        assertTrue(engine.missiles[0].confusedTicks <= 0f, "Missile should be un-confused after $totalTicks ticks")
    }
}

// ---------------------------------------------------------------------------
// F3 — Spark spread within C bounds
// ---------------------------------------------------------------------------

class SparkSpreadTest {
    @Test
    fun sparkSpreadWithinCBounds() {
        val engine = makeEngineSeeded()
        // Set player heading = 0 (right); exhaust goes left (π)
        engine.player.setFloatDir(0.0)

        // Tick with thrust to spawn sparks
        engine.holdKey(Key.KEY_THRUST, n = 20)

        assertTrue(engine.sparks.isNotEmpty(), "Expected sparks to be spawned")

        val exhaustDir = (PI) // heading 0 → reverse = π (leftward)
        val maxSpread = WeaponConst.SPARK_SPREAD_RAD
        // Capture ship velocity at the end of thrusting (sparks are already stored)
        val shipVx =
            engine.player.vel.x
                .toDouble()
        val shipVy =
            engine.player.vel.y
                .toDouble()

        for (spark in engine.sparks) {
            // Relative velocity (subtract ship vel to get exhaust direction)
            val rvx = spark.vel.x.toDouble() - shipVx
            val rvy = spark.vel.y.toDouble() - shipVy
            val angle = atan2(rvy, rvx)
            // Angle difference from exhaustDir, wrapped to [0, π]
            val diff = kotlin.math.abs(((angle - exhaustDir + PI * 3) % (2.0 * PI)) - PI)
            assertTrue(
                diff <= maxSpread + 1e-9,
                "Spark relative angle deviation $diff exceeds max spread $maxSpread",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// F4 — Spark minimum speed
// ---------------------------------------------------------------------------

class SparkMinSpeedTest {
    @Test
    fun sparkMinSpeedRespected() {
        val engine = makeEngineSeeded()
        engine.player.setFloatDir(0.0)
        engine.holdKey(Key.KEY_THRUST, n = 20)

        assertTrue(engine.sparks.isNotEmpty(), "Expected sparks to be spawned")

        for (spark in engine.sparks) {
            val relVx = spark.vel.x.toDouble() - engine.player.vel.x
            val relVy = spark.vel.y.toDouble() - engine.player.vel.y
            val speed = hypot(relVx, relVy)
            assertTrue(
                speed >= WeaponConst.SPARK_MIN_SPEED - 1e-9,
                "Spark speed $speed is below SPARK_MIN_SPEED ${WeaponConst.SPARK_MIN_SPEED}",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// F5 — Energy pack lifetime range
// ---------------------------------------------------------------------------

class EnergyPackLifetimeTest {
    @Test
    fun energyPackLifetimeInRange() {
        val engine = makeEngineSeeded()
        val cx = (engine.world.width / 2) * 64
        val cy = (engine.world.height / 2) * 64
        // Place pack far from player so it won't be picked up
        val packPos = ClPos(cx + 10_000, cy)
        engine.spawnEnergyPack(packPos)

        assertTrue(engine.energyPacks.isNotEmpty())
        val pack = engine.energyPacks[0]
        val startLife = pack.life

        assertTrue(
            startLife >= WeaponConst.ENERGY_PACK_LIFE_MIN - 1f,
            "Pack lifetime $startLife below ENERGY_PACK_LIFE_MIN ${WeaponConst.ENERGY_PACK_LIFE_MIN}",
        )
        assertTrue(
            startLife <= WeaponConst.ENERGY_PACK_LIFE_MAX + 1f,
            "Pack lifetime $startLife above ENERGY_PACK_LIFE_MAX ${WeaponConst.ENERGY_PACK_LIFE_MAX}",
        )
    }
}

// ---------------------------------------------------------------------------
// F5/F8 — Energy pack fuel random in range (seeded for determinism)
// ---------------------------------------------------------------------------

class EnergyPackFuelRangeTest {
    @Test
    fun energyPackFuelRandomInRange() {
        // Use seeded RNG; place pack at player position so it gets picked up immediately.
        val engine = makeEngineSeeded(seed = 12345)
        // Drain fuel so there's room to receive the pack (pack gives 500-1011 fuel)
        engine.fuel = 0.0
        val fuelBefore = engine.fuel
        // Place pack exactly on player
        val cx = engine.player.pos.cx
        val cy = engine.player.pos.cy
        engine.spawnEnergyPack(ClPos(cx, cy))

        val emptyKeys = KeyState()
        engine.tick(emptyKeys) // should pick up the pack this tick
        emptyKeys.advanceTick()

        val gained = engine.fuel - fuelBefore
        assertTrue(
            gained >= WeaponConst.ENERGY_PACK_FUEL_MIN - 1e-6,
            "Fuel gained $gained below ENERGY_PACK_FUEL_MIN ${WeaponConst.ENERGY_PACK_FUEL_MIN}",
        )
        assertTrue(
            gained <= WeaponConst.ENERGY_PACK_FUEL_MAX + 1e-6,
            "Fuel gained $gained above ENERGY_PACK_FUEL_MAX ${WeaponConst.ENERGY_PACK_FUEL_MAX}",
        )
        assertTrue(engine.energyPacks.isEmpty(), "Pack should have been consumed")
    }
}

// ---------------------------------------------------------------------------
// F6 — Energy pack pickup radius
// ---------------------------------------------------------------------------

class EnergyPackPickupRadiusTest {
    @Test
    fun energyPackPickedUpAt23px() {
        val engine = makeEngine()
        val playerCx = engine.player.pos.cx
        val playerCy = engine.player.pos.cy
        // Place pack 23 px from player centre in click-space (23 * PIXEL_CLICKS = 23 * 64)
        val packCx = playerCx + (23 * 64)
        val packPos = ClPos(engine.world.wrapXClick(packCx), playerCy)
        engine.spawnEnergyPack(packPos)
        assertEquals(1, engine.energyPacks.size)

        val emptyKeys = KeyState()
        engine.tick(emptyKeys)
        emptyKeys.advanceTick()

        assertTrue(engine.energyPacks.isEmpty(), "Pack at 23 px should be picked up (radius=24 px)")
    }

    @Test
    fun energyPackNotPickedUpAt25px() {
        val engine = makeEngine()
        val playerCx = engine.player.pos.cx
        val playerCy = engine.player.pos.cy
        val packCx = playerCx + (25 * 64)
        val packPos = ClPos(engine.world.wrapXClick(packCx), playerCy)
        engine.spawnEnergyPack(packPos)
        assertEquals(1, engine.energyPacks.size)

        val emptyKeys = KeyState()
        engine.tick(emptyKeys)
        emptyKeys.advanceTick()

        assertFalse(engine.energyPacks.isEmpty(), "Pack at 25 px should NOT be picked up (radius=24 px)")
    }
}
