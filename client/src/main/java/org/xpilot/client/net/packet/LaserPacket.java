package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Laser packet.
 * @author Vlad Firoiu
 */
public final class LaserPacket implements XPilotPacket {

	private byte pkt_type;
	protected byte color;
	protected short x, y, len;
	protected byte dir;

	public byte getColor(){return color;}
	public short getX(){return x;}
	public short getY(){return y;}
	public short getLen(){return len;}
	public byte getDir(){return dir;}
	
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
