package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data form a Thrust Time packet.
 * @author Vlad Firoiu
 */
public final class ThrustTimePacket extends XPilotPacketAdaptor {
	private short count, max;
	
	public short getCount(){return count;}
	public short getMax(){return max;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		count = in.getShort();
		max = in.getShort();
	}

	@Override
	public String toString() {
		return "Thrust Time Packet\npacket type = " + pkt_type +
				"\ncount = " + count +
				"\nmax = " + max;
	}
}
