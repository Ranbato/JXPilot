package org.lambertland.kxpilot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lambertland.kxpilot.net.UdpChannel
import org.lambertland.kxpilot.net.UdpDatagram
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// FakeUdpTransport — never blocks, used to isolate ServerController in tests
// ---------------------------------------------------------------------------

/**
 * A no-op [UdpChannel] for tests.
 *
 * [receive] suspends forever so the contact loop never progresses;
 * [send] records datagrams for assertion.
 */
private class FakeUdpTransport(
    override val port: Int = 0,
) : UdpChannel {
    val sent = mutableListOf<Triple<ByteArray, String, Int>>()
    private var closed = false

    override fun bind() { /* no-op */ }

    override suspend fun receive(): UdpDatagram {
        // Suspend forever (until the coroutine is cancelled)
        return kotlinx.coroutines.suspendCancellableCoroutine { }
    }

    override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ) {
        if (!closed) sent.add(Triple(data, addr, toPort))
    }

    override fun close() {
        closed = true
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
private fun fakeController(scope: kotlinx.coroutines.CoroutineScope): ServerController {
    var portCounter = 10000
    return ServerController(scope) { FakeUdpTransport(portCounter++) }
}

private val testConfig = ServerConfig(port = 15345, maxPlayers = 4, targetFps = 10)

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class ServerGameLoopTest {
    // -----------------------------------------------------------------------
    // Basic lifecycle with fake transport
    // -----------------------------------------------------------------------

    @Test
    fun `start with fake transport transitions to Running`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            assertIs<ServerState.Running>(controller.state.value)
            controller.stop()
        }

    @Test
    fun `stop transitions back to Stopped`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            controller.stop()
            assertIs<ServerState.Stopped>(controller.state.value)
        }

    @Test
    fun `running state has correct config`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            val running = assertIs<ServerState.Running>(controller.state.value)
            assertEquals(testConfig, running.config)
            controller.stop()
        }

    // -----------------------------------------------------------------------
    // Metrics tick
    // -----------------------------------------------------------------------

    @Test
    fun `metrics tickRateTarget is set from config`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            val running = assertIs<ServerState.Running>(controller.state.value)
            assertEquals(testConfig.targetFps, running.metrics.tickRateTarget)
            controller.stop()
        }

    @Test
    fun `metrics uptimeMs advances after 1 second of virtual time`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()

            advanceTimeBy(1500L)
            runCurrent()

            val running = assertIs<ServerState.Running>(controller.state.value)
            assertTrue(
                running.metrics.uptimeMs >= 1000L,
                "uptimeMs=${running.metrics.uptimeMs} should be ≥ 1000",
            )
            controller.stop()
        }

    @Test
    fun `tickRateActual is populated after 1 second`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()

            advanceTimeBy(1500L)
            runCurrent()

            val running = assertIs<ServerState.Running>(controller.state.value)
            assertTrue(
                running.metrics.tickRateActual > 0.0,
                "tickRateActual=${running.metrics.tickRateActual} should be > 0",
            )
            controller.stop()
        }

    // -----------------------------------------------------------------------
    // Player management (no real UDP)
    // -----------------------------------------------------------------------

    @Test
    fun `kickPlayer on missing player is no-op`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            controller.kickPlayer(999)
            val running = assertIs<ServerState.Running>(controller.state.value)
            assertTrue(running.players.isEmpty())
            controller.stop()
        }

    @Test
    fun `sendMessageAll appends broadcast event`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            val before = (controller.state.value as ServerState.Running).events.size
            controller.sendMessageAll("hello everyone")
            val after = (controller.state.value as ServerState.Running).events.size
            assertTrue(after > before, "event count should increase")
            val events = (controller.state.value as ServerState.Running).events
            assertTrue(events.any { it.message.contains("hello everyone") })
            controller.stop()
        }

    @Test
    fun `event log stays within MAX_EVENTS cap`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            repeat(ServerController.MAX_EVENTS + 50) { i ->
                controller.sendMessageAll("msg $i")
            }
            val running = assertIs<ServerState.Running>(controller.state.value)
            assertTrue(
                running.events.size <= ServerController.MAX_EVENTS,
                "events.size=${running.events.size} should be ≤ ${ServerController.MAX_EVENTS}",
            )
            controller.stop()
        }

    @Test
    fun `changeMap restarts server with new path`() =
        runTest {
            val controller = fakeController(this)
            controller.start(testConfig)
            runCurrent()
            controller.changeMap("/tmp/new.xp")
            runCurrent()
            val running = assertIs<ServerState.Running>(controller.state.value)
            assertEquals("/tmp/new.xp", running.config.mapPath)
            controller.stop()
        }
}
