package org.lambertland.kxpilot.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

actual class UdpTransport actual constructor(
    actual override val port: Int,
) : UdpChannel {
    private var socket: DatagramSocket? = null

    actual override fun bind() {
        socket = DatagramSocket(port)
    }

    actual override suspend fun receive(): UdpDatagram =
        withContext(Dispatchers.IO) {
            val sock = socket ?: error("UdpTransport not bound")
            val buf = ByteArray(65_535)
            val packet = DatagramPacket(buf, buf.size)
            sock.receive(packet)
            UdpDatagram(
                data = packet.data.copyOf(packet.length),
                srcAddr = packet.address.hostAddress ?: packet.address.toString(),
                srcPort = packet.port,
            )
        }

    actual override fun send(
        data: ByteArray,
        addr: String,
        toPort: Int,
    ) {
        try {
            val sock = socket ?: return
            val address = InetAddress.getByName(addr)
            val packet = DatagramPacket(data, data.size, address, toPort)
            sock.send(packet)
        } catch (_: Exception) {
        }
    }

    actual override fun close() {
        socket?.close()
        socket = null
    }
}
