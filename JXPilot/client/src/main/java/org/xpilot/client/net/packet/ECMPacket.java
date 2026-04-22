package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from an ECM packet.
 * @author Vlad Firoiu
 */
public final class ECMPacket implements XPilotPacket {
	private byte pkt_type;
	protected short x, y, size;

	public short getX(){return x;}
	public short getY(){return y;}
	public short getSize(){return size;}
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		size = in.getShort();
	}
	
	@Override
	public String toString() {
		return "ECM Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nsize = " + size;
	}

}
