package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from an Item packet.
 * @author Vlad Firoiu
 */
public class ItemPacket extends XPilotPacketAdaptor {
	private short x, y;
	private byte type;

	public short getX(){return x;}
	public short getY(){return y;}
	public byte getType(){return type;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		type = in.getByte();
	}
	
	@Override
	public String toString() {
		return "Item Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\ntype = " + type;
	}
}
