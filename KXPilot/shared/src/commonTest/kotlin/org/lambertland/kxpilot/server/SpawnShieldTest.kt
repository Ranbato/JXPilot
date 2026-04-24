package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpawnShieldTest {
    private fun makeWorld(): ServerGameWorld {
        val w = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))
        // The default open-field world has no map bases.  Add one so that
        // respawn tests can use a valid spawn position (not ClPos(0,0)).
        w.world.bases.add(Base(pos = ClPos(100 * 64, 100 * 64), dir = 0, ind = 0, team = 0, order = 0))
        return w
    }

    @Test
    fun playerHasShieldAfterSpawn() {
        val world = makeWorld()
        val player = world.spawnPlayer(sessionId = 1, nick = "test", userName = "test", team = 0)
        assertTrue(
            (player.have and PlayerAbility.SHIELD) != 0L,
            "Player should have SHIELD in 'have' after spawn",
        )
        assertTrue(
            (player.used and PlayerAbility.SHIELD) != 0L,
            "Player should have SHIELD active in 'used' after spawn",
        )
    }

    @Test
    fun playerHasShieldAfterRespawn() {
        val world = makeWorld()
        val player = world.spawnPlayer(sessionId = 1, nick = "test", userName = "test", team = 0)
        val base = world.world.findSpawnBase(0)
        assertNotNull(base, "Test world should have at least one spawn base (added in makeWorld)")
        world.respawn(player, base.pos, 0.0)
        assertTrue(
            (player.have and PlayerAbility.SHIELD) != 0L,
            "Player should have SHIELD in 'have' after respawn",
        )
        assertTrue(
            (player.used and PlayerAbility.SHIELD) != 0L,
            "Player should have SHIELD active in 'used' after respawn",
        )
    }

    @Test
    fun playerHasDefaultAbilitiesAfterSpawn() {
        val world = makeWorld()
        val player = world.spawnPlayer(sessionId = 1, nick = "test", userName = "test", team = 0)
        // Must have all DEF_HAVE bits
        val defHave =
            PlayerAbility.SHIELD or PlayerAbility.COMPASS or PlayerAbility.REFUEL or
                PlayerAbility.REPAIR or PlayerAbility.CONNECTOR or PlayerAbility.SHOT or PlayerAbility.LASER
        assertTrue(
            (player.have and defHave) == defHave,
            "Player should have all DEF_HAVE abilities after spawn",
        )
    }

    /**
     * Regression: shield must be re-granted after a respawn even if the player
     * had manually deactivated it before death.
     *
     * Previously `initPlayerPhysics` would OR into existing `have`/`used` flags,
     * meaning a cleared shield bit could survive across lives.
     */
    @Test
    fun shieldIsRegrantedAfterRespawnWhenPreviouslyDisabled() {
        val world = makeWorld()
        val player = world.spawnPlayer(sessionId = 1, nick = "test", userName = "test", team = 0)

        // Simulate player manually disabling shield (clears both have and used)
        player.have = player.have and PlayerAbility.SHIELD.inv()
        player.used = player.used and PlayerAbility.SHIELD.inv()
        assertTrue(
            (player.have and PlayerAbility.SHIELD) == 0L,
            "Shield should be cleared before respawn",
        )

        val base = world.world.findSpawnBase(0)
        assertNotNull(base, "Test world should have at least one spawn base (added in makeWorld)")
        world.respawn(player, base.pos, 0.0)

        assertTrue(
            (player.have and PlayerAbility.SHIELD) != 0L,
            "Shield must be re-granted in 'have' after respawn even if previously cleared",
        )
        assertTrue(
            (player.used and PlayerAbility.SHIELD) != 0L,
            "Shield must be active in 'used' after respawn even if previously cleared",
        )
    }
}
