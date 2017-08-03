package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Refuel packet.
 * @author Vlad Firoiu
 */
public final class RefuelPacket implements XPilotPacket {
	private byte pkt_type;
	protected short x0, y0, x1, y1;

	public short getX0(){return x0;}
	public short getY0(){return y0;}
	public short getX1(){return x1;}
	public short getY1(){return y1;}
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
