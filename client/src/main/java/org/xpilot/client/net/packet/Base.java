package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a base packet.
 * @author Vlad Firoiu
 */
public final class Base extends XPilotAbstractObject {
	/**
	 * Length of base packet (7).
	 */
	public static final int LENGTH = 1 + 2 + 4;//7

	private short num;
	
	private final ReliableReadException BASE_READ_EXCEPTION = new ReliableReadException("Base");

	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.remaining()<LENGTH) throw BASE_READ_EXCEPTION;
		pkt_type = in.get();
		id = in.getShort();
		num = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Base Packet\npacket type = " + pkt_type +
				"\nid = " + id +
				"\nnum = " + num;
	}
}
