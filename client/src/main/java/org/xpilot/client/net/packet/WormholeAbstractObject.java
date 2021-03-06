package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Wormhole packet.
 * @author Vlad Firoiu
 * @since xpilot version 4.5.0
 */
public final class WormholeAbstractObject extends XPilotAbstractObject {
	private short x, y;

	public short getX(){return x;}
	public short getY(){return y;}
	
	/**
	 * Reads the packet type, x and y.
	 * @param in The buffer to read from.
	 */
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Wormhole Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y;
	}
}
