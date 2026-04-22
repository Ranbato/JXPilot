package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// UdpTransport — platform-agnostic UDP socket abstraction
// ---------------------------------------------------------------------------
//
// The `expect` declaration lives here in commonMain.  Each platform provides
// an `actual` implementation:
//
//   desktopMain — java.net.DatagramSocket via Dispatchers.IO
//   androidMain — stub (throws UnsupportedOperationException)
//   jsMain      — stub (throws UnsupportedOperationException)
//
// Design notes:
// - [bind] is a synchronous call that opens the socket.  It must be called
//   before [receive] or [send].  If the port is already in use it throws
//   [java.net.BindException] (or an equivalent platform error).
// - [receive] suspends until a datagram arrives.  It should be called from
//   a coroutine running on Dispatchers.IO (platform implementations may
//   enforce this).
// - [send] is fire-and-forget; errors are silently swallowed (caller logs).
// - [close] releases the underlying socket.  Safe to call multiple times.

/**
 * A single UDP datagram as received by [UdpTransport].
 *
 * @param data     Raw payload bytes.
 * @param srcAddr  Source IP address string (e.g. "192.168.1.5").
 * @param srcPort  Source UDP port.
 */
data class UdpDatagram(
    val data: ByteArray,
    val srcAddr: String,
    val srcPort: Int,
) {
    override fun equals(other: Any?) =
        other is UdpDatagram &&
            srcAddr == other.srcAddr &&
            srcPort == other.srcPort &&
            data.contentEquals(other.data)

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + srcAddr.hashCode()
        result = 31 * result + srcPort
        return result
    }
}

/**
 * Platform-specific UDP socket.
 *
 * Obtain an instance, call [bind], then use [receive]/[send] inside a
 * coroutine.  Always [close] when done (use `try/finally` or `use` via
 * [AutoCloseable]).
 *
 * @param port UDP port to bind on (0 = OS-assigned ephemeral port).
 */
expect class UdpTransport(
    port: Int,
) : UdpChannel {
    /** The port this socket is (or will be) bound to. */
    override val port: Int

    override fun bind()

    override suspend fun receive(): UdpDatagram

    override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    )

    /** Close the underlying socket and release all resources. */
    override fun close()
}
