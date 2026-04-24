package org.lambertland.kxpilot

import org.lambertland.kxpilot.engine.DemoShip
import org.lambertland.kxpilot.engine.EngineConst
import org.lambertland.kxpilot.engine.NPC_ID_BASE
import org.lambertland.kxpilot.engine.NpcAiConst
import org.lambertland.kxpilot.engine.NpcAiManager
import org.lambertland.kxpilot.engine.NpcBehavior
import org.lambertland.kxpilot.engine.NpcWeaponEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Build a minimal DemoShip for multi-target AI tests. */
private fun aiShip(
    idOffset: Int,
    x: Float,
    y: Float,
    hp: Float = EngineConst.NPC_INITIAL_HP,
): DemoShip =
    DemoShip(
        id = NPC_ID_BASE + idOffset,
        label = "bot${NPC_ID_BASE + idOffset}",
        x = x,
        y = y,
        heading = 0f,
        vx = 0f,
        vy = 0f,
        rotSpeed = 0f,
        hp = hp,
    )

class NpcAiMultiTargetTest {
    /**
     * Two NPCs close together, human player dead — both should transition to ATTACK
     * and fire shots.
     */
    @Test
    fun npcWithNoHumanPlayerAttacksNearestNpc() {
        val manager = NpcAiManager(worldW = 1000f, worldH = 1000f)
        val npc2 = aiShip(idOffset = 2, x = 100f, y = 100f)
        val npc3 = aiShip(idOffset = 3, x = 110f, y = 100f) // 10 px apart — within attack range
        manager.register(npc2)
        manager.register(npc3)

        // Tick enough times for PATROL → CHASE → ATTACK transition and cooldown expiry
        var allEvents = mutableListOf<NpcWeaponEvent>()
        repeat(NpcAiConst.SHOT_COOLDOWN_TICKS + 5) {
            allEvents +=
                manager.tickAll(
                    npcs = listOf(npc2, npc3),
                    playerX = 900f,
                    playerY = 900f,
                    playerVx = 0f,
                    playerVy = 0f,
                    playerAlive = false,
                )
        }

        assertEquals(NpcBehavior.ATTACK, manager.getBehavior(NPC_ID_BASE + 2), "npc2 should be in ATTACK state")
        assertEquals(NpcBehavior.ATTACK, manager.getBehavior(NPC_ID_BASE + 3), "npc3 should be in ATTACK state")

        // Both NPCs should have fired at least one shot
        val shotIds = allEvents.filterIsInstance<NpcWeaponEvent.Shot>().map { it.npcId }.toSet()
        assertTrue(NPC_ID_BASE + 2 in shotIds, "npc2 should have fired at least one shot")
        assertTrue(NPC_ID_BASE + 3 in shotIds, "npc3 should have fired at least one shot")
    }

    /** Human far away, another NPC close — NPC should target the close NPC, not the human. */
    @Test
    fun npcTargetsNearestTarget() {
        val manager = NpcAiManager(worldW = 2000f, worldH = 2000f)
        val npc2 = aiShip(idOffset = 2, x = 500f, y = 500f)
        val npc3 = aiShip(idOffset = 3, x = 510f, y = 500f) // 10 px away
        manager.register(npc2)
        manager.register(npc3)

        // Human alive but very far away
        repeat(5) {
            manager.tickAll(
                npcs = listOf(npc2, npc3),
                playerX = 1800f,
                playerY = 1800f,
                playerVx = 0f,
                playerVy = 0f,
                playerAlive = true,
            )
        }

        // npc2 should target npc3 (nearest), not the human (id=1)
        assertEquals(NPC_ID_BASE + 3, manager.getTargetId(NPC_ID_BASE + 2), "npc2 should target npc3 (nearest)")
    }

    /**
     * An NPC must never select itself as a target even when it is the only entity.
     * This test keeps the human player dead and no other NPCs so self would be the
     * only remaining candidate — verifying the self-exclusion filter is effective.
     */
    @Test
    fun npcIgnoresSelf() {
        val manager = NpcAiManager(worldW = 1000f, worldH = 1000f)
        val npc2 = aiShip(idOffset = 2, x = 500f, y = 500f)
        manager.register(npc2)

        // No human player alive, no other NPCs — only npc2 exists
        repeat(3) {
            manager.tickAll(
                npcs = listOf(npc2),
                playerX = 500f,
                playerY = 500f,
                playerVx = 0f,
                playerVy = 0f,
                playerAlive = false,
            )
        }

        // targetId must be -1 (no target), not npc2's own id
        assertEquals(-1, manager.getTargetId(NPC_ID_BASE + 2), "npc2 must not target itself; targetId should be -1")
    }

    /** `state.targetId` is updated to the selected target's id each tick; reverts to -1 when target dies. */
    @Test
    fun targetIdUpdatedEachTick() {
        val manager = NpcAiManager(worldW = 1000f, worldH = 1000f)
        val npc2 = aiShip(idOffset = 2, x = 500f, y = 500f)
        manager.register(npc2)

        // Player alive and close
        manager.tickAll(
            npcs = listOf(npc2),
            playerX = 520f,
            playerY = 500f,
            playerVx = 0f,
            playerVy = 0f,
            playerAlive = true,
        )
        assertEquals(1, manager.getTargetId(NPC_ID_BASE + 2), "targetId should be 1 (human player) when player is alive")

        // Player dies — no living targets remain
        manager.tickAll(
            npcs = listOf(npc2),
            playerX = 520f,
            playerY = 500f,
            playerVx = 0f,
            playerVy = 0f,
            playerAlive = false,
        )
        assertEquals(-1, manager.getTargetId(NPC_ID_BASE + 2), "targetId should be -1 when no living target exists")
        assertEquals(NpcBehavior.PATROL, manager.getBehavior(NPC_ID_BASE + 2), "behavior should revert to PATROL when no target exists")
    }

    /**
     * Shot cooldown must be respected — NPC should not fire on every tick.
     * Fires are expected only on tick 0 (first tick, cooldown not yet set) and
     * every SHOT_COOLDOWN_TICKS thereafter.
     */
    @Test
    fun weaponCooldownRespected() {
        val manager = NpcAiManager(worldW = 1000f, worldH = 1000f)
        // Place NPC and player within attack range
        val npc2 = aiShip(idOffset = 2, x = 100f, y = 100f)
        manager.register(npc2)
        // Manually put into ATTACK state immediately by ticking until behavior is ATTACK
        // (player is adjacent — dist < ATTACK_RANGE_PX)
        val totalTicks = NpcAiConst.SHOT_COOLDOWN_TICKS * 3
        val shotTicks = mutableListOf<Int>()
        for (tick in 0 until totalTicks) {
            val events =
                manager.tickAll(
                    npcs = listOf(npc2),
                    playerX = 120f, // 20px away — within attack range
                    playerY = 100f,
                    playerVx = 0f,
                    playerVy = 0f,
                    playerAlive = true,
                )
            if (events.filterIsInstance<NpcWeaponEvent.Shot>().any { it.npcId == NPC_ID_BASE + 2 }) {
                shotTicks += tick
            }
        }

        // Must have fired at least once
        assertTrue(shotTicks.isNotEmpty(), "NPC should fire at least once in $totalTicks ticks")
        // No two consecutive shots within fewer than SHOT_COOLDOWN_TICKS ticks
        for (i in 1 until shotTicks.size) {
            val gap = shotTicks[i] - shotTicks[i - 1]
            assertTrue(
                gap >= NpcAiConst.SHOT_COOLDOWN_TICKS,
                "Shots fired only $gap ticks apart — cooldown not respected (min ${NpcAiConst.SHOT_COOLDOWN_TICKS})",
            )
        }
    }

    /**
     * Toroidal wrap in target selection: an NPC near one edge should prefer a target
     * that is closer via world wrap than via the direct path.
     */
    @Test
    fun targetSelectionUsesToroidalDistance() {
        val worldSize = 1000f
        val manager = NpcAiManager(worldW = worldSize, worldH = worldSize)
        // npc2 at (10, 500) — near the left edge
        val npc2 = aiShip(idOffset = 2, x = 10f, y = 500f)
        // npc3 at (990, 500) — near the right edge; toroidal distance = 20px, direct = 980px
        val npc3 = aiShip(idOffset = 3, x = 990f, y = 500f)
        manager.register(npc2)
        manager.register(npc3)

        // Human player at world center — direct distance from npc2 = ~490px
        // npc3 via toroidal wrap from npc2 = 20px — much closer
        repeat(5) {
            manager.tickAll(
                npcs = listOf(npc2, npc3),
                playerX = 500f,
                playerY = 500f,
                playerVx = 0f,
                playerVy = 0f,
                playerAlive = false, // player dead — only npc3 is a target
            )
        }

        assertEquals(
            NPC_ID_BASE + 3,
            manager.getTargetId(NPC_ID_BASE + 2),
            "npc2 should select npc3 via toroidal shortest path (20px) not direct (980px)",
        )
    }

    /**
     * Evade oscillation fix: when HP stays below the threshold, the NPC must stay
     * in EVADE continuously (timer is reset) rather than flickering EVADE→PATROL→EVADE.
     */
    @Test
    fun evadeTimerResetsWhenHpStillLow() {
        val manager = NpcAiManager(worldW = 1000f, worldH = 1000f)
        val npc2 = aiShip(idOffset = 2, x = 500f, y = 500f, hp = NpcAiConst.EVADE_HP_THRESHOLD - 1f)
        manager.register(npc2)

        // Tick past two full evade cycles
        val totalTicks = NpcAiConst.EVADE_DURATION_TICKS * 2 + 5
        repeat(totalTicks) {
            manager.tickAll(
                npcs = listOf(npc2),
                playerX = 500f,
                playerY = 500f,
                playerVx = 0f,
                playerVy = 0f,
                playerAlive = true,
            )
        }

        // NPC should still be in EVADE (never switched to PATROL while HP is low)
        assertEquals(NpcBehavior.EVADE, manager.getBehavior(NPC_ID_BASE + 2), "NPC should stay in EVADE when HP remains below threshold")
        assertFalse(
            manager.getEvadeTimer(NPC_ID_BASE + 2) == 0,
            "Evade timer should not be zero — it should have been reset on expiry",
        )
    }
}
