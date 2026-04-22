package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Paused packet.
 * @author Vlad Firoiu
 */
public final class PausedPacket extends XPilotPacketAdaptor {
	private short x, y, count;
	
	public short getX(){return x;}
	public short getY(){return y;}
	public short getCount(){return count;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		count = in.getShort();
	}
	@Override
	public String toString() {
		return "Paused Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\ncount = " + count;
	}
}
