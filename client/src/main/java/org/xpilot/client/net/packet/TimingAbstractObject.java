package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Timing packet.
 * @author Vlad Firoiu
 */
public final class TimingAbstractObject extends XPilotAbstractObject {
	/**
	 * The number of bytes in a Timing packet.
	 */
	public static final int LENGTH = 1+2+2;//5
	
	private final ReliableReadException TIMING_READ_EXCEPTION = new ReliableReadException("Timing");
	
	private short id;
	/**
	 * Unsigned short.
	 */
	private short timing;
	
	public short getId(){return id;}
	public short getTiming(){return timing;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.remaining()<LENGTH) throw TIMING_READ_EXCEPTION;
		pkt_type = in.get();
		id = in.getShort();
		timing = in.getShort();
	}

	@Override
	public String toString() {
		return "Timing Packet\npacket type = " + pkt_type +
				"\nid = " + id +
				"\ntiming = " + timing;
	}
}
