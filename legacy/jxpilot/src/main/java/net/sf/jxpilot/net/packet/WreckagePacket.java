package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Wreckage packet.
 * @author Vlad Firoiu
 */
public final class WreckagePacket extends XPilotPacketAdaptor {
	private short x, y;
	private byte wreck_type, size, rot;

	public short getX(){return x;}
	public short getY(){return y;}
	public byte getWreckType(){return wreck_type;}
	public byte getSize(){return size;}
	public byte getRot(){return rot;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		wreck_type = in.getByte();
		size = in.getByte();
		rot = in.getByte();
	}
	
	@Override
	public String toString() {
		return "Wreckage Packet\npacket type = " + pkt_type +
			"\nx = " + x +
			"\ny = " + y +
			"\nwreck type = " + wreck_type +
			"\nsize = " + size +
			"\nrot = " + rot;
	}
}
