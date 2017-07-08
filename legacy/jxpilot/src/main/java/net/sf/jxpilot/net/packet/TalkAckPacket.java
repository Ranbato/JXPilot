package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Talk Ack packet.
 * @author Vlad Firoiu
 */
public final class TalkAckPacket extends XPilotPacketAdaptor {
	
	/**
	 * The number of bytes in a Talk Ack packet.
	 */
	public static final int LENGTH = 1+4;//5
	
	private final ReliableReadException TALK_ACK_READ_EXCEPTION = new ReliableReadException("Talk Ack");
	
	private int talk_ack;
	public int getTalkAck() {return talk_ack;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.length()<LENGTH) throw TALK_ACK_READ_EXCEPTION;
		pkt_type = in.getByte();
		talk_ack = in.getInt();
	}

	@Override
	public String toString() {
		return "Talk Ack Packet\ntype = " + pkt_type +
				"\ntalk ack = " + talk_ack;
	}
}
