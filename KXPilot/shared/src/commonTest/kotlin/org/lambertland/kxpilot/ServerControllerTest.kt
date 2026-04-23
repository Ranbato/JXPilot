package org.lambertland.kxpilot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ServerControllerTest {
    private val defaultConfig = ServerConfig(port = 15345, maxPlayers = 4, targetFps = 10)

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    fun `initial state is Stopped`() =
        runTest {
            val controller = ServerController(this)
            assertIs<ServerState.Stopped>(controller.state.value)
        }

    // -----------------------------------------------------------------------
    // start / stop transitions
    // -----------------------------------------------------------------------

    @Test
    fun `start transitions from Stopped to Running`() =
        runTest {
            val controller = ServerController(this)
            // R39: wrap start/assertions in try/finally so stop() always runs.
            try {
                controller.start(defaultConfig)
                runCurrent()
                assertIs<ServerState.Running>(controller.state.value)
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `stop from Running transitions to Stopped`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                controller.stop()
                runCurrent()
                assertIs<ServerState.Stopped>(controller.state.value)
            } finally {
                controller.stop() // idempotent — safe to call again
            }
        }

    @Test
    fun `stop from Stopped is a no-op`() =
        runTest {
            val controller = ServerController(this)
            controller.stop()
            assertIs<ServerState.Stopped>(controller.state.value)
        }

    @Test
    fun `start while Starting is ignored`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                controller.start(defaultConfig.copy(port = 9999))
                runCurrent()
                val running = controller.state.value
                assertIs<ServerState.Running>(running)
                assertEquals(15345, running.config.port)
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `start while Running is ignored`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                controller.start(defaultConfig.copy(port = 9999))
                runCurrent()
                val running = controller.state.value
                assertIs<ServerState.Running>(running)
                assertEquals(15345, running.config.port)
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `running state carries config`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                val running = assertIs<ServerState.Running>(controller.state.value)
                assertEquals(defaultConfig, running.config)
            } finally {
                controller.stop()
            }
        }

    // -----------------------------------------------------------------------
    // Player management
    // -----------------------------------------------------------------------

    @Test
    fun `kickPlayer on non-existent player is a no-op`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                controller.kickPlayer(playerId = 1)
                val running = assertIs<ServerState.Running>(controller.state.value)
                assertTrue(running.players.none { it.id == 1 })
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `mutePlayer on missing id is a no-op`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                controller.mutePlayer(99)
                assertIs<ServerState.Running>(controller.state.value)
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `sendMessageAll appends event`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                val before = (controller.state.value as ServerState.Running).events.size
                controller.sendMessageAll("Hello world")
                val after = (controller.state.value as ServerState.Running).events.size
                assertTrue(after > before)
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `sendMessageOne to nonexistent player does not append event`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                val before = (controller.state.value as ServerState.Running).events.size
                // No player with id=1 is connected — must be a silent no-op (R3 fix)
                controller.sendMessageOne(1, "Hi Alice")
                val after = (controller.state.value as ServerState.Running).events.size
                assertEquals(before, after, "sendMessageOne to nonexistent player must not append event")
            } finally {
                controller.stop()
            }
        }

    // -----------------------------------------------------------------------
    // changeMap
    // -----------------------------------------------------------------------

    @Test
    fun `changeMap restarts server with new map path`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                controller.changeMap("/maps/foo.xp")
                runCurrent()
                val running = assertIs<ServerState.Running>(controller.state.value)
                assertEquals("/maps/foo.xp", running.config.mapPath)
            } finally {
                controller.stop()
            }
        }

    @Test
    fun `changeMap while stopped is a no-op`() =
        runTest {
            val controller = ServerController(this)
            controller.changeMap("/maps/foo.xp")
            assertIs<ServerState.Stopped>(controller.state.value)
        }

    // -----------------------------------------------------------------------
    // ServerConfig validation
    // -----------------------------------------------------------------------

    @Test
    fun `ServerConfig rejects port 0`() {
        var threw = false
        try {
            ServerConfig(port = 0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "port=0 should throw")
    }

    @Test
    fun `ServerConfig rejects maxPlayers 0`() {
        var threw = false
        try {
            ServerConfig(maxPlayers = 0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "maxPlayers=0 should throw")
    }

    @Test
    fun `ServerConfig accepts boundary values`() {
        val config = ServerConfig(port = 65535, maxPlayers = 256, targetFps = 200)
        assertEquals(65535, config.port)
        assertEquals(256, config.maxPlayers)
        assertEquals(200, config.targetFps)
    }

    // -----------------------------------------------------------------------
    // MAX_EVENTS ring buffer
    // -----------------------------------------------------------------------

    @Test
    fun `event log is capped at MAX_EVENTS`() =
        runTest {
            val controller = ServerController(this)
            try {
                controller.start(defaultConfig)
                runCurrent()
                repeat(ServerController.MAX_EVENTS + 50) { i ->
                    controller.sendMessageAll("msg $i")
                }
                val running = assertIs<ServerState.Running>(controller.state.value)
                assertTrue(
                    running.events.size <= ServerController.MAX_EVENTS,
                    "events.size=${running.events.size} should be ≤ ${ServerController.MAX_EVENTS}",
                )
            } finally {
                controller.stop()
            }
        }
}
