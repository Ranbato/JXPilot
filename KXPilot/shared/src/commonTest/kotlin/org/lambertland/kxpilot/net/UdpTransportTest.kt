package org.lambertland.kxpilot.net

import kotlin.test.Test
import kotlin.test.assertFailsWith

// ---------------------------------------------------------------------------
// Tests for UdpTransport (P2)
// ---------------------------------------------------------------------------

class UdpTransportTest {
    /**
     * Verifies that binding on port 0 (OS-assigned ephemeral port) succeeds
     * on desktop/JVM without throwing.  Port 0 is used to avoid conflicts with
     * other processes.
     */
    @Test
    fun `UdpTransport bind on port 0 does not throw on desktop`() {
        val transport = UdpTransport(0)
        try {
            transport.bind()
        } finally {
            transport.close()
        }
    }

    /**
     * Verifies that close() is idempotent — calling it twice does not throw.
     */
    @Test
    fun `UdpTransport close is idempotent`() {
        val transport = UdpTransport(0)
        transport.bind()
        transport.close()
        transport.close() // second close must not throw
    }
}
