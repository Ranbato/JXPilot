package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Start packet.
 * @author Vlad Firoiu
 */
public final class StartAbstractObject extends XPilotAbstractObject {
	/**
	 * Length of start packet (9).
	 */
	public static final int LENGTH = 1 + 4 + 4;

	private final ReliableReadException START_READ_EXCEPTION = new ReliableReadException();
	
	private int loops, key_ack;
	
	public int getLoops() {return loops;}
	public int getKeyAck(){return key_ack;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.length() < LENGTH) throw START_READ_EXCEPTION;
		pkt_type = in.get();
		loops = in.getInt();
		key_ack = in.getInt();
	}
	
	@Override
	public String toString() {
		return "Start Packet\npacket type = " + pkt_type +
				"\nloops = " + loops +
				"\nkey ack = " + key_ack;
	}
}
