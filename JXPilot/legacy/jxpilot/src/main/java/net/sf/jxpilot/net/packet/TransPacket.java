package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Trans packet.
 * @author Vlad Firoiu
 */
public final class TransPacket extends XPilotPacketAdaptor {
	
	private short x1, y1, x2, y2;
	
	public short getX1(){return x1;}
	public short getY1(){return y1;}
	public short getX2(){return x2;}
	public short getY2(){return y2;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x1 = in.getShort();
		y1 = in.getShort();
		x2 = in.getShort();
		y2 = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Trans Packet\npacket type = " + pkt_type +
				"\nx1 = " + x1 +
				"\ny1 = " + y2 +
				"\nx2 = " + x2 +
				"\ny2 = " + y2;
	}
}
