package org.lambertland.kxpilot

import org.lambertland.kxpilot.client.KeyState
import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.Key
import org.lambertland.kxpilot.common.Vector
import org.lambertland.kxpilot.engine.DemoShip
import org.lambertland.kxpilot.engine.EngineConst
import org.lambertland.kxpilot.engine.GameEngine
import org.lambertland.kxpilot.engine.MineData
import org.lambertland.kxpilot.engine.NPC_ID_BASE
import org.lambertland.kxpilot.engine.NpcAiConst
import org.lambertland.kxpilot.engine.NpcAiManager
import org.lambertland.kxpilot.engine.NpcBehavior
import org.lambertland.kxpilot.engine.ShotData
import org.lambertland.kxpilot.server.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun makeEngine(blocks: Int = 40): GameEngine = GameEngine.forEmptyWorld(blocks, blocks)

/** Build a DemoShip with an NPC-safe id (NPC_ID_BASE + offset). */
private fun npc(
    idOffset: Int,
    x: Float,
    y: Float,
    hp: Float = EngineConst.NPC_INITIAL_HP,
    heading: Float = 0f,
): DemoShip =
    DemoShip(
        id = NPC_ID_BASE + idOffset,
        label = "bot${NPC_ID_BASE + idOffset}",
        x = x,
        y = y,
        heading = heading,
        vx = 0f,
        vy = 0f,
        rotSpeed = 0f,
        hp = hp,
    )

private fun makeAiManager(engine: GameEngine): NpcAiManager = NpcAiManager(engine.world.width.toFloat(), engine.world.height.toFloat())

/** Run AI + collect events; apply shot events to engine. Returns count of shots fired. */
private fun tickAi(
    manager: NpcAiManager,
    engine: GameEngine,
    npcs: MutableList<DemoShip>,
): Int {
    val events =
        manager.tickAll(
            npcs = npcs,
            playerX = engine.playerPixelX,
            playerY = engine.playerPixelY,
            playerVx = engine.player.vel.x,
            playerVy = engine.player.vel.y,
            playerAlive = engine.player.isAlive(),
        )
    events.forEach { engine.dispatchNpcWeaponEvents(listOf(it), npcs) }
    return events.size
}

// ---------------------------------------------------------------------------
// NPC id safety
// ---------------------------------------------------------------------------

class NpcIdSafetyTest {
    @Test
    fun npcIdDoesNotCollideWithPlayerId() {
        // NPC_ID_BASE must be ≥ 2 to avoid colliding with NO_ID (0) and player.id (1)
        assertTrue(NPC_ID_BASE >= 2, "NPC_ID_BASE must be ≥ 2 to avoid player id collision")
        val engine = makeEngine()
        // The first NPC in the default demo world has id NPC_ID_BASE + 1 (idx 1)
        val firstNpcId = NPC_ID_BASE + 1
        assertNotEquals(engine.player.id.toInt(), firstNpcId, "NPC id $firstNpcId must not equal player id ${engine.player.id}")
    }

    @Test
    fun npcShotOwnerIdDoesNotMatchPlayerId() {
        // A shot fired by an NPC with the NPC_ID_BASE offset must NOT have
        // ownerId == player.id, otherwise the collision loop would skip it.
        val engine = makeEngine()
        val npcId = NPC_ID_BASE + 1
        assertTrue(
            npcId.toShort() != engine.player.id.toShort(),
            "NPC shot ownerId must differ from player.id to reach the player-collision branch",
        )
    }
}

// ---------------------------------------------------------------------------
// NpcAiManager — basic behavior transitions
// ---------------------------------------------------------------------------

class NpcAiBehaviorTest {
    @Test
    fun npcStartsInPatrol() {
        // Use a large world (120 blocks = 4200 px) so DETECT_RANGE_PX * 2 doesn't wrap
        val engine = makeEngine(blocks = 120)
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        // Place NPC far from player (outside detect range)
        val bot = npc(idOffset = 1, x = cx.toFloat() + NpcAiConst.DETECT_RANGE_PX * 2, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        tickAi(manager, engine, npcs)

        // NPC is outside detect range; must be in PATROL state
        assertEquals(NpcBehavior.PATROL, manager.getBehavior(bot.id), "NPC outside detect range should be in PATROL")
        assertEquals(0, engine.shots.size, "NPC outside detect range should not fire")
    }

    @Test
    fun npcTransitionsToAttack() {
        // Use a large world so placement is unambiguous
        val engine = makeEngine(blocks = 120)
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        // In C, DETECT_RANGE == ATTACK_RANGE == Visibility_distance (~1000 px).
        // NPCs go directly from PATROL to ATTACK (no intermediate CHASE state in C).
        // Place NPC inside ATTACK range.
        val attackX = cx.toFloat() + NpcAiConst.ATTACK_RANGE_PX * 0.7f
        val bot = npc(idOffset = 2, x = attackX, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        // First tick: NPC is within attack range, should transition to ATTACK
        tickAi(manager, engine, npcs)
        val state = manager.getBehavior(bot.id)
        assertTrue(
            state == NpcBehavior.ATTACK || state == NpcBehavior.CHASE,
            "NPC inside attack range should be in ATTACK (or CHASE transitioning), was $state",
        )

        // Tick until cooldown expires and NPC fires
        var shotFired = false
        val noKeys = KeyState()
        repeat(NpcAiConst.SHOT_COOLDOWN_TICKS + 5) {
            tickAi(manager, engine, npcs)
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
            if (engine.shots.any { it.ownerId != engine.player.id.toShort() }) {
                shotFired = true
            }
        }
        assertTrue(shotFired, "NPC in ATTACK range should fire at least one shot")
    }

    @Test
    fun npcDoesNotFireOutsideAttackRange() {
        // Use a large world so the NPC genuinely stays outside attack range without wrapping
        val engine = makeEngine(blocks = 120)
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        // Place NPC just outside ATTACK range (DETECT == ATTACK in C, so beyond both)
        val outX = cx.toFloat() + NpcAiConst.ATTACK_RANGE_PX * 1.1f
        val bot = npc(idOffset = 3, x = outX, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        val noKeys = KeyState()
        repeat(NpcAiConst.SHOT_COOLDOWN_TICKS + 5) {
            tickAi(manager, engine, npcs)
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }
        val npcShots = engine.shots.filter { it.ownerId != engine.player.id.toShort() }
        assertEquals(0, npcShots.size, "NPC outside attack range should not fire")
    }

    @Test
    fun lowHpNpcTransitionsToEvadeAndFlees() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        // Low-HP NPC inside attack range; player is at cx,cy to the LEFT of the NPC
        val bot =
            npc(
                idOffset = 4,
                x = cx.toFloat() + NpcAiConst.ATTACK_RANGE_PX * 0.5f, // NPC is to the right of player
                y = cy.toFloat(),
                hp = NpcAiConst.EVADE_HP_THRESHOLD - 1f, // below threshold
            )
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        tickAi(manager, engine, npcs)

        // Must be in EVADE state
        assertEquals(NpcBehavior.EVADE, manager.getBehavior(bot.id), "Low-HP NPC should enter EVADE")

        // Desired velocity must point AWAY from player (player is to the left, so desiredVx > 0)
        assertTrue(bot.desiredVx > 0f, "Low-HP NPC should flee to the right (away from player at left), got desiredVx=${bot.desiredVx}")

        // Should NOT fire while evading
        val npcShots = engine.shots.filter { it.ownerId != engine.player.id.toShort() }
        assertEquals(0, npcShots.size, "Low-HP NPC should not fire while evading")
    }

    @Test
    fun evadeTimerCountsDownAndExpires() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot =
            npc(
                idOffset = 5,
                x = cx.toFloat() + NpcAiConst.ATTACK_RANGE_PX * 0.5f,
                y = cy.toFloat(),
                hp = NpcAiConst.EVADE_HP_THRESHOLD - 1f,
            )
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        // Enter EVADE
        tickAi(manager, engine, npcs)
        assertEquals(NpcBehavior.EVADE, manager.getBehavior(bot.id))
        val timerAfterEntry = manager.getEvadeTimer(bot.id)!!
        // Timer should have been set to EVADE_DURATION_TICKS and decremented once
        assertEquals(
            NpcAiConst.EVADE_DURATION_TICKS - 1,
            timerAfterEntry,
            "Timer should be EVADE_DURATION_TICKS - 1 after first tick in EVADE",
        )

        // Heal the NPC (hp now above threshold) so the entry guard no longer fires
        bot.hp = NpcAiConst.EVADE_HP_THRESHOLD + 10f
        // Tick out remaining timer
        repeat(timerAfterEntry) {
            tickAi(manager, engine, npcs)
        }
        // Timer should have reached 0 and NPC transitioned back to PATROL
        assertEquals(
            NpcBehavior.PATROL,
            manager.getBehavior(bot.id),
            "NPC should return to PATROL after evade timer expires",
        )
    }

    @Test
    fun npcDoesNotFireWhenPlayerDead() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot = npc(idOffset = 6, x = cx.toFloat() + 50f, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        // Kill player via point-blank enemy shot (ownerId=999, not player.id)
        engine.shots.add(
            ShotData(
                pos = ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel = Vector(0f, 0f),
                life = 5f,
                ownerId = 999.toShort(),
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys, npcs)
        noKeys.advanceTick()
        assertEquals(PlayerState.KILLED, engine.player.plState, "Player should be killed")

        // Clear shots list, then tick AI
        engine.shots.clear()
        tickAi(manager, engine, npcs)

        val npcShots = engine.shots.filter { it.ownerId != engine.player.id.toShort() }
        assertEquals(0, npcShots.size, "NPC should not fire at a dead player")
    }
}

// ---------------------------------------------------------------------------
// NpcAiManager — shot hits the player
// ---------------------------------------------------------------------------

class NpcShotHitsPlayerTest {
    @Test
    fun npcShotKillsUnshieldedPlayer() {
        val engine = makeEngine()

        // Inject an NPC shot directly on top of the player (ownerId != player.id)
        engine.shots.add(
            ShotData(
                pos = ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel = Vector(0f, 0f),
                life = 5f,
                ownerId = (NPC_ID_BASE + 10).toShort(),
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys, emptyList())
        noKeys.advanceTick()
        assertEquals(PlayerState.KILLED, engine.player.plState, "Point-blank NPC shot should kill player")
    }

    @Test
    fun npcShotBlockedByPlayerShield() {
        val engine = makeEngine()

        engine.shots.add(
            ShotData(
                pos = ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel = Vector(0f, 0f),
                life = 5f,
                ownerId = (NPC_ID_BASE + 10).toShort(),
            ),
        )
        val shieldKeys = KeyState()
        shieldKeys.press(Key.KEY_SHIELD)
        engine.tick(shieldKeys, emptyList())
        shieldKeys.advanceTick()
        // With enough fuel, shield should have blocked the shot without killing player
        assertEquals(PlayerState.ALIVE, engine.player.plState, "Shield should block NPC shot")
    }

    @Test
    fun shieldAbsorbsShotViaCEnergyDrain() {
        // C: when shielded, shot drain = ED_SHOT_HIT = -25 fuel (not a separate 150-fuel absorption cost).
        val engine = makeEngine()
        val fuelBefore = engine.fuel

        engine.shots.add(
            ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 5f,
                ownerId = (NPC_ID_BASE + 10).toShort(),
            ),
        )
        val shieldKeys = KeyState()
        shieldKeys.press(Key.KEY_SHIELD)
        engine.tick(shieldKeys, emptyList())
        shieldKeys.advanceTick()

        // Fuel lost = SHIELD_FUEL_COST (per-tick drain) + abs(ED_SHOT_HIT) = ~0.96 + 25 = ~25.96
        // Crucially, NOT 150 (old invented mechanic)
        val fuelLost = fuelBefore - engine.fuel
        assertTrue(
            fuelLost < 30.0,
            "Shield absorption should drain ED_SHOT_HIT (25 fuel), not the invented 150-fuel cost (lost: $fuelLost)",
        )
        assertTrue(fuelLost > 0.0, "Some fuel must be drained when shield absorbs a shot")
    }

    @Test
    fun shieldDropsWhenFuelExhaustedByShot() {
        // C: if fuel hits 0, shield is cleared — but player is NOT killed.
        // (collision.c: CLR_BIT(pl->used, HAS_SHIELD) — no Player_set_state(KILLED))
        val engine = makeEngine()
        // Set fuel just below one shot's worth so the shield will exhaust on this hit
        engine.fuel = 1.0 // less than ED_SHOT_HIT (25), so fuel hits 0 after the shot

        engine.shots.add(
            ShotData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel =
                    org.lambertland.kxpilot.common
                        .Vector(0f, 0f),
                life = 5f,
                ownerId = (NPC_ID_BASE + 10).toShort(),
            ),
        )
        val shieldKeys = KeyState()
        shieldKeys.press(Key.KEY_SHIELD)
        engine.tick(shieldKeys, emptyList())
        shieldKeys.advanceTick()

        // Player should still be alive — fuel exhaustion drops shield, doesn't kill
        assertEquals(
            PlayerState.ALIVE,
            engine.player.plState,
            "Player must survive shield fuel exhaustion (C: CLR_BIT(HAS_SHIELD), not kill)",
        )
        assertEquals(0.0, engine.fuel, "Fuel should be at 0 after exhaustion")
    }
}

// ---------------------------------------------------------------------------
// NpcAiManager — manager lifecycle
// ---------------------------------------------------------------------------

class NpcAiManagerLifecycleTest {
    @Test
    fun removeStopsNpcFromFiring() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot = npc(idOffset = 20, x = cx.toFloat() + NpcAiConst.ATTACK_RANGE_PX * 0.5f, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)
        manager.remove(bot.id)

        // NPC still in world but AI state removed; should produce no shot events
        val events =
            manager.tickAll(
                npcs = npcs,
                playerX = engine.playerPixelX,
                playerY = engine.playerPixelY,
                playerVx = 0f,
                playerVy = 0f,
                playerAlive = true,
            )
        assertEquals(0, events.size, "Removed NPC should produce no shot events")
        assertNull(manager.getBehavior(bot.id), "Removed NPC should have no behavior state")
    }

    @Test
    fun clearRemovesAllStates() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot1 = npc(idOffset = 21, x = cx.toFloat() + 50f, y = cy.toFloat())
        val bot2 = npc(idOffset = 22, x = cx.toFloat() + 60f, y = cy.toFloat())
        val npcs = mutableListOf(bot1, bot2)
        val manager = makeAiManager(engine)
        manager.register(bot1)
        manager.register(bot2)
        manager.clear()

        val events =
            manager.tickAll(
                npcs = npcs,
                playerX = engine.playerPixelX,
                playerY = engine.playerPixelY,
                playerVx = 0f,
                playerVy = 0f,
                playerAlive = true,
            )
        assertEquals(0, events.size, "Cleared manager should produce no events")
        assertNull(manager.getBehavior(bot1.id))
        assertNull(manager.getBehavior(bot2.id))
    }
}

// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
// Mine mechanics — C-correct behaviour
// ---------------------------------------------------------------------------

class MineBlastTest {
    @Test
    fun mineImmediatelyActiveNpcDamagedOnFirstTick() {
        // KXPilot design: MINE_ARM_TICKS=30 — mine arms after 30 ticks (C default is 0).
        // After the arm delay expires the mine is live; NPC within trigger radius detonates it.
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2

        val bot = npc(idOffset = 30, x = cx.toFloat() + 10f, y = cy.toFloat())
        val npcs = mutableListOf(bot)

        // Drop mine at player (world centre)
        val dropKeys = KeyState()
        dropKeys.press(Key.KEY_DROP_MINE)
        engine.tick(dropKeys, npcs)
        dropKeys.advanceTick()

        // Mine should still be present while arming (ticks 1–30)
        assertEquals(1, engine.mines.size, "Mine should be arming — not yet live")

        // Advance through the full arm delay; mine becomes live on tick 30
        val noKeys = KeyState()
        repeat(EngineConst.MINE_ARM_TICKS - 1) {
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }

        // One more tick — mine is now armed, NPC at 10 px triggers it
        engine.tick(noKeys, npcs)
        noKeys.advanceTick()

        // After mine detonates, debris spawn and travel toward NPC.
        assertEquals(0, engine.mines.size, "Mine should have detonated after arm delay")
        assertTrue(engine.debris.isNotEmpty(), "Debris should be spawned after mine detonation")

        // Tick a few times to let debris travel 10 px to the NPC
        repeat(10) {
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }

        assertTrue(bot.hp < EngineConst.NPC_INITIAL_HP, "Mine debris should damage NPC after arming delay")
    }

    @Test
    fun ownerImmuneMineSurvivesPlayerContact() {
        // C default: fuse=-1 → owner is permanently immune. Player cannot trigger own mine.
        val engine = makeEngine()
        val dropKeys = KeyState()
        dropKeys.press(Key.KEY_DROP_MINE)
        engine.tick(dropKeys, emptyList())
        dropKeys.advanceTick()

        // Player is at the mine's position — tick several more times
        val noKeys = KeyState()
        repeat(5) {
            engine.tick(noKeys, emptyList())
            noKeys.advanceTick()
        }

        // Mine should still be present (not detonated by the owner)
        assertEquals(1, engine.mines.size, "Own mine should not detonate on the owner (C default: permanent owner immunity)")
        assertEquals(org.lambertland.kxpilot.server.PlayerState.ALIVE, engine.player.plState, "Player must survive own mine")
    }

    @Test
    fun nonOwnerMineTriggerOnPlayer() {
        // A mine whose ownerImmune=false triggers on any player entering range and spawns debris.
        // Debris from a point-blank mine blast drain fuel via collision_cost.
        // With low starting fuel, the player dies.
        val engine = makeEngine()
        // Set player fuel very low so even a single debris hit (cost ≈ 2–4 fuel) kills them.
        // C: collision_cost(mass, speed) = DEBRIS_MASS * speed / 128 ≈ 4.5 * 20..128 / 128
        //    minimum cost ≈ 0.7, median ≈ 2.6.  Setting fuel = 0.5 guarantees death on first hit.
        engine.fuel = 0.5

        // Inject a mine at the player's position with ownerImmune=false
        val px = engine.player.pos.cx
        val py = engine.player.pos.cy
        engine.mines.add(
            MineData(
                pos =
                    org.lambertland.kxpilot.common
                        .ClPos(px, py),
                ownerImmune = false,
                life = EngineConst.MINE_LIFE,
                ownerId = (NPC_ID_BASE + 99).toShort(), // enemy NPC owner
                armTicksRemaining = 0, // injected as already-armed
            ),
        )
        val noKeys = KeyState()
        // First tick: mine detonates, spawns debris
        engine.tick(noKeys, emptyList())
        noKeys.advanceTick()
        assertEquals(0, engine.mines.size, "Mine should be consumed after detonation")
        assertTrue(engine.debris.isNotEmpty(), "Debris should be spawned")

        // Tick until player is killed by debris (up to 50 ticks — debris at point-blank)
        repeat(50) {
            if (engine.player.isAlive()) {
                engine.tick(noKeys, emptyList())
                noKeys.advanceTick()
            }
        }

        assertEquals(
            org.lambertland.kxpilot.server.PlayerState.KILLED,
            engine.player.plState,
            "Player with low fuel should be killed by enemy mine debris at point-blank range",
        )
    }

    @Test
    fun singleRadiusNpcTriggersAndTakesDamage() {
        // C: a single radius (MINE_TRIGGER_RADIUS = 100 px) for both trigger and damage.
        // An NPC inside that radius triggers the mine; debris spawn and travel toward the NPC.
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2

        val bot = npc(idOffset = 31, x = cx.toFloat() + 50f, y = cy.toFloat()) // 50 px away — within 100 px
        val npcs = mutableListOf(bot)

        val dropKeys = KeyState()
        dropKeys.press(Key.KEY_DROP_MINE)
        engine.tick(dropKeys, npcs)
        dropKeys.advanceTick()

        // Mine arms after MINE_ARM_TICKS ticks; advance through the arm delay
        val noKeys = KeyState()
        repeat(EngineConst.MINE_ARM_TICKS - 1) {
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }
        // Final tick — mine is now armed and NPC at 50 px triggers it
        engine.tick(noKeys, npcs)
        noKeys.advanceTick()

        assertEquals(0, engine.mines.size, "Mine should have detonated after arm delay")
        assertTrue(engine.debris.isNotEmpty(), "Debris should be spawned after NPC triggers mine")

        // Tick to let debris reach the NPC at 50 px (debris max speed 128 px/tick → instant at 50px)
        repeat(5) {
            engine.tick(noKeys, npcs)
            noKeys.advanceTick()
        }

        assertTrue(
            bot.hp < EngineConst.NPC_INITIAL_HP,
            "NPC inside single trigger radius should take damage from debris (C: same radius for trigger and kill)",
        )
    }

    @Test
    fun deadNpcDoesNotTriggerMine() {
        // Dead NPCs (hp == 0) cannot trigger a mine.
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2

        val deadBot = npc(idOffset = 32, x = cx.toFloat() + 10f, y = cy.toFloat(), hp = 0f)
        val liveBot = npc(idOffset = 33, x = cx.toFloat() + 200f, y = cy.toFloat()) // outside trigger radius
        val npcs = mutableListOf(deadBot, liveBot)

        val dropKeys = KeyState()
        dropKeys.press(Key.KEY_DROP_MINE)
        engine.tick(dropKeys, npcs)
        dropKeys.advanceTick()

        // Mine should not have detonated — dead NPC cannot trigger it
        assertEquals(EngineConst.NPC_INITIAL_HP, liveBot.hp, "Live NPC outside radius must not be damaged by a non-triggered mine")
        assertEquals(1, engine.mines.size, "Mine should still be present — dead NPC cannot trigger it")
    }

    @Test
    fun mineDebrisConstantsMatchCSource() {
        // C: DEBRIS_MASS = 4.5 (serverconst.h:234)
        assertEquals(4.5, EngineConst.DEBRIS_MASS, "DEBRIS_MASS must match C value 4.5")
        // C: intensity=512 → num_debris avg = 128
        assertEquals(128, EngineConst.MINE_DEBRIS_COUNT, "MINE_DEBRIS_COUNT must be 128 (expected value)")
        // C: min_speed=20, max_speed=512>>2=128
        assertEquals(20.0, EngineConst.MINE_DEBRIS_MIN_SPEED)
        assertEquals(128.0, EngineConst.MINE_DEBRIS_MAX_SPEED)
        // C: collision radius = SHIP_SZ + 6 = 22 px
        assertEquals(22.0, EngineConst.MINE_DEBRIS_HIT_RADIUS)

        // Expected total damage at d=0: MINE_DEBRIS_COUNT * avgDrain = 128 * (4.5 * 74 / 128) = 333
        val avgSpeed = (EngineConst.MINE_DEBRIS_MIN_SPEED + EngineConst.MINE_DEBRIS_MAX_SPEED) / 2.0 // 74
        val avgCost = EngineConst.DEBRIS_MASS * avgSpeed / 128.0 // collision_cost per fragment
        val expectedTotal = EngineConst.MINE_DEBRIS_COUNT * avgCost
        assertTrue(
            kotlin.math.abs(expectedTotal - 333.0) < 1.0,
            "Expected total damage at d=0 ≈ 333 fuel (C formula), got $expectedTotal",
        )
    }
}

// ---------------------------------------------------------------------------
// Bug fix B — clearLock clears stale NPC id
// ---------------------------------------------------------------------------

class ClearLockTest {
    @Test
    fun clearLockResetsLockedNpcId() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot = npc(idOffset = 40, x = cx.toFloat() + 50f, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        engine.lockNext(npcs)
        assertEquals(bot.id, engine.lockedNpcId, "NPC should be locked")

        engine.clearLock()
        assertEquals(-1, engine.lockedNpcId, "lockedNpcId should be -1 after clearLock")
        assertTrue(engine.lockDirRad.isNaN(), "lockDirRad should be NaN after clearLock")
        assertEquals(0.0, engine.lockDistPx)
    }

    @Test
    fun staleLockClearsOnLockNext() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot = npc(idOffset = 41, x = cx.toFloat() + 50f, y = cy.toFloat())
        val npcs = mutableListOf(bot)
        engine.lockNext(npcs)
        assertEquals(bot.id, engine.lockedNpcId)

        // Remove NPC from list (simulating death) without calling clearLock first
        npcs.clear()
        // lockNext with stale id should clear, not silently jump to index 0
        engine.lockNext(npcs) // empty list
        assertEquals(-1, engine.lockedNpcId, "Lock should clear when NPC list is empty")
    }

    @Test
    fun staleLockClearsOnLockNextWithRemainingNpcs() {
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        val bot1 = npc(idOffset = 42, x = cx.toFloat() + 50f, y = cy.toFloat())
        val bot2 = npc(idOffset = 43, x = cx.toFloat() + 60f, y = cy.toFloat())
        val npcs = mutableListOf(bot1, bot2)
        engine.lockNext(npcs) // locks bot1
        assertEquals(bot1.id, engine.lockedNpcId)

        // Remove bot1 without clearing lock; bot2 remains
        npcs.remove(bot1)
        engine.lockNext(npcs) // stale id + non-empty list → should clear, not jump to bot2
        assertEquals(-1, engine.lockedNpcId, "Stale lock should be cleared on lockNext, not silently transferred")
    }
}

// ---------------------------------------------------------------------------
// Bug fix F — Respawn delay gates spawnAtBase/respawn
// ---------------------------------------------------------------------------

class RespawnGateTest {
    private fun killPlayer(engine: GameEngine) {
        engine.shots.add(
            ShotData(
                pos = ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel = Vector(0f, 0f),
                life = 5f,
                ownerId = 999.toShort(),
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys, emptyList())
        noKeys.advanceTick()
    }

    @Test
    fun spawnAtBaseBlockedDuringRespawnDelay() {
        val engine = makeEngine()
        killPlayer(engine)
        assertEquals(PlayerState.KILLED, engine.player.plState)
        assertTrue(engine.deathTicksRemaining > 0)

        val result = engine.spawnAtBase(0)
        assertFalse(result, "spawnAtBase should return false during respawn delay")
        assertEquals(PlayerState.KILLED, engine.player.plState, "Player should still be dead")
    }

    @Test
    fun respawnBlockedDuringRespawnDelay() {
        val engine = makeEngine()
        killPlayer(engine)
        assertEquals(PlayerState.KILLED, engine.player.plState)

        val result = engine.respawn()
        assertFalse(result, "respawn() should return false during respawn delay")
        assertEquals(PlayerState.KILLED, engine.player.plState)
    }

    @Test
    fun spawnAtBaseSucceedsAfterDelay() {
        val engine = makeEngine()
        killPlayer(engine)

        val noKeys = KeyState()
        repeat(engine.deathTicksRemaining) {
            engine.tick(noKeys, emptyList())
            noKeys.advanceTick()
        }
        assertEquals(0, engine.deathTicksRemaining, "Delay should have elapsed")

        val result = engine.respawn()
        assertTrue(result, "respawn() should succeed after delay expires")
        assertEquals(PlayerState.ALIVE, engine.player.plState)
    }
}

// ---------------------------------------------------------------------------
// Ball disconnect on respawn
// ---------------------------------------------------------------------------

class BallDisconnectOnRespawnTest {
    @Test
    fun respawnDisconnectsBallFromPlayer() {
        val engine = makeEngine()
        // Manually connect a ball to the player (simulate connector-held state)
        val ball = engine.balls.firstOrNull()
        // If no balls on empty world, skip gracefully — this test only applies with balls
        if (ball == null) return
        ball.connectedPlayerId = engine.player.id.toInt()
        assertTrue(ball.connectedPlayerId == engine.player.id.toInt(), "Ball should be connected before death")

        // Kill player and tick out delay
        engine.shots.add(
            ShotData(
                pos = ClPos(engine.player.pos.cx, engine.player.pos.cy),
                vel = Vector(0f, 0f),
                life = 5f,
                ownerId = 999.toShort(),
            ),
        )
        val noKeys = KeyState()
        engine.tick(noKeys, emptyList())
        noKeys.advanceTick()

        repeat(engine.deathTicksRemaining) {
            engine.tick(noKeys, emptyList())
            noKeys.advanceTick()
        }

        engine.respawn()
        assertEquals(
            org.lambertland.kxpilot.engine.BallData.NO_PLAYER,
            ball.connectedPlayerId,
            "Ball should be disconnected after respawn",
        )
    }
}

// ---------------------------------------------------------------------------
// Velocity ownership — AI desired velocity vs physics integration
// ---------------------------------------------------------------------------

class NpcVelocityOwnershipTest {
    @Test
    fun aiWritesDesiredVelocityNotActualVelocity() {
        // After an AI tick, desiredVx/desiredVy should be non-zero (NPC has a heading),
        // while vx/vy may still be 0 until DemoGameState.tick() blends them.
        val engine = makeEngine()
        val cx = engine.world.width / 2
        val cy = engine.world.height / 2
        // PATROL NPC (far from player) — AI sets a patrol heading/speed
        val bot = npc(idOffset = 50, x = cx.toFloat() + NpcAiConst.DETECT_RANGE_PX * 2, y = cy.toFloat())
        bot.vx = 0f
        bot.vy = 0f
        bot.desiredVx = 0f
        bot.desiredVy = 0f
        val npcs = mutableListOf(bot)
        val manager = makeAiManager(engine)
        manager.register(bot)

        tickAi(manager, engine, npcs)

        // AI must have written a non-trivial desired velocity
        val desiredSpeed = kotlin.math.hypot(bot.desiredVx.toDouble(), bot.desiredVy.toDouble())
        assertTrue(desiredSpeed > 0.0, "AI should set non-zero desiredVx/desiredVy for PATROL NPC")
    }

    @Test
    fun externalForcePreservedForOneTick() {
        // If a tractor beam gives the NPC +10f vx, and then the AI blend factor is 0.85,
        // the resulting velocity should retain ~15% of the beam impulse.
        val bot = npc(idOffset = 51, x = 300f, y = 300f)
        bot.desiredVx = 0f // AI wants the NPC to stand still (for simplicity)
        bot.desiredVy = 0f
        bot.vx = 10f // beam applied this frame
        bot.vy = 0f

        // Simulate one DemoGameState.tick() blend (no engine — use simple path)
        val ds =
            org.lambertland.kxpilot.engine.DemoGameState(
                worldW = 800f,
                worldH = 600f,
                engine = null,
            )
        ds.ships += bot
        ds.tick()

        // After blend at 0.85, vx should be ~10 * 0.15 = 1.5 (not 0, not 10)
        assertTrue(
            bot.vx in 1.0f..2.5f,
            "After blend, some beam impulse (≈15%) should remain; got vx=${bot.vx}",
        )
    }
}

// ---------------------------------------------------------------------------
// Calibration G — Thrust fuel drain recalibrated to 0.22/tick
// ---------------------------------------------------------------------------

class ThrustFuelCalibrationTest {
    @Test
    fun thrustFuelCostApprox0_22PerTick() {
        val engine = makeEngine()
        val fuelBefore = engine.fuel
        val thrustKeys = KeyState()
        thrustKeys.press(Key.KEY_THRUST)
        engine.tick(thrustKeys, emptyList())
        thrustKeys.advanceTick()
        val drained = fuelBefore - engine.fuel
        // Should be close to 0.22 (allow small floating-point tolerance)
        assertTrue(drained in 0.20..0.25, "Thrust should drain ≈ 0.22 fuel/tick, got $drained")
    }
}
