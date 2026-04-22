package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Destruct packet.
 * @author Vlad Firoiu
 */
public final class DestructPacket extends XPilotPacketAdaptor {

	private short count;
	
	public short getCount(){return count;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		count = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Destruct Packet\npacket type = " + pkt_type +
				"\ncount = " + count;
	}
}
