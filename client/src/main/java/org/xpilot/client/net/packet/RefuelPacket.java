package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.RefuelHolder;

/**
 * Holds data from a Refuel packet.
 * @author Vlad Firoiu
 */
public final class RefuelPacket extends RefuelHolder implements XPilotPacket {
	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x0 = in.getShort();
		y0 = in.getShort();
		x1 = in.getShort();
		y1 = in.getShort();
	}

	@Override
	public String toString() {
		return "Refuel Packet\npacket type = " + pkt_type +
			"\nx0 = " + x0 +
			"\ny0 = " + y0 +
			"\nx1 = " + x1 +
			"\ny1 = " + y1;
	}
}
