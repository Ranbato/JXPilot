package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;
import net.sf.jxpilot.net.StringReadException;

/**
 * Holds data from a Quit packet.
 * @author Vlad Firoiu
 */
public final class QuitPacket extends XPilotPacketAdaptor {
	private final ReliableReadException QUIT_READ_EXCEPTION = new ReliableReadException("Quit");
	
	private String reason;
	public String getReason(){return reason;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		pkt_type = in.getByte();
		if((reason = in.getString()) == null) throw QUIT_READ_EXCEPTION;
	}
	
	@Override
	public String toString() {
		return "Quit Packet\npacket type = " + pkt_type +
				"reason = " + reason;
	}

}
