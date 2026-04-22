package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Wreckage packet.
 * @author Vlad Firoiu
 */
public final class WreckageAbstractObject extends XPilotAbstractObject {
	private short x, y;
	private byte wreck_type, size, rot;

	public short getX(){return x;}
	public short getY(){return y;}
	public byte getWreckType(){return wreck_type;}
	public byte getSize(){return size;}
	public byte getRot(){return rot;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		wreck_type = in.get();
		size = in.get();
		rot = in.get();
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
