package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.LaserHolder;

/**
 * Holds data from a Laser packet.
 * @author Vlad Firoiu
 */
public final class LaserPacket extends LaserHolder implements XPilotPacket {

	private byte pkt_type;
	
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		color = in.get();
		x = in.getShort();
		y = in.getShort();
		len = in.getShort();
		dir = in.get();
	}

	@Override
	public String toString() {
		return "Laser Packet\npacket type = " + pkt_type +
				"\ncolor = " + color +
				"\nx = " + x +
				"\ny = " + y +
				"\nlen = " + len +
				"\ndir = " + dir;
	}
}
