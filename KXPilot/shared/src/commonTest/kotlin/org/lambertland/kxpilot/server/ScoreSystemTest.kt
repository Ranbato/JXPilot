package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// ScoreSystemTest
// ---------------------------------------------------------------------------

class ScoreSystemTest {
    private fun makePlayer(): Player {
        val pl = Player()
        pl.score = 0.0
        pl.kills = 0
        pl.deaths = 0
        pl.updateScore = false
        return pl
    }

    @Test
    fun wallDeathDeductsVictimAndCreditsAttacker() {
        val victim = makePlayer()
        val attacker = makePlayer()

        ScoreSystem.wallDeath(victim, attacker)

        assertEquals(1, victim.deaths)
        assertEquals(-1.0, victim.score)
        assertTrue(victim.updateScore)

        assertEquals(1, attacker.kills)
        assertEquals(1.0, attacker.score)
        assertTrue(attacker.updateScore)
    }

    @Test
    fun wallDeathNoAttackerOnlyVictimPenalty() {
        val victim = makePlayer()

        ScoreSystem.wallDeath(victim, null)

        assertEquals(1, victim.deaths)
        assertEquals(-1.0, victim.score)
        assertEquals(0, victim.kills)
    }

    @Test
    fun playerKillDeductsVictimAndCreditsKiller() {
        val victim = makePlayer()
        val killer = makePlayer()

        ScoreSystem.playerKill(victim, killer)

        assertEquals(1, victim.deaths)
        assertEquals(-1.0, victim.score)

        assertEquals(1, killer.kills)
        assertEquals(1.0, killer.score)
    }

    @Test
    fun playerKillSelfOnlyIncrementsDeaths() {
        val pl = makePlayer()

        ScoreSystem.playerKill(pl, pl)

        assertEquals(1, pl.deaths)
        assertEquals(-1.0, pl.score)
        assertEquals(0, pl.kills) // no self-kill credit
    }

    @Test
    fun environmentKillDeductsVictim() {
        val victim = makePlayer()

        ScoreSystem.environmentKill(victim)

        assertEquals(1, victim.deaths)
        assertEquals(-1.0, victim.score)
        assertTrue(victim.updateScore)
    }

    @Test
    fun respawnPreservesScoreAndResetsPhysics() {
        val config = ServerConfig(serverName = "test", port = 9999, targetFps = 10)
        val world = ServerGameWorld(config)
        val pl = world.spawnPlayer(0, "tester", "user", 0)

        pl.score = 5.0
        pl.kills = 3
        pl.deaths = 2
        pl.plState = PlayerState.KILLED

        val spawnPos = ClPos(100, 100)
        world.respawn(pl, spawnPos, 0.0)

        assertEquals(PlayerState.ALIVE, pl.plState)
        assertEquals(5.0, pl.score, "Score should be preserved across respawn")
        assertEquals(3, pl.kills, "Kills should be preserved across respawn")
        assertEquals(2, pl.deaths, "Deaths should be preserved across respawn")
        assertEquals(spawnPos, pl.pos)
    }
}
