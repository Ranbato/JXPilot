package org.lambertland.kxpilot.net

// ---------------------------------------------------------------------------
// UdpChannel — testable interface for UDP send/receive
// ---------------------------------------------------------------------------
//
// UdpTransport (the platform actual class) implements this interface.
// Tests can supply a fake implementation without needing to subclass
// the expect/actual UdpTransport.

/**
 * Minimal interface for a bound UDP socket.
 *
 * [UdpTransport] implements this.  Tests inject a fake implementation.
 */
interface UdpChannel : AutoCloseable {
    /** The local UDP port this channel is bound to. */
    val port: Int

    /** Bind the underlying socket. Must be called before [receive] or [send]. */
    fun bind()

    /** Suspend until a datagram arrives. */
    suspend fun receive(): UdpDatagram

    /** Send [data] to [addr]:[toPort]. Fire-and-forget. */
    fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    )

    /** Close the socket and release resources. */
    override fun close()
}
