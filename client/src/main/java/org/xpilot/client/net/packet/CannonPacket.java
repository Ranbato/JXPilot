package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Cannon packet.
 * @author Vlad Firoiu
 */
public final class CannonPacket extends XPilotAbstractObject {
	int num;
	long dead_time;
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		num = Short.toUnsignedInt(in.getShort());
		dead_time = Short.toUnsignedInt(in.getShort());
	}

	@Override
	public String toString() {
		return "Cannon Packet\npacket type = " + pkt_type + 
				"\nnum = " + num +
				"\ndead time = " + dead_time;
	}
}
