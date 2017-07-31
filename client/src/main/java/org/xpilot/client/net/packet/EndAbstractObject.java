package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from an End packet.
 * @author Vlad Firoiu
 */
public final class EndAbstractObject extends XPilotAbstractObject {

	private int loops;
	public int getLoops(){return loops;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		loops = in.getInt();
	}

	@Override
	public String toString() {
		return "End Packet\npacket type = " + pkt_type +
				"\nloops = " + loops;
	}
}
