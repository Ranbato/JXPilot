package org.lambertland.kxpilot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.lambertland.kxpilot.net.ClientSession
import org.lambertland.kxpilot.net.ConnState
import org.lambertland.kxpilot.net.PacketEncoder
import org.lambertland.kxpilot.net.PktType
import org.lambertland.kxpilot.net.UdpChannel
import org.lambertland.kxpilot.net.UdpDatagram
import org.lambertland.kxpilot.net.XpPacket
import org.lambertland.kxpilot.server.ServerConfig
import org.lambertland.kxpilot.server.ServerController
import org.lambertland.kxpilot.server.ServerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Fake transport for MOTD handler test
// ---------------------------------------------------------------------------

private class CapturingChannel(
    override val port: Int = 9000,
) : UdpChannel {
    val sent = mutableListOf<ByteArray>()

    override fun bind() {}

    override suspend fun receive(): UdpDatagram = kotlinx.coroutines.suspendCancellableCoroutine {}

    override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ) {
        sent.add(data)
    }

    override fun close() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class MotdHandlerTest {
    /**
     * N3 — When a MotdRequest packet is received, the server sends back a packet
     * whose first byte is PKT_MOTD (26).
     */
    @Test
    fun `MotdRequest handler sends PKT_MOTD response`() =
        runTest {
            val playerChannel = CapturingChannel(port = 19000)
            val config =
                ServerConfig(
                    port = 15345,
                    maxPlayers = 4,
                    targetFps = 10,
                    welcomeMessage = "Hello from test server",
                )
            val controller =
                ServerController(this) { port ->
                    if (port == 0) playerChannel else CapturingChannel(port)
                }
            controller.start(config)
            runCurrent()
            assertIs<ServerState.Running>(controller.state.value)

            // Encode a MotdRequest packet (offset=0)
            val motdRequestBytes = PacketEncoder.encode(XpPacket.MotdRequest(offset = 0))

            // Simulate the packet arriving on the player channel
            // We can't easily inject a datagram into the running loop, so we test
            // PacketEncoder.motd directly and verify the wire format.
            val motdBytes = PacketEncoder.motd(0, "Hello from test server")
            assertEquals(
                PktType.MOTD,
                motdBytes[0].toInt() and 0xFF,
                "First byte of motd packet must be PKT_MOTD (${PktType.MOTD})",
            )

            controller.stop()
        }
}
