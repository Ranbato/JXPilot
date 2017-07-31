package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Destruct packet.
 * @author Vlad Firoiu
 */
public final class DestructAbstractObject extends XPilotAbstractObject {

	private short count;
	
	public short getCount(){return count;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		count = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Destruct Packet\npacket type = " + pkt_type +
				"\ncount = " + count;
	}
}
