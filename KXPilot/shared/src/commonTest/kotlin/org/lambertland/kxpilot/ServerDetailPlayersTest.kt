package org.lambertland.kxpilot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.lambertland.kxpilot.model.ServerBrowserState
import org.lambertland.kxpilot.model.ServerInfo
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.ui.screens.MainMenuStateHolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** N2/S2 — ServerInfo.players propagates through selectServer to Detail state. */
class ServerDetailPlayersTest {
    private fun makeHolder(): MainMenuStateHolder {
        val sc = ServerController(CoroutineScope(Dispatchers.Unconfined))
        return MainMenuStateHolder(CoroutineScope(Dispatchers.Unconfined), sc)
    }

    @Test
    fun `selectServer with populated players list results in Detail with non-empty players`() {
        val holder = makeHolder()
        val server =
            ServerInfo(
                host = "test.example.com",
                port = 15345,
                mapName = "dogfight",
                playerCount = 2,
                queueCount = 0,
                maxPlayers = 16,
                fps = 30,
                version = "4.7.3",
                pingMs = 42,
                status = "running",
                players = listOf("Alice", "Bob"),
            )
        holder.selectServer(server)
        val detail = assertIs<ServerBrowserState.Detail>(holder.browserState)
        assertEquals(2, detail.server.players.size)
        assertTrue(detail.server.players.contains("Alice"))
        assertTrue(detail.server.players.contains("Bob"))
    }

    @Test
    fun `selectServer with empty players list results in Detail with empty players`() {
        val holder = makeHolder()
        val server =
            ServerInfo(
                host = "empty.example.com",
                port = 15345,
                mapName = "map",
                playerCount = 0,
                queueCount = 0,
                maxPlayers = 8,
                fps = 25,
                version = "4.7.3",
                pingMs = null,
                status = "idle",
            )
        holder.selectServer(server)
        val detail = assertIs<ServerBrowserState.Detail>(holder.browserState)
        assertTrue(detail.server.players.isEmpty())
    }
}
