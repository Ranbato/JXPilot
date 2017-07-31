package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data form a Round Delay packet.
 * @author Vlad Firoiu
 */
public final class RoundDelayAbstractObject extends XPilotAbstractObject {
	private short count, max;
	
	public short getCount(){return count;}
	public short getMax(){return max;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		count = in.getShort();
		max = in.getShort();
	}

	@Override
	public String toString() {
		return "Round Delay Packet\npacket type = " + pkt_type +
				"\ncount = " + count +
				"\nmax = " + max;
	}
}
