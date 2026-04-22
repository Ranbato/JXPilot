package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Time Left packet.
 * @author Vlad Firoiu
 */
public final class TimeLeftAbstractObject extends XPilotAbstractObject {
	protected int seconds;
	
	public int getSeconds(){return seconds;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		seconds = in.getInt();
	}
	
	@Override
	public String toString() {
		return "Time Left Packet\npacket type = " + pkt_type +
				"\nseconds = " + seconds;
	}
}
