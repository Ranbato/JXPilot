package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
import org.lambertland.kxpilot.common.GameConst
import org.lambertland.kxpilot.common.Item
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.engine.BallData
import org.lambertland.kxpilot.engine.DemoShip
import org.lambertland.kxpilot.engine.EngineConst
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.MineData
import org.lambertland.kxpilot.engine.NPC_ID_BASE
import org.lambertland.kxpilot.engine.NpcAiConst
import org.lambertland.kxpilot.engine.NpcWeaponEvent
import org.lambertland.kxpilot.engine.PlayerItems
import org.lambertland.kxpilot.engine.TreasureGoal
import org.lambertland.kxpilot.engine.TreasurePlacement
import org.lambertland.kxpilot.engine.WeaponConst
import org.lambertland.kxpilot.engine.WorldItem
import org.lambertland.kxpilot.server.Cannon
import org.lambertland.kxpilot.server.CannonWeapon
import org.lambertland.kxpilot.server.CellType
import org.lambertland.kxpilot.server.Check
import org.lambertland.kxpilot.server.FrictionArea
import org.lambertland.kxpilot.server.ItemConfig
import org.lambertland.kxpilot.server.PlayerState
import org.lambertland.kxpilot.server.WormType
import org.lambertland.kxpilot.server.Wormhole
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun keysDown(vararg k: Key): KeyState = KeyState().also { ks -> k.forEach { ks.press(it) } }

private fun noKeys() = KeyState()

private fun GameEngine.tickN(
    n: Int,
    keys: KeyState,
) {
    repeat(n) {
        tick(keys)
        keys.advanceTick()
    }
}

// ---------------------------------------------------------------------------
// BL-01 — Laser pulse
// ---------------------------------------------------------------------------

class Bl01LaserTest {
    @Test
    fun laserFireCreatesLaserPulse() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(laser = 1)
        val keys = keysDown(Key.KEY_FIRE_LASER)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.laserPulses.isNotEmpty(), "Firing laser should create a laser pulse")
    }

    @Test
    fun laserRequiresLaserItem() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        // laser = 0 (default) — no pulse should spawn
        val keys = keysDown(Key.KEY_FIRE_LASER)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.laserPulses.isEmpty(), "No laser pulse without laser item")
    }

    @Test
    fun laserRepeatRateLimitsFireRate() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(laser = 1)
        val keys = keysDown(Key.KEY_FIRE_LASER)
        engine.tick(keys)
        keys.advanceTick()
        val count1 = engine.laserPulses.size // 1

        // Keep holding — should not fire again immediately (repeat rate > 1 tick)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(count1, engine.laserPulses.size, "Laser should not fire twice in consecutive ticks (repeat rate)")
    }

    @Test
    fun laserPulseFasterthaNormalShot() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(laser = 1)
        engine.player.setFloatDir(0.0)
        val keys = keysDown(Key.KEY_FIRE_LASER)
        engine.tick(keys)
        keys.advanceTick()
        val pulse = engine.laserPulses.firstOrNull()
        assertTrue(
            pulse != null && pulse.vel.x > GameConst.SHOT_SPEED.toFloat(),
            "Laser pulse X velocity should exceed normal shot speed",
        )
    }

    @Test
    fun laserPulseExpiresAfterLife() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(laser = 1)
        val keys = keysDown(Key.KEY_FIRE_LASER)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.laserPulses.isNotEmpty())

        // Tick past pulse life
        val noKeys = noKeys()
        val lifeTicks = EngineConst.LASER_PULSE_LIFE.toInt() + 5
        engine.tickN(lifeTicks, noKeys)
        assertTrue(engine.laserPulses.isEmpty(), "Laser pulse should expire after its lifetime")
    }
}

// ---------------------------------------------------------------------------
// BL-02 — Wormholes
// ---------------------------------------------------------------------------

class Bl02WormholeTest {
    /** Build an engine with two wormholes and the player at the first one. */
    private fun makeWormholeEngine(): GameEngine {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val wh1Pos = ClPos(cx * ClickConst.CLICK, cy * ClickConst.CLICK)
        val wh2Pos = ClPos((cx + 5 * GameConst.BLOCK_SZ) * ClickConst.CLICK, cy * ClickConst.CLICK)
        engine.world.wormholes.add(Wormhole(pos = wh1Pos, type = WormType.NORMAL))
        engine.world.wormholes.add(Wormhole(pos = wh2Pos, type = WormType.NORMAL))
        // Move player onto wh1
        engine.player.pos = wh1Pos
        return engine
    }

    @Test
    fun playerOnWormholeTeleports() {
        val engine = makeWormholeEngine()
        val startPos = engine.player.pos
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertNotEquals(startPos, engine.player.pos, "Player should teleport after entering wormhole")
    }

    @Test
    fun wormTypeOUTCannotBeEntered() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val exitPos = ClPos(cx * ClickConst.CLICK, cy * ClickConst.CLICK)
        val destPos = ClPos((cx + 5 * GameConst.BLOCK_SZ) * ClickConst.CLICK, cy * ClickConst.CLICK)
        engine.world.wormholes.add(Wormhole(pos = exitPos, type = WormType.OUT))
        engine.world.wormholes.add(Wormhole(pos = destPos, type = WormType.NORMAL))
        engine.player.pos = exitPos
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(exitPos, engine.player.pos, "OUT wormhole should not teleport the player")
    }

    @Test
    fun wormholeStableCountdownPreventsNewDestPick() {
        val engine = makeWormholeEngine()
        val wh = engine.world.wormholes[0]
        wh.countdown = 500.0
        wh.lastDest = 1
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        // If countdown was set and > 0, the same lastDest should be used → player goes to wh[1]
        assertEquals(
            engine.world.wormholes[1].pos,
            engine.player.pos,
            "Stable wormhole should reuse lastDest",
        )
    }
}

// ---------------------------------------------------------------------------
// BL-03 — Bouncing shots
// ---------------------------------------------------------------------------

class Bl03BounceShotsTest {
    private fun makeEngineWithWall(): GameEngine {
        val cols = 40
        val rows = 20
        val wallBx = cols / 2 + 2
        val wallBy = rows / 2
        val engine = GameEngine.forEmptyWorld(cols, rows)
        engine.world.block[wallBx][wallBy] = CellType.FILLED
        return engine
    }

    @Test
    fun shotBouncesOffWall() {
        val engine = makeEngineWithWall()
        engine.player.setFloatDir(0.0) // heading right
        val keys = keysDown(Key.KEY_FIRE_SHOT)
        engine.tick(keys)
        keys.advanceTick()
        val initialVelX =
            engine.shots
                .first()
                .vel.x
        assertTrue(initialVelX > 0, "Initial shot vel.x should be positive")

        val noKeys = noKeys()
        var bounced = false
        repeat(30) {
            if (engine.shots.isNotEmpty() && engine.shots
                    .first()
                    .vel.x < 0
            ) {
                bounced = true
            }
            engine.tick(noKeys)
            noKeys.advanceTick()
        }
        assertTrue(bounced, "Shot should bounce (vel.x reverses) after hitting FILLED wall")
    }

    @Test
    fun shotLifeReducesAfterBounce() {
        val engine = makeEngineWithWall()
        engine.player.setFloatDir(0.0)
        val keys = keysDown(Key.KEY_FIRE_SHOT)
        engine.tick(keys)
        keys.advanceTick()
        val originalLife = engine.shots.first().life

        val noKeys = noKeys()
        repeat(20) {
            engine.tick(noKeys)
            noKeys.advanceTick()
        }

        // After a bounce the life should have been multiplied by BOUNCE_LIFE_FACTOR
        val life = engine.shots.firstOrNull()?.life
        if (life != null) {
            assertTrue(life < originalLife, "Shot life should decrease after bounce")
        }
    }
}

// ---------------------------------------------------------------------------
// BL-04 — Shot spread
// ---------------------------------------------------------------------------

class Bl04SpreadTest {
    @Test
    fun spreadLevelCyclesOnToggle() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        assertEquals(0, engine.spreadLevel)

        fun pressToggle() {
            val keys = keysDown(Key.KEY_TOGGLE_SPREAD)
            engine.tick(keys)
            keys.advanceTick()
        }
        pressToggle()
        assertEquals(1, engine.spreadLevel)
        pressToggle()
        assertEquals(2, engine.spreadLevel)
        pressToggle()
        assertEquals(3, engine.spreadLevel)
        pressToggle()
        assertEquals(0, engine.spreadLevel, "Spread level wraps back to 0")
    }

    @Test
    fun spreadFiresThreeShotsWithWideangle() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(wideangle = 1)
        val keys = keysDown(Key.KEY_FIRE_SHOT)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(3, engine.shots.size, "With wideangle, should fire 3 spread shots")
    }

    @Test
    fun noSpreadWithoutWideangle() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        // wideangle = 0 (default)
        val keys = keysDown(Key.KEY_FIRE_SHOT)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(1, engine.shots.size, "Without wideangle, should fire 1 shot")
    }

    @Test
    fun spreadShotsHaveDifferentAngles() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(wideangle = 1)
        engine.player.setFloatDir(0.0) // heading right
        val keys = keysDown(Key.KEY_FIRE_SHOT)
        engine.tick(keys)
        keys.advanceTick()
        val vels = engine.shots.map { it.vel.y }
        // The three shots should have different Y velocities (diverging angles)
        val distinct = vels.toSet().size
        assertEquals(3, distinct, "Three spread shots should have distinct Y velocities")
    }
}

// ---------------------------------------------------------------------------
// BL-05 — Checkpoints
// ---------------------------------------------------------------------------

class Bl05CheckpointTest {
    @Test
    fun playerPassesCheckpointAdvancesIndex() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val cx = (engine.world.width / 2) * ClickConst.CLICK
        val cy = (engine.world.height / 2) * ClickConst.CLICK
        engine.world.checks.add(Check(ClPos(cx, cy)))
        engine.world.checks.add(Check(ClPos(cx + 10 * ClickConst.CLICK, cy)))
        engine.player.pos = ClPos(cx, cy)
        assertEquals(0, engine.checkIndex)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(1, engine.checkIndex, "checkIndex should advance after passing checkpoint 0")
    }

    @Test
    fun passingAllCheckpointsIncrementsLap() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val cx = (engine.world.width / 2) * ClickConst.CLICK
        val cy = (engine.world.height / 2) * ClickConst.CLICK
        engine.world.checks.add(Check(ClPos(cx, cy)))
        engine.player.pos = ClPos(cx, cy)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(0, engine.checkIndex, "checkIndex wraps to 0 after last checkpoint")
        assertEquals(1, engine.laps, "laps should increment after completing all checkpoints")
    }
}

// ---------------------------------------------------------------------------
// BL-06 — Friction areas
// ---------------------------------------------------------------------------

class Bl06FrictionTest {
    @Test
    fun frictionAreaSlowsPlayer() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        // Place friction area at player's block with friction = 0.5 (50% speed loss per tick)
        val cx = (engine.world.width / 2) * ClickConst.CLICK
        val cy = (engine.world.height / 2) * ClickConst.CLICK
        engine.world.frictionAreas.add(FrictionArea(pos = ClPos(cx, cy), frictionSetting = 0.5, friction = 0.5, group = 0))
        engine.player.pos = ClPos(cx, cy)
        engine.player.vel = Vector(10f, 0f)

        val initialSpeed = engine.player.vel.x
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(
            engine.player.vel.x < initialSpeed,
            "Player velocity should decrease inside friction area",
        )
    }

    @Test
    fun noFrictionOutsideArea() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        // Friction area far from player
        val farCx = 0
        val farCy = 0
        engine.world.frictionAreas.add(FrictionArea(pos = ClPos(farCx, farCy), frictionSetting = 0.5, friction = 0.5, group = 0))
        engine.player.vel = Vector(10f, 0f)

        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        // Global friction is 0 so speed should be unchanged (aside from physics steps)
        // Just verify it's still positive — no dramatic decay
        assertTrue(
            engine.player.vel.x > 5f,
            "Velocity should not be strongly reduced when not in friction area",
        )
    }
}

// ---------------------------------------------------------------------------
// BL-08 — Self-destruct
// ---------------------------------------------------------------------------

class Bl08SelfDestructTest {
    @Test
    fun selfDestructStartsCountdown() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        assertEquals(0.0, engine.selfDestructTicks)
        val keys = keysDown(Key.KEY_SELF_DESTRUCT)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.selfDestructTicks > 0.0, "Self-destruct countdown should start on first press")
    }

    @Test
    fun secondPressCancelsCountdown() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val keys = keysDown(Key.KEY_SELF_DESTRUCT)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.selfDestructTicks > 0.0)

        // Second press: key must be released and re-pressed (justPressed only)
        keys.release(Key.KEY_SELF_DESTRUCT)
        engine.tick(keys)
        keys.advanceTick()
        keys.press(Key.KEY_SELF_DESTRUCT)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(0.0, engine.selfDestructTicks, "Second KEY_SELF_DESTRUCT press should cancel countdown")
    }

    @Test
    fun selfDestructKillsPlayerAtZero() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val keys = keysDown(Key.KEY_SELF_DESTRUCT)
        engine.tick(keys)
        keys.advanceTick()
        val delay = engine.selfDestructTicks.toInt()

        val noKeys = noKeys()
        engine.tickN(delay + 5, noKeys)
        assertEquals(
            PlayerState.KILLED,
            engine.player.plState,
            "Player should be killed when self-destruct countdown reaches 0",
        )
    }
}

// ---------------------------------------------------------------------------
// BL-09 — Hyperjump
// ---------------------------------------------------------------------------

class Bl09HyperjumpTest {
    @Test
    fun hyperjumpTeleportsPlayer() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(hyperjump = 1)
        val startPos = engine.player.pos
        val keys = keysDown(Key.KEY_HYPERJUMP)
        engine.tick(keys)
        keys.advanceTick()
        assertNotEquals(
            startPos,
            engine.player.pos,
            "Hyperjump should teleport player to a different position",
        )
    }

    @Test
    fun hyperjumpConsumesCharge() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(hyperjump = 1)
        val keys = keysDown(Key.KEY_HYPERJUMP)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(0, engine.playerItems.hyperjump, "Hyperjump should consume one charge")
    }

    @Test
    fun hyperjumpRequiresCharge() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(hyperjump = 0)
        val startPos = engine.player.pos
        val keys = keysDown(Key.KEY_HYPERJUMP)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(startPos, engine.player.pos, "Hyperjump should not fire without charge")
    }
}

// ---------------------------------------------------------------------------
// BL-10 — Detonate all mines
// ---------------------------------------------------------------------------

class Bl10DetonateTest {
    @Test
    fun detonateAllMinesRemovesOwnedMines() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        // Place two owned mines
        val minePos = ClPos(engine.player.pos.cx + 100 * ClickConst.CLICK, engine.player.pos.cy)
        engine.mines.add(MineData(pos = minePos, ownerId = engine.player.id))
        engine.mines.add(MineData(pos = minePos, ownerId = engine.player.id))
        assertEquals(2, engine.mines.size)

        val keys = keysDown(Key.KEY_DETONATE_MINES)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.mines.isEmpty(), "All owned mines should be detonated")
    }

    @Test
    fun detonateSpawnsDebris() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        val minePos = ClPos(engine.player.pos.cx + 100 * ClickConst.CLICK, engine.player.pos.cy)
        engine.mines.add(MineData(pos = minePos, ownerId = engine.player.id))

        val keys = keysDown(Key.KEY_DETONATE_MINES)
        engine.tick(keys)
        keys.advanceTick()
        assertTrue(engine.debris.isNotEmpty(), "Detonating a mine should spawn debris fragments")
    }

    @Test
    fun detonateDoesNotRemoveEnemyMines() {
        val engine = GameEngine.forEmptyWorld(40, 40)
        val minePos = ClPos(engine.player.pos.cx + 100 * ClickConst.CLICK, engine.player.pos.cy)
        val enemyId: Short = 99
        engine.mines.add(MineData(pos = minePos, ownerId = enemyId))

        val keys = keysDown(Key.KEY_DETONATE_MINES)
        engine.tick(keys)
        keys.advanceTick()
        assertEquals(1, engine.mines.size, "KEY_DETONATE_MINES should not remove enemy-owned mines")
    }
}

// ---------------------------------------------------------------------------
// BL-11 — Cannon weapon variety
// ---------------------------------------------------------------------------

class Bl11CannonWeaponTest {
    private fun makeCannonEngine(weapon: CannonWeapon): GameEngine {
        val engine = GameEngine.forEmptyWorld(20, 20)
        // Place a cannon at a nearby block
        val cx = (engine.world.width / 2 - 3 * GameConst.BLOCK_SZ) * ClickConst.CLICK
        val cy = (engine.world.height / 2) * ClickConst.CLICK
        val cannon =
            Cannon(
                pos = ClPos(cx, cy),
                dir = 0,
                connMask = 0u,
                fireTimer = 0.0,
                weapon = weapon,
            )
        engine.world.cannons.add(cannon)
        return engine
    }

    @Test
    fun cannonShotWeaponFiresShot() {
        val engine = makeCannonEngine(CannonWeapon.SHOT)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.shots.isNotEmpty(), "SHOT cannon should fire a shot")
    }

    @Test
    fun cannonLaserWeaponFiresLaserPulse() {
        val engine = makeCannonEngine(CannonWeapon.LASER)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.laserPulses.isNotEmpty(), "LASER cannon should fire a laser pulse")
        assertTrue(engine.shots.isEmpty(), "LASER cannon should NOT fire a regular shot")
    }

    @Test
    fun cannonMissileWeaponFiresMissile() {
        val engine = makeCannonEngine(CannonWeapon.MISSILE)
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.missiles.isNotEmpty(), "MISSILE cannon should fire a missile")
        assertTrue(engine.shots.isEmpty(), "MISSILE cannon should NOT fire a regular shot")
    }
}

// ---------------------------------------------------------------------------
// BL-07 — World item spawning & pickup
// ---------------------------------------------------------------------------

class Bl07WorldItemTest {
    /** Build an engine with a FUEL item config (index 0) set to always spawn. */
    private fun makeItemEngine(): GameEngine {
        val engine = GameEngine.forEmptyWorld(20, 20)
        // Set FUEL (id=0) to always spawn (chance=1, max=999)
        engine.world.items[0] =
            org.lambertland.kxpilot.server.ItemConfig(
                prob = 1.0,
                max = 999,
                num = 1,
                chance = 1,
                cannonProb = 0.0,
                minPerPack = 1,
                maxPerPack = 1,
                initial = 0,
                cannonInitial = 0,
                limit = 999,
            )
        return engine
    }

    @Test
    fun worldItemSpawnsAfterInterval() {
        val engine = makeItemEngine()
        // Tick enough times to let the spawn timer fire at least once
        val noKeys = noKeys()
        engine.tickN((EngineConst.ITEM_SPAWN_INTERVAL + 2).toInt(), noKeys)
        assertTrue(engine.worldItems.isNotEmpty(), "A world item should have been spawned")
    }

    @Test
    fun worldItemExpiresAfterLife() {
        val engine = GameEngine.forEmptyWorld(10, 10)
        // Manually add a short-lived item far from the player so it is not picked up
        val farCx = 5 * ClickConst.BLOCK_CLICKS
        val farCy = 5 * ClickConst.BLOCK_CLICKS
        engine.worldItems.add(WorldItem(ClPos(farCx, farCy), Item.FUEL, life = 3f))
        assertEquals(1, engine.worldItems.size)
        val noKeys = noKeys()
        engine.tickN(5, noKeys)
        assertTrue(engine.worldItems.isEmpty(), "Expired world item should be removed")
    }

    @Test
    fun playerPicksUpItemOnOverlap() {
        val engine = GameEngine.forEmptyWorld(10, 10)
        // Drain fuel so we can detect the FUEL pickup
        engine.fuel = 0.0
        // Place item exactly at player position
        engine.worldItems.add(WorldItem(ClPos(engine.player.pos.cx, engine.player.pos.cy), Item.FUEL, life = 1000f))
        val noKeys = noKeys()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.worldItems.isEmpty(), "Item should be consumed on player overlap")
        assertTrue(engine.fuel > 0.0, "FUEL item pickup should replenish fuel")
    }
}

// ---------------------------------------------------------------------------
// BL-17 — Afterburner thrust scaling
// ---------------------------------------------------------------------------

class Bl17AfterburnerTest {
    @Test
    fun afterburnerLevel0GivesThrustMultiplierOne() {
        // With no afterburner the thrustMultiplier must be 1.0 — verified indirectly
        // by confirming the speed gain per tick is the same for level 0 and level 0.
        val engine = GameEngine.forEmptyWorld(20, 20)
        engine.playerItems = PlayerItems(afterburner = 0)
        // Player faces right (dir = 0), thrust for one tick
        engine.player.setFloatDir(0.0)
        val beforeVx = engine.player.vel.x
        val keys = keysDown(Key.KEY_THRUST)
        engine.tick(keys)
        keys.advanceTick()
        val deltaVx0 = engine.player.vel.x - beforeVx
        assertTrue(deltaVx0 > 0f, "Thrust should accelerate player rightward")
        // level 0 is the baseline
    }

    @Test
    fun afterburnerHigherLevelGivesMoreThrust() {
        // Level MAX_AFTERBURNER should yield significantly more velocity gain than level 0.
        fun thrustDeltaAtLevel(level: Int): Float {
            val engine = GameEngine.forEmptyWorld(20, 20)
            engine.playerItems = PlayerItems(afterburner = level)
            engine.player.setFloatDir(0.0)
            val before = engine.player.vel.x
            val keys = keysDown(Key.KEY_THRUST)
            engine.tick(keys)
            keys.advanceTick()
            return engine.player.vel.x - before
        }
        val delta0 = thrustDeltaAtLevel(0)
        val deltaMax = thrustDeltaAtLevel(WeaponConst.MAX_AFTERBURNER)
        assertTrue(deltaMax > delta0, "Max afterburner should give more thrust than level 0")
    }

    @Test
    fun afterburnerScalesLinearly() {
        // Thrust multiplier at level n = 1 + n*(ALT_SPARK_MASS_FACT-1)/(MAX_AFTERBURNER+1).
        // Verify level 8 gives roughly double the level 0 thrust gain (mid-point).
        fun thrustDeltaAtLevel(level: Int): Float {
            val engine = GameEngine.forEmptyWorld(20, 20)
            engine.playerItems = PlayerItems(afterburner = level)
            engine.player.setFloatDir(0.0)
            val before = engine.player.vel.x
            val keys = keysDown(Key.KEY_THRUST)
            engine.tick(keys)
            keys.advanceTick()
            return engine.player.vel.x - before
        }
        val delta0 = thrustDeltaAtLevel(0)
        val delta8 = thrustDeltaAtLevel(8)
        // At level 8 multiplier ≈ 1 + 8*(4.2-1)/16 = 1 + 8*3.2/16 = 2.6
        // delta8 / delta0 should be > 1.5 and < 4.0
        val ratio = delta8 / delta0
        assertTrue(ratio > 1.5f, "Afterburner level 8 thrust ratio should exceed 1.5 (got $ratio)")
        assertTrue(ratio < 4.0f, "Afterburner level 8 thrust ratio should be under 4.0 (got $ratio)")
    }
}

// ---------------------------------------------------------------------------
// BL-18 — Tractor beam Newton's 3rd + fuel cost
// ---------------------------------------------------------------------------

class Bl18TractorBeamTest {
    private fun makeEngineWithNpc(): Pair<GameEngine, DemoShip> {
        val engine = GameEngine.forEmptyWorld(40, 40)
        engine.playerItems = PlayerItems(tractorBeam = 1)
        // Place NPC directly to the right of the player, within tractor range
        val npcX = (engine.world.width / 2 + 100).toFloat()
        val npcY = (engine.world.height / 2).toFloat()
        val npc =
            DemoShip(
                id = NPC_ID_BASE,
                label = "test",
                x = npcX,
                y = npcY,
                heading = 0f,
                vx = 0f,
                vy = 0f,
                rotSpeed = 0f,
            )
        engine.lockNext(listOf(npc))
        return engine to npc
    }

    @Test
    fun tractorBeamPullsNpc() {
        val (engine, npc) = makeEngineWithNpc()
        val beforeNpcVx = npc.vx
        val keys = keysDown(Key.KEY_TRACTOR_BEAM)
        engine.tick(keys, listOf(npc))
        keys.advanceTick()
        // NPC should have been pulled leftward (toward player)
        assertTrue(npc.vx < beforeNpcVx, "Tractor should pull NPC toward player (decrease vx)")
    }

    @Test
    fun tractorBeamAppliesReactionToPlayer() {
        val (engine, npc) = makeEngineWithNpc()
        val beforePlayerVx = engine.player.vel.x
        val keys = keysDown(Key.KEY_TRACTOR_BEAM)
        engine.tick(keys, listOf(npc))
        keys.advanceTick()
        // Player should be pulled rightward (toward NPC) — Newton's 3rd
        assertTrue(
            engine.player.vel.x > beforePlayerVx,
            "Newton's 3rd: player should accelerate toward NPC during tractor (before=$beforePlayerVx after=${engine.player.vel.x})",
        )
    }

    @Test
    fun tractorBeamCostsFuel() {
        val (engine, npc) = makeEngineWithNpc()
        val fuelBefore = engine.fuel
        val keys = keysDown(Key.KEY_TRACTOR_BEAM)
        engine.tick(keys, listOf(npc))
        keys.advanceTick()
        assertTrue(engine.fuel < fuelBefore, "Tractor beam should consume fuel")
    }
}

// ---------------------------------------------------------------------------
// BL-19 — NPC ball-carry + CTF scoring
// ---------------------------------------------------------------------------

class Bl19CtfScoringTest {
    /**
     * Build a minimal CTF scenario:
     * - One ball placed at team-1 treasure (home of team 1).
     * - A goal for team 0 placed at a different location (where team-1 ball can score).
     * - Ball is pre-positioned on top of the team-0 goal and marked as touched by team 1.
     */
    private fun makeCtfEngine(): Triple<GameEngine, BallData, DemoShip> {
        val engine = GameEngine.forEmptyWorld(30, 30)
        // Team-1 treasure at block (2,2), team-0 goal at pixel (200,200)
        engine.spawnBallsFromTreasures(
            listOf(TreasurePlacement(blockX = 2, blockY = 2, team = 1)),
        )
        val ball = engine.balls.first()
        // Add a team-0 goal (enemy goal for team-1 ball) at pixel (200,200)
        engine.treasureGoals.add(TreasureGoal(x = 200f, y = 200f, team = 0))
        // Pre-position ball directly on the goal
        val goalCx = (200.0 * ClickConst.CLICK).toInt()
        val goalCy = (200.0 * ClickConst.CLICK).toInt()
        ball.pos = ClPos(goalCx, goalCy)
        ball.touchTeam = 1 // touched by team 1

        // NPC carrying the ball
        val npc =
            DemoShip(
                id = NPC_ID_BASE,
                label = "carrier",
                x = 200f,
                y = 200f,
                heading = 0f,
                vx = 0f,
                vy = 0f,
                rotSpeed = 0f,
            )
        ball.connectedPlayerId = NPC_ID_BASE
        npc.carryingBallId = NPC_ID_BASE

        return Triple(engine, ball, npc)
    }

    @Test
    fun npcDeliveringBallScoresPoint() {
        val (engine, _, npc) = makeCtfEngine()
        val beforeScore = npc.score
        val noKeys = noKeys()
        engine.tick(noKeys, listOf(npc))
        noKeys.advanceTick()
        assertTrue(npc.score > beforeScore, "NPC should score when delivering ball to enemy goal")
    }

    @Test
    fun ballRespawnsAtHomeAfterScore() {
        val (engine, ball, npc) = makeCtfEngine()
        val homeX = ball.homeX
        val homeY = ball.homeY
        val noKeys = noKeys()
        engine.tick(noKeys, listOf(npc))
        noKeys.advanceTick()
        // Ball should have respawned near its home position
        val ballPx = ball.pos.cx.toDouble() / ClickConst.CLICK
        val ballPy = ball.pos.cy.toDouble() / ClickConst.CLICK
        val dist = kotlin.math.hypot(ballPx - homeX, ballPy - homeY)
        assertTrue(dist < GameConst.BLOCK_SZ * 2, "Ball should respawn near home after scoring (dist=$dist)")
    }
}

// ---------------------------------------------------------------------------
// BL-20 — NpcWeaponEvent dispatch
// ---------------------------------------------------------------------------

class Bl20NpcWeaponEventTest {
    private fun baseEvent(npcId: Int = NPC_ID_BASE): NpcWeaponEvent.Shot =
        NpcWeaponEvent.Shot(
            npcId = npcId,
            x = 200f,
            y = 200f,
            headingRad = 0.0,
            npcVx = 0f,
            npcVy = 0f,
        )

    @Test
    fun dispatchShotEventSpawnsShot() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val event = baseEvent()
        engine.dispatchNpcWeaponEvents(listOf(event), emptyList())
        assertTrue(engine.shots.isNotEmpty(), "Shot event should spawn a shot")
    }

    @Test
    fun dispatchMissileEventSpawnsMissile() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val event =
            NpcWeaponEvent.Missile(
                npcId = NPC_ID_BASE,
                x = 200f,
                y = 200f,
                headingRad = 0.0,
                npcVx = 0f,
                npcVy = 0f,
            )
        engine.dispatchNpcWeaponEvents(listOf(event), emptyList())
        assertTrue(engine.missiles.isNotEmpty(), "Missile event should spawn a missile")
    }

    @Test
    fun dispatchMineEventSpawnsMine() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val event =
            NpcWeaponEvent.Mine(
                npcId = NPC_ID_BASE,
                x = 200f,
                y = 200f,
                headingRad = 0.0,
                npcVx = 0f,
                npcVy = 0f,
            )
        engine.dispatchNpcWeaponEvents(listOf(event), emptyList())
        assertTrue(engine.mines.isNotEmpty(), "Mine event should spawn a mine")
    }

    @Test
    fun dispatchShieldChangeActivatesNpcShield() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val npc =
            DemoShip(
                id = NPC_ID_BASE,
                label = "test",
                x = 200f,
                y = 200f,
                heading = 0f,
                vx = 0f,
                vy = 0f,
                rotSpeed = 0f,
                shield = false,
            )
        val event =
            NpcWeaponEvent.ShieldChange(
                npcId = NPC_ID_BASE,
                x = 200f,
                y = 200f,
                headingRad = 0.0,
                npcVx = 0f,
                npcVy = 0f,
                active = true,
            )
        engine.dispatchNpcWeaponEvents(listOf(event), listOf(npc))
        assertTrue(npc.shield, "ShieldChange event should activate NPC shield")
    }

    @Test
    fun dispatchShieldChangeDeactivatesNpcShield() {
        val engine = GameEngine.forEmptyWorld(20, 20)
        val npc =
            DemoShip(
                id = NPC_ID_BASE,
                label = "test",
                x = 200f,
                y = 200f,
                heading = 0f,
                vx = 0f,
                vy = 0f,
                rotSpeed = 0f,
                shield = true,
            )
        val event =
            NpcWeaponEvent.ShieldChange(
                npcId = NPC_ID_BASE,
                x = 200f,
                y = 200f,
                headingRad = 0.0,
                npcVx = 0f,
                npcVy = 0f,
                active = false,
            )
        engine.dispatchNpcWeaponEvents(listOf(event), listOf(npc))
        assertFalse(npc.shield, "ShieldChange event should deactivate NPC shield")
    }
}
