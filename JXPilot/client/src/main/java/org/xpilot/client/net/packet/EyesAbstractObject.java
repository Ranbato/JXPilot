package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from an Eyes packet.
 * @author Vlad Firoiu
 */
public class EyesAbstractObject extends XPilotAbstractObject {
	private short id;
	public short getId(){return id;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		id = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Eyes Packet\npacket type = " + pkt_type +
				"\nid = " + id;
	}
}
