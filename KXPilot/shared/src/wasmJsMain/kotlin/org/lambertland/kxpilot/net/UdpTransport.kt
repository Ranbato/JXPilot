package org.lambertland.kxpilot.net

actual class UdpTransport actual constructor(
    actual override val port: Int,
) : UdpChannel {
    actual override fun bind(): Unit =
        throw UnsupportedOperationException(
            "Raw UDP is not available in the browser. Use a WebSocket proxy to reach the XPilot server.",
        )

    actual override suspend fun receive(): UdpDatagram =
        throw UnsupportedOperationException(
            "Raw UDP is not available in the browser. Use a WebSocket proxy to reach the XPilot server.",
        )

    actual override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ): Unit =
        throw UnsupportedOperationException(
            "Raw UDP is not available in the browser. Use a WebSocket proxy to reach the XPilot server.",
        )

    actual override fun close() { /* no-op */ }
}
