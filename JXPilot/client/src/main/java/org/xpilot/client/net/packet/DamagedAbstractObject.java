package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Damaged packet.
 * @author Vlad Firoiu
 */
public final class DamagedAbstractObject extends XPilotAbstractObject {

	private byte damaged;
	
	public byte getDamaged(){return damaged;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		damaged = in.get();
	}

	@Override
	public String toString() {
		return "Damaged Packet\npacket type = " + pkt_type +
				"\ndamaged = " + damaged;
	}
}
