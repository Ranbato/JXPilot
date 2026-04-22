package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.net.ClientSession
import org.lambertland.kxpilot.net.ConnState
import org.lambertland.kxpilot.net.PktType
import org.lambertland.kxpilot.net.UdpChannel
import org.lambertland.kxpilot.net.UdpDatagram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// FrameBroadcastTest
// ---------------------------------------------------------------------------

private class CapturingChannel(
    override val port: Int = 9000,
) : UdpChannel {
    val sent = mutableListOf<Triple<ByteArray, String, Int>>()

    override fun bind() {}

    override suspend fun receive(): UdpDatagram = kotlinx.coroutines.suspendCancellableCoroutine {}

    override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ) {
        sent.add(Triple(data, addr, toPort))
    }

    override fun close() {}
}

class FrameBroadcastTest {
    private fun makeWorld(): ServerGameWorld = ServerGameWorld(ServerConfig(serverName = "test", port = 9999, targetFps = 10))

    /** Build a ClientSession whose state is [state] and whose loginPort is [loginPort]. */
    private fun makeSession(
        id: Int,
        loginPort: Int,
        state: ConnState,
    ): ClientSession {
        val s = ClientSession(id = id, addr = "127.0.0.1", loginPort = loginPort, magic = 0)
        s.state = state
        return s
    }

    @Test
    fun onlyPlayingSessionsReceiveFramePackets() {
        val world = makeWorld()
        val transport1 = CapturingChannel(port = 1001)
        val transport2 = CapturingChannel(port = 1002)

        val sessions =
            mapOf(
                1001 to makeSession(id = 1, loginPort = 1001, state = ConnState.PLAYING),
                1002 to makeSession(id = 2, loginPort = 1002, state = ConnState.LISTENING),
            )
        val transports = mapOf(1001 to transport1 as UdpChannel, 1002 to transport2 as UdpChannel)
        val players =
            listOf(
                ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001"),
                ConnectedPlayer(id = 2, name = "Bob", address = "127.0.0.1:4002"),
            )

        FrameBroadcast.sendFrame(world, sessions, transports, players)

        assertTrue(transport1.sent.isNotEmpty(), "PLAYING session should receive packets")
        assertTrue(transport2.sent.isEmpty(), "LISTENING session should receive no packets")
    }

    @Test
    fun framePacketSequenceIsStartScoreEnd() {
        val world = makeWorld()
        val transport = CapturingChannel()

        val sessions =
            mapOf(
                1001 to makeSession(id = 1, loginPort = 1001, state = ConnState.PLAYING),
            )
        val transports = mapOf(1001 to transport as UdpChannel)
        val players = listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001"))

        FrameBroadcast.sendFrame(world, sessions, transports, players)

        val types = transport.sent.map { (data, _, _) -> data[0].toInt() and 0xFF }
        // Expect PKT_START … PKT_END at minimum (no players so no SCORE packets)
        assertTrue(types.contains(PktType.START), "Should send PKT_START")
        assertTrue(types.contains(PktType.END), "Should send PKT_END")
        assertTrue(types.indexOf(PktType.START) < types.indexOf(PktType.END), "START should come before END")
    }

    @Test
    fun scorePacketSentForEachPlayer() {
        val world = makeWorld()
        // Spawn two players
        world.spawnPlayer(1, "Alice", "alice", 0)
        world.spawnPlayer(2, "Bob", "bob", 0)

        val transport = CapturingChannel()
        val sessions =
            mapOf(
                1001 to makeSession(id = 1, loginPort = 1001, state = ConnState.PLAYING),
            )
        val transports = mapOf(1001 to transport as UdpChannel)
        val players = listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001"))

        FrameBroadcast.sendFrame(world, sessions, transports, players)

        val scoreCount = transport.sent.count { (data, _, _) -> (data[0].toInt() and 0xFF) == PktType.SCORE }
        assertEquals(2, scoreCount, "Should send one PKT_SCORE per player in world")
    }

    @Test
    fun addressLookupFailsGracefullyForMissingPlayer() {
        val world = makeWorld()
        val transport = CapturingChannel()

        // Session is PLAYING but not in connectedPlayers list
        val sessions =
            mapOf(
                1001 to makeSession(id = 99, loginPort = 1001, state = ConnState.PLAYING),
            )
        val transports = mapOf(1001 to transport as UdpChannel)
        val players = emptyList<ConnectedPlayer>()

        // Should not throw
        FrameBroadcast.sendFrame(world, sessions, transports, players)

        assertTrue(transport.sent.isEmpty(), "No packets should be sent when address lookup fails")
    }
}
