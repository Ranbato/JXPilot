package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Quit packet.
 * @author Vlad Firoiu
 */
public final class QuitAbstractObject extends XPilotAbstractObject {
	private final ReliableReadException QUIT_READ_EXCEPTION = new ReliableReadException("Quit");
	
	private String reason;
	public String getReason(){return reason;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		pkt_type = in.get();
		if((reason = in.asCharBuffer().toString()) == null) throw QUIT_READ_EXCEPTION;
	}
	
	@Override
	public String toString() {
		return "Quit Packet\npacket type = " + pkt_type +
				"reason = " + reason;
	}

}
