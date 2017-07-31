package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Message packet.
 * @author Vlad Firoiu
 */
public final class MessageAbstractObject extends XPilotAbstractObject {
	private final ReliableReadException MESSAGE_READ_EXCEPTION = new ReliableReadException("Message");
	
	private String message;
	public String getMessage() {return message;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		//int pos = in.position();
		pkt_type = in.get();
		message = in.getString();
		if(message == null) throw MESSAGE_READ_EXCEPTION;
	}
	
	@Override
	public String toString() {
		return "Message Packet\npacket type = " + pkt_type +
				"\nmessage = " + message;
	}
}
