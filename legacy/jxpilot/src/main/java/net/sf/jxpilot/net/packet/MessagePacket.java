package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Message packet.
 * @author Vlad Firoiu
 */
public final class MessagePacket extends XPilotPacketAdaptor {
	private final ReliableReadException MESSAGE_READ_EXCEPTION = new ReliableReadException("Message");
	
	private String message;
	public String getMessage() {return message;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		//int pos = in.position();
		pkt_type = in.getByte();
		message = in.getString();
		if(message == null) throw MESSAGE_READ_EXCEPTION;
	}
	
	@Override
	public String toString() {
		return "Message Packet\npacket type = " + pkt_type +
				"\nmessage = " + message;
	}
}
