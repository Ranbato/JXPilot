package org.lambertland.kxpilot.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

actual class UdpTransport actual constructor(
    actual override val port: Int,
) : UdpChannel {
    // R29: @Volatile ensures that concurrent close/send/receive threads see the
    // latest socket reference without a data race on the JMM.
    @Volatile private var socket: DatagramSocket? = null

    // R30: pre-allocate the receive buffer once per instance to avoid per-call
    // ByteArray allocation on the hot receive path.
    private val recvBuf = ByteArray(65_535)
    private val recvPacket = DatagramPacket(recvBuf, recvBuf.size)

    // R31: Cache resolved InetAddress per hostname so that repeated send() calls
    // to the same server do not block the caller's thread with DNS lookups.
    // ConcurrentHashMap is used for thread-safety without locking on the hot path.
    private val addrCache = ConcurrentHashMap<String, InetAddress>()

    actual override fun bind() {
        socket = DatagramSocket(port)
    }

    actual override suspend fun receive(): UdpDatagram =
        withContext(Dispatchers.IO) {
            val sock = socket ?: error("UdpTransport not bound")
            recvPacket.setLength(recvBuf.size)
            sock.receive(recvPacket)
            UdpDatagram(
                data = recvPacket.data.copyOf(recvPacket.length),
                srcAddr = recvPacket.address.hostAddress ?: recvPacket.address.toString(),
                srcPort = recvPacket.port,
            )
        }

    actual override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ) {
        try {
            val sock = socket ?: return
            // R31: resolve DNS only on the first send to this address; subsequent
            // sends reuse the cached InetAddress, avoiding a blocking lookup on the
            // game-loop thread.  If the cache miss itself blocks, it is acceptable
            // once per connection lifetime.
            val address = addrCache.getOrPut(addr) { InetAddress.getByName(addr) }
            val packet = DatagramPacket(data, data.size, address, toPort)
            sock.send(packet)
        } catch (_: Exception) {
        }
    }

    actual override fun close() {
        socket?.close()
        socket = null
        addrCache.clear()
    }
}
