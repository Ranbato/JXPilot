package org.lambertland.kxpilot.net

actual class UdpTransport actual constructor(
    actual override val port: Int,
) : UdpChannel {
    actual override fun bind(): Unit = throw UnsupportedOperationException("Embedded UDP server is not supported in the browser")

    actual override suspend fun receive(): UdpDatagram =
        throw UnsupportedOperationException("Embedded UDP server is not supported in the browser")

    actual override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ): Unit = throw UnsupportedOperationException("Embedded UDP server is not supported in the browser")

    actual override fun close() { /* no-op */ }
}
