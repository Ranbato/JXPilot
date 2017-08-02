package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Missile packet.
 * @author Vlad Firoiu
 */
public class MissilePacket implements XPilotPacket {
	private byte pkt_type;
	protected short x,y;
	protected short len, dir;
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		len = (short)Byte.toUnsignedInt(in.get());
		dir = (short)Byte.toUnsignedInt(in.get());
	}

	@Override
	public String toString() {
		return "Missile Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nlen = " + len +
				"\ndir = " + dir;
	}
}
