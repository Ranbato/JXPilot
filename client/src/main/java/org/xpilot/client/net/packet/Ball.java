package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Ball packet.
 * @author Vlad Firoiu
 */
public final class Ball extends XPilotAbstractObject {

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		id = in.getShort();
	}
	
	@Override
	public String toString() {
		return "\nBall Packet\ntype = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nid = " + id;
	}
}
