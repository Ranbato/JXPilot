package org.lambertland.kxpilot.server

import org.lambertland.kxpilot.common.ClPos
import org.lambertland.kxpilot.common.ClickConst
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
        clientVersion: Int = 0,
        viewWidth: Int = 800,
        viewHeight: Int = 600,
    ): ClientSession {
        val s = ClientSession(id = id, addr = "127.0.0.1", loginPort = loginPort, magic = 0)
        s.state = state
        s.clientVersion = clientVersion
        s.viewWidth = viewWidth
        s.viewHeight = viewHeight
        return s
    }

    /** Read a big-endian int16 from [bytes] at [offset]. */
    private fun readInt16BE(
        bytes: ByteArray,
        offset: Int,
    ): Int {
        val hi = bytes[offset].toInt() and 0xFF
        val lo = bytes[offset + 1].toInt() and 0xFF
        val raw = (hi shl 8) or lo
        // sign-extend 16-bit
        return if (raw >= 0x8000) raw - 0x10000 else raw
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

    // -----------------------------------------------------------------------
    // PKT_BALL tests
    // -----------------------------------------------------------------------

    /**
     * Balls within the player's viewport are sent; wire layout is verified.
     *
     * Ball at click (128, 256) → pixel (2, 4).
     * Player at world centre (world uses default 100×100 block grid).
     * Session view 800×600 px — the ball is well within the viewport.
     *
     * Wire format (long form, version >= 0x4F14):
     *   byte[0]  = PKT_BALL (17)
     *   bytes[1-2] = posX big-endian int16
     *   bytes[3-4] = posY big-endian int16
     *   bytes[5-6] = id  big-endian int16
     *   byte[7]  = style
     */
    @Test
    fun ballPacketWireLayoutVerified() {
        val world = makeWorld()

        // Spawn a player so we have a physical position for viewport computation.
        val pl = world.spawnPlayer(1, "Alice", "alice", 0)
        // Place player at world centre
        val worldCx = world.world.cwidth / 2
        val worldCy = world.world.cheight / 2
        pl.pos = ClPos(worldCx, worldCy)

        // Place a ball near the player (same centre — definitely in view).
        val ball1 = world.pools.balls.allocate()!!
        ball1.id = 7 // carrier player id in C semantics
        ball1.pos = ClPos(worldCx + 128, worldCy + 256) // +2px, +4px from centre
        ball1.ballStyle = 3

        val transport = CapturingChannel()
        val session =
            makeSession(
                id = 1,
                loginPort = 1001,
                state = ConnState.PLAYING,
                clientVersion = 0x4F14, // F_BALLSTYLE enabled
                viewWidth = 800,
                viewHeight = 600,
            )
        val sessions = mapOf(1001 to session)
        val transports = mapOf(1001 to transport as UdpChannel)
        val players = listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001"))

        FrameBroadcast.sendFrame(world, sessions, transports, players)

        val ballPackets = transport.sent.filter { (data, _, _) -> (data[0].toInt() and 0xFF) == PktType.BALL }
        assertEquals(1, ballPackets.size, "Exactly one in-view ball should produce one PKT_BALL")

        val pkt = ballPackets[0].first
        // Long form: 8 bytes
        assertEquals(8, pkt.size, "PKT_BALL long form should be 8 bytes")
        // posX = (worldCx + 128) >> 6
        val expectedPosX = (worldCx + 128) shr ClickConst.CLICK_SHIFT
        val expectedPosY = (worldCy + 256) shr ClickConst.CLICK_SHIFT
        assertEquals(expectedPosX, readInt16BE(pkt, 1), "posX bytes should match CLICK_TO_PIXEL")
        assertEquals(expectedPosY, readInt16BE(pkt, 3), "posY bytes should match CLICK_TO_PIXEL")
        assertEquals(7, readInt16BE(pkt, 5), "id bytes should match ball carrier player id")
        assertEquals(3, pkt[7].toInt() and 0xFF, "style byte should match ballStyle")
    }

    @Test
    fun ballPacketShortFormForOldClient() {
        val world = makeWorld()
        val pl = world.spawnPlayer(1, "Alice", "alice", 0)
        val worldCx = world.world.cwidth / 2
        val worldCy = world.world.cheight / 2
        pl.pos = ClPos(worldCx, worldCy)

        val ball = world.pools.balls.allocate()!!
        ball.id = 3
        ball.pos = ClPos(worldCx, worldCy)
        ball.ballStyle = 5

        val transport = CapturingChannel()
        // clientVersion < 0x4F14 → no F_BALLSTYLE → short form (7 bytes, no style)
        val session =
            makeSession(
                id = 1,
                loginPort = 1001,
                state = ConnState.PLAYING,
                clientVersion = 0x4F13,
                viewWidth = 800,
                viewHeight = 600,
            )
        FrameBroadcast.sendFrame(
            world,
            mapOf(1001 to session),
            mapOf(1001 to transport as UdpChannel),
            listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001")),
        )

        val ballPkts = transport.sent.filter { (d, _, _) -> d[0].toInt() and 0xFF == PktType.BALL }
        assertEquals(1, ballPkts.size)
        assertEquals(7, ballPkts[0].first.size, "Old client should receive 7-byte short form (no style byte)")
    }

    @Test
    fun ballPacketComesBeforePktEnd() {
        val world = makeWorld()
        val pl = world.spawnPlayer(1, "Alice", "alice", 0)
        val worldCx = world.world.cwidth / 2
        val worldCy = world.world.cheight / 2
        pl.pos = ClPos(worldCx, worldCy)

        val ball = world.pools.balls.allocate()!!
        ball.pos = ClPos(worldCx, worldCy)

        val transport = CapturingChannel()
        val session = makeSession(id = 1, loginPort = 1001, state = ConnState.PLAYING, clientVersion = 0x4F14)
        FrameBroadcast.sendFrame(
            world,
            mapOf(1001 to session),
            mapOf(1001 to transport as UdpChannel),
            listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001")),
        )

        val types = transport.sent.map { (d, _, _) -> d[0].toInt() and 0xFF }
        val ballIdx = types.indexOf(PktType.BALL)
        val endIdx = types.indexOf(PktType.END)
        assertTrue(ballIdx >= 0, "PKT_BALL should be sent")
        assertTrue(ballIdx < endIdx, "PKT_BALL must come before PKT_END")
    }

    @Test
    fun noBallsProducesNoBallPackets() {
        val world = makeWorld()
        // No balls allocated
        val transport = CapturingChannel()
        val session = makeSession(id = 1, loginPort = 1001, state = ConnState.PLAYING, clientVersion = 0x4F14)
        FrameBroadcast.sendFrame(
            world,
            mapOf(1001 to session),
            mapOf(1001 to transport as UdpChannel),
            listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001")),
        )

        val ballCount = transport.sent.count { (d, _, _) -> d[0].toInt() and 0xFF == PktType.BALL }
        assertEquals(0, ballCount, "No PKT_BALL should be sent when there are no live balls")
        // But START and END should still be sent
        val types = transport.sent.map { (d, _, _) -> d[0].toInt() and 0xFF }
        assertTrue(types.contains(PktType.START))
        assertTrue(types.contains(PktType.END))
    }

    // -----------------------------------------------------------------------
    // Viewport culling tests
    // -----------------------------------------------------------------------

    @Test
    fun ballOutsideViewportIsNotSent() {
        val world = makeWorld()
        val pl = world.spawnPlayer(1, "Alice", "alice", 0)
        // Place player at world centre
        val worldCx = world.world.cwidth / 2
        val worldCy = world.world.cheight / 2
        pl.pos = ClPos(worldCx, worldCy)

        val ball = world.pools.balls.allocate()!!
        // Place ball 10000 pixels away — well outside any 800×600 viewport
        ball.pos = ClPos(worldCx + 10000 * ClickConst.CLICK, worldCy)
        ball.ballStyle = 1

        val transport = CapturingChannel()
        val session =
            makeSession(
                id = 1,
                loginPort = 1001,
                state = ConnState.PLAYING,
                clientVersion = 0x4F14,
                viewWidth = 800,
                viewHeight = 600,
            )
        FrameBroadcast.sendFrame(
            world,
            mapOf(1001 to session),
            mapOf(1001 to transport as UdpChannel),
            listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001")),
        )

        val ballCount = transport.sent.count { (d, _, _) -> d[0].toInt() and 0xFF == PktType.BALL }
        assertEquals(0, ballCount, "Ball far outside viewport should not be sent")
    }

    @Test
    fun onlyInViewBallsSentWhenMultipleBallsExist() {
        val world = makeWorld()
        val pl = world.spawnPlayer(1, "Alice", "alice", 0)
        val worldCx = world.world.cwidth / 2
        val worldCy = world.world.cheight / 2
        pl.pos = ClPos(worldCx, worldCy)

        // Ball in view — 10 pixels from player
        val ballIn = world.pools.balls.allocate()!!
        ballIn.pos = ClPos(worldCx + 10 * ClickConst.CLICK, worldCy)
        ballIn.ballStyle = 2

        // Ball out of view — 5000 pixels away
        val ballOut = world.pools.balls.allocate()!!
        ballOut.pos = ClPos(worldCx + 5000 * ClickConst.CLICK, worldCy)
        ballOut.ballStyle = 9

        val transport = CapturingChannel()
        val session =
            makeSession(
                id = 1,
                loginPort = 1001,
                state = ConnState.PLAYING,
                clientVersion = 0x4F14,
                viewWidth = 800,
                viewHeight = 600,
            )
        FrameBroadcast.sendFrame(
            world,
            mapOf(1001 to session),
            mapOf(1001 to transport as UdpChannel),
            listOf(ConnectedPlayer(id = 1, name = "Alice", address = "127.0.0.1:4001")),
        )

        val ballPkts = transport.sent.filter { (d, _, _) -> d[0].toInt() and 0xFF == PktType.BALL }
        assertEquals(1, ballPkts.size, "Only the in-view ball should be sent")
        // Verify it's the in-view ball by checking style byte
        assertEquals(2, ballPkts[0].first[7].toInt() and 0xFF, "Only the in-view ball's style should appear")
    }
}
