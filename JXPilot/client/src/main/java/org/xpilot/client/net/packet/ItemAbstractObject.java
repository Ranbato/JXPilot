package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from an Item packet.
 * @author Vlad Firoiu
 */
public class ItemAbstractObject extends XPilotAbstractObject {
	private short x, y;
	private byte type;

	public short getX(){return x;}
	public short getY(){return y;}
	public byte getType(){return type;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		type = in.get();
	}
	
	@Override
	public String toString() {
		return "Item Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\ntype = " + type;
	}
}
