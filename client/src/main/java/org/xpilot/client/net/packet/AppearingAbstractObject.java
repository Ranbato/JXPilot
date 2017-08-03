package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Paused packet.
 * @author Vlad Firoiu
 */
public final class AppearingAbstractObject extends XPilotAbstractObject {
	private short x, y, count;
	
	public short getX(){return x;}
	public short getY(){return y;}
	public short getCount(){return count;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		count = in.getShort();
	}
	@Override
	public String toString() {
		return "Appearing Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\ncount = " + count;
	}
}
