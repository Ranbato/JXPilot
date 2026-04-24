package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.engine.DemoShip
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.NPC_ID_BASE
import org.lambertland.kxpilot.server.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Helpers shared across audit tests
// ---------------------------------------------------------------------------

private fun makeEngine(blocks: Int = 40): GameEngine = GameEngine.forEmptyWorld(blocks, blocks)

private fun KeyState.hold(vararg keys: Key): KeyState = also { ks -> keys.forEach { ks.press(it) } }

private fun GameEngine.tickWith(
    vararg keys: Key,
    n: Int = 1,
) {
    val ks = KeyState().hold(*keys)
    repeat(n) {
        tick(ks)
        ks.advanceTick()
    }
}

private fun npc(
    id: Int,
    x: Float = 0f,
    y: Float = 0f,
    hp: Float = org.lambertland.kxpilot.engine.EngineConst.NPC_INITIAL_HP,
) = DemoShip(
    // Caller passes a small number; we offset by NPC_ID_BASE so the id can never
    // equal player.id (1) or NO_ID (0).
    id = NPC_ID_BASE + id,
    label = "bot${NPC_ID_BASE + id}",
    x = x,
    y = y,
    heading = 0f,
    vx = 0f,
    vy = 0f,
    rotSpeed = 0f,
    hp = hp,
)

// ---------------------------------------------------------------------------
// #8 Thrust fuel drain
// ---------------------------------------------------------------------------

class ThrustFuelDrainTest {
    @Test
    fun thrustDecreaseFuel() {
        val engine = makeEngine()
        val fuelBefore = engine.fuel
        engine.tickWith(Key.KEY_THRUST, n = 60)
        assertTrue(engine.fuel < fuelBefore, "Fuel should decrease when thrusting")
    }

    @Test
    fun thrustAtZeroFuelStillAccelerates() {
        // With zero fuel the engine enters "fumes" mode and applies a reduced thrust
        // (FUMES_THRUST_FRACTION * full thrust).  Velocity must still increase.
        val engine = makeEngine()
        engine.fuel = 0.0 // force fuel to zero without killing the player
        val vxBefore = engine.player.vel.x
        val ks = KeyState().hold(Key.KEY_THRUST)
        engine.tick(ks)
        ks.advanceTick()
        // Player is facing right (heading 0); vx should have increased slightly
        assertTrue(
            engine.player.vel.x > vxBefore,
            "Fumes thrust should still produce positive vx at zero fuel, got ${engine.player.vel.x}",
        )
    }
}

// ---------------------------------------------------------------------------
// #1 Shield / weapon lockout
// ---------------------------------------------------------------------------

class ShieldWeaponLockoutTest {
    @Test
    fun cannotFireShotWithShieldUp() {
        val engine = makeEngine()
        // Press both shield and fire at the same time (justPressed == true for fire)
        val ks = KeyState().hold(Key.KEY_SHIELD, Key.KEY_FIRE_SHOT)
        engine.tick(ks)
        ks.advanceTick()
        assertEquals(0, engine.shots.size, "Should not spawn shot while shield is active")
    }

    @Test
    fun canFireShotWithShieldDown() {
        val engine = makeEngine()
        val ks = KeyState().hold(Key.KEY_FIRE_SHOT)
        engine.tick(ks)
        ks.advanceTick()
        assertEquals(1, engine.shots.size, "Should spawn shot when shield is down")
    }

    @Test
    fun cannotDropMineWithShieldUp() {
        val engine = makeEngine()
        val ks = KeyState().hold(Key.KEY_SHIELD, Key.KEY_DROP_MINE)
        engine.tick(ks)
        ks.advanceTick()
        assertEquals(0, engine.mines.size, "Should not drop mine while shield is active")
    }

    @Test
    fun cannotFireMissileWithShieldUp() {
        val engine = makeEngine()
        val ks = KeyState().hold(Key.KEY_SHIELD, Key.KEY_FIRE_MISSILE)
        engine.tick(ks)
        ks.advanceTick()
        assertEquals(0, engine.missiles.size, "Should not fire missile while shield is active")
    }
}

// ---------------------------------------------------------------------------
// #6 NPC HP and kill
// ---------------------------------------------------------------------------

class NpcHpAndKillTest {
    @Test
    fun npcHpDecreasesOnShotHit() {
        val engine = makeEngine()
        // Place NPC directly in front of player (player at world centre, facing right)
        // Shot spawns 21px ahead of player, moves another 21px per tick; NPC at 45px
        // puts it within the 18px collision radius after the first full shot movement.
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val npc = npc(id = 10, x = cx.toFloat() + 45f, y = cy.toFloat())
        val npcs = mutableListOf(npc)
        // Fire a shot
        val fireKs = KeyState().hold(Key.KEY_FIRE_SHOT)
        engine.tick(fireKs, npcs)
        fireKs.advanceTick()
        // Advance until shot travels to NPC or expires
        val noKeys = KeyState()
        repeat(10) {
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }
        assertTrue(npc.hp < org.lambertland.kxpilot.engine.EngineConst.NPC_INITIAL_HP, "NPC HP should decrease when hit by a shot")
    }

    @Test
    fun playerScoreIncreasesOnNpcKill() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        // Very low hp NPC at 45px ahead of player (shot spawns 21px ahead, moves 21px more)
        val npc = npc(id = 11, x = cx.toFloat() + 45f, y = cy.toFloat())
        npc.hp = 1f // one shot kill
        val npcs = mutableListOf(npc)
        val scoreBefore = engine.player.score
        val fireKs = KeyState().hold(Key.KEY_FIRE_SHOT)
        engine.tick(fireKs, npcs)
        fireKs.advanceTick()
        val noKeys = KeyState()
        repeat(10) {
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }
        assertTrue(engine.player.score > scoreBefore, "Score should increase when NPC is killed")
    }
}

// ---------------------------------------------------------------------------
// #3 Shield absorption fuel cost
// ---------------------------------------------------------------------------

class ShieldAbsorptionFuelDrainTest {
    @Test
    fun shieldAbsorbsShotWithFuelCost() {
        val engine = makeEngine()
        val px = engine.player.pos.cx
        val py = engine.player.pos.cy
        // Inject an enemy shot directly on top of the player so it hits on tick 1
        engine.shots.add(
            org.lambertland.kxpilot.engine.ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(px, py),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 30f,
                ownerId = (NPC_ID_BASE + 50).toShort(),
            ),
        )
        val fuelBefore = engine.fuel
        val shieldKs = KeyState().hold(Key.KEY_SHIELD)
        engine.tick(shieldKs, emptyList())
        shieldKs.advanceTick()

        // Shield must have absorbed the shot: shot list should now be empty
        val npcShots = engine.shots.filter { it.ownerId != engine.player.id.toShort() }
        assertEquals(0, npcShots.size, "Shield should absorb (remove) the incoming shot")
        // Player must still be alive
        assertEquals(
            org.lambertland.kxpilot.server.PlayerState.ALIVE,
            engine.player.plState,
            "Shield should prevent player death",
        )
        // Fuel must have decreased by shield drain + absorption cost
        assertTrue(engine.fuel < fuelBefore, "Fuel should decrease when shield absorbs a shot")
    }
}

// ---------------------------------------------------------------------------
// #11 Turn inertia
// ---------------------------------------------------------------------------

class TurnInertiaTest {
    @Test
    fun headingChangesWhenTurning() {
        val engine = makeEngine()
        val initialDir = engine.player.floatDir
        val ks = KeyState().hold(Key.KEY_TURN_LEFT)
        repeat(10) {
            engine.tick(ks)
            ks.advanceTick()
        }
        assertTrue(
            engine.player.floatDir != initialDir,
            "Heading should change after turning left for 10 ticks",
        )
    }

    @Test
    fun turnStopsImmediatelyAfterKeyRelease() {
        // C behaviour: heading changes are direct (no angular momentum).
        // After releasing the turn key the heading must not change on the next tick.
        val engine = makeEngine()
        val ks = KeyState().hold(Key.KEY_TURN_LEFT)
        repeat(20) {
            engine.tick(ks)
            ks.advanceTick()
        }
        val dirAfterHold = engine.player.floatDir

        // Release key — heading must be exactly the same after the next tick.
        val noKeys = KeyState()
        engine.tick(noKeys)
        noKeys.advanceTick()
        val dirAfterRelease = engine.player.floatDir

        assertEquals(
            dirAfterHold,
            dirAfterRelease,
            "Heading must not change after releasing turn key (C direct-step, no inertia)",
        )
    }
}

// ---------------------------------------------------------------------------
// #19 Respawn delay
// ---------------------------------------------------------------------------

class RespawnDelayTest {
    @Test
    fun deathTicksDecreaseAfterDeath() {
        val engine = makeEngine()
        // Inject an enemy shot on top of the player
        val px = engine.player.pos.cx
        val py = engine.player.pos.cy
        engine.shots.add(
            org.lambertland.kxpilot.engine.ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(px, py),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 5f,
                ownerId = 99,
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(PlayerState.KILLED, engine.player.plState, "Player should be killed by point-blank shot")
        val delay1 = engine.deathTicksRemaining
        assertTrue(delay1 > 0, "deathTicksRemaining should be > 0 after death")

        // Tick again (while dead) — counter should decrease
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.deathTicksRemaining < delay1, "deathTicksRemaining should decrease each tick")
    }

    @Test
    fun respawnResetsDeathTimer() {
        val engine = makeEngine()
        // Force kill
        engine.shots.add(
            org.lambertland.kxpilot.engine.ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 5f,
                ownerId = 99,
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertTrue(engine.deathTicksRemaining > 0)

        // Tick out the respawn delay so the gate allows respawn
        val noKeys2 = KeyState()
        repeat(engine.deathTicksRemaining) {
            engine.tick(noKeys2)
            noKeys2.advanceTick()
        }
        assertEquals(0, engine.deathTicksRemaining, "delay should have elapsed before respawn")

        val respawned = engine.respawn()
        assertTrue(respawned, "respawn() should return true when delay is elapsed")
        assertEquals(0, engine.deathTicksRemaining, "deathTicksRemaining should be 0 after respawn")
        assertEquals(PlayerState.ALIVE, engine.player.plState)
    }
}

// ---------------------------------------------------------------------------
// #20 Mines cleared on death
// ---------------------------------------------------------------------------

class MinesClearedOnDeathTest {
    @Test
    fun ownMinesClearedWhenPlayerDies() {
        val engine = makeEngine()
        // Drop a mine
        val dropKs = KeyState().hold(Key.KEY_DROP_MINE)
        engine.tick(dropKs)
        dropKs.advanceTick()
        assertEquals(1, engine.mines.size, "Should have one mine after dropping")

        // Kill player with an enemy shot
        engine.shots.add(
            org.lambertland.kxpilot.engine.ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 5f,
                ownerId = 99,
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys)
        noKeys.advanceTick()
        assertEquals(PlayerState.KILLED, engine.player.plState)
        assertEquals(0, engine.mines.size, "Own mines should be cleared when player dies")
    }
}

// ---------------------------------------------------------------------------
// #15 Missile velocity inheritance
// ---------------------------------------------------------------------------

class MissileVelocityInheritanceTest {
    @Test
    fun missileSpawnPositionOffsetByPlayerVelocity() {
        val engine = makeEngine()
        // Give player rightward velocity of 5 px/tick
        engine.player.vel =
            org.lambertland.kxpilot.common
                .Vector(5f, 0f)
        // Capture player position BEFORE the tick (missile spawn uses position + vel offset)
        val playerCxBefore = engine.player.pos.cx

        val fireKs = KeyState().hold(Key.KEY_FIRE_MISSILE)
        engine.tick(fireKs)
        fireKs.advanceTick()

        assertTrue(engine.missiles.isNotEmpty(), "Should have fired a missile")
        val missileCx =
            engine.missiles
                .first()
                .pos.cx
        // The missile should be ahead of the player's pre-fire position by ~vel offset
        // (5 px * 64 clicks/px = 320 clicks to the right)
        val clickOffset = missileCx - playerCxBefore
        assertTrue(clickOffset > 0, "Missile should be spawned ahead (rightward) of pre-fire position, offset=$clickOffset")
    }

    @Test
    fun missileSpawnNotOffsetWhenPlayerStationary() {
        // With vel=0 the missile spawns at the player's position, then moves
        // SMART_SHOT_MIN_SPEED + SMART_SHOT_ACC pixels forward (heading=0, rightward)
        // in the first tick (speed ramps from MIN_SPEED by ACC each tick).
        // C: missiles start at SMART_SHOT_MIN_SPEED and accelerate at SMART_SHOT_ACC
        // per tick (serverconst.h:175-177).
        val engine = makeEngine()
        engine.player.vel =
            org.lambertland.kxpilot.common
                .Vector(0f, 0f)
        val playerCxBefore = engine.player.pos.cx

        val fireKs = KeyState().hold(Key.KEY_FIRE_MISSILE)
        engine.tick(fireKs)
        fireKs.advanceTick()

        assertTrue(engine.missiles.isNotEmpty())
        val missileCx =
            engine.missiles
                .first()
                .pos.cx

        // First-tick speed = MIN_SPEED + ACC (unguided ramp, no target present)
        val firstTickSpeed =
            org.lambertland.kxpilot.engine.EngineConst.SMART_SHOT_MIN_SPEED +
                org.lambertland.kxpilot.engine.EngineConst.SMART_SHOT_ACC
        val expectedCx =
            playerCxBefore +
                (firstTickSpeed * org.lambertland.kxpilot.common.ClickConst.CLICK).toInt()
        assertEquals(
            expectedCx,
            missileCx,
            "With vel=0 missile should move SMART_SHOT_MIN_SPEED+ACC in first tick (C acc ramp)",
        )
    }
}

// ---------------------------------------------------------------------------
// #9 Tractor beam fuel cost
// ---------------------------------------------------------------------------

class TractorFuelCostTest {
    @Test
    fun tractorBeamDrainsFuel() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val npc = npc(id = 20, x = cx.toFloat() + 100f, y = cy.toFloat())
        val npcs = mutableListOf(npc)

        // Lock onto the NPC
        engine.lockNext(npcs)

        val fuelBefore = engine.fuel
        val tractorKs = KeyState().hold(Key.KEY_TRACTOR_BEAM)
        repeat(20) {
            engine.tick(tractorKs, npcs)
            tractorKs.advanceTick()
        }
        assertTrue(engine.fuel < fuelBefore, "Tractor beam should drain fuel")
    }
}
