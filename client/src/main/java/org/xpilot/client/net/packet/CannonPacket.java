package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.CannonHolder;

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
		num = in.getUnsignedShort();
		dead_time = in.getUnsignedShort();
	}

	@Override
	public String toString() {
		return "Cannon Packet\npacket type = " + pkt_type + 
				"\nnum = " + num +
				"\ndead time = " + dead_time;
	}
}
