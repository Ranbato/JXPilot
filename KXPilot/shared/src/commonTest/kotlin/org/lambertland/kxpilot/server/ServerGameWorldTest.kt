package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClickConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// ---------------------------------------------------------------------------
// ServerGameWorldTest
// ---------------------------------------------------------------------------

class ServerGameWorldTest {
    private fun makeWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

    @Test
    fun spawnPlayerAtWorldCentreWhenNoBasesExist() {
        val world = makeWorld()
        // Default open-field world has no bases
        assertEquals(0, world.world.bases.size, "Default world should have no bases")

        val pl = world.spawnPlayer(1, "tester", "user", 0)

        val expectedCx = world.world.cwidth / 2
        val expectedCy = world.world.cheight / 2
        assertEquals(expectedCx, pl.pos.cx, "Spawn X should be world centre")
        assertEquals(expectedCy, pl.pos.cy, "Spawn Y should be world centre")
    }

    @Test
    fun spawnPlayerAtBasePosWhenBaseExists() {
        val world = makeWorld()
        val basePos =
            org.lambertland.kxpilot.common.ClPos(
                5 * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2,
                5 * ClickConst.BLOCK_CLICKS + ClickConst.BLOCK_CLICKS / 2,
            )
        world.world.bases.add(Base(pos = basePos, dir = 0, ind = 0, team = 0, order = 0))

        val pl = world.spawnPlayer(2, "basePlayer", "user", 0)

        assertEquals(basePos.cx, pl.pos.cx, "Spawn X should match base position")
        assertEquals(basePos.cy, pl.pos.cy, "Spawn Y should match base position")
    }

    @Test
    fun despawnRemovesPlayer() {
        val world = makeWorld()
        world.spawnPlayer(1, "tester", "user", 0)
        assertNotNull(world.playerForSession(1))

        world.despawnPlayer(1)

        assertEquals(null, world.playerForSession(1))
    }

    @Test
    fun advanceFrameIncrementsCounter() {
        val world = makeWorld()
        assertEquals(0L, world.frameLoop)
        world.advanceFrame()
        assertEquals(1L, world.frameLoop)
        world.advanceFrame()
        assertEquals(2L, world.frameLoop)
    }

    @Test
    fun respawnPreservesIdentityAndResetsPosition() {
        val world = makeWorld()
        val pl = world.spawnPlayer(1, "tester", "user", 0)
        pl.score = 7.0
        pl.kills = 2
        pl.deaths = 1
        pl.plState = PlayerState.KILLED

        val newPos =
            org.lambertland.kxpilot.common
                .ClPos(10, 20)
        world.respawn(pl, newPos, 0.0)

        assertEquals(PlayerState.ALIVE, pl.plState)
        assertEquals(7.0, pl.score)
        assertEquals(2, pl.kills)
        assertEquals(1, pl.deaths)
        assertEquals(newPos, pl.pos)
    }
}
