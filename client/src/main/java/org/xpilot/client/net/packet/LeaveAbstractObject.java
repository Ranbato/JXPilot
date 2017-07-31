package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Leave packet.
 * @author Vlad Firoiu
 */
public final class LeaveAbstractObject extends XPilotAbstractObject {
	/**
	 * The number of bytes in a Leave packet.
	 */
	public static final int LENGTH = 1 + 2;//3
	
	private final ReliableReadException LEAVE_READ_EXCEPTION = new ReliableReadException("Leave");
	private short id;
	public short getId(){return id;}
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.remaining()<LENGTH) throw LEAVE_READ_EXCEPTION;
		pkt_type = in.get();
		id = in.getShort();
	}
	@Override
	public String toString() {
		return "Leave Packet\npacket type = " + pkt_type +
					"\nid = " + id;
	}
}
