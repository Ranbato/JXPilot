package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a shutdown packet.
 * @author Vlad Firoiu
 */
public class ShutdownAbstractObject extends XPilotAbstractObject {

	protected short count, delay;
	
	public short getCount(){return count;}
	public short getDelay(){return delay;}
	
	@Override
	public void readPacket(ByteBuffer in) {
		pkt_type = in.get();
		count = in.getShort();
		delay = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Shutdown Packet\npacket type = " + pkt_type +
				"\ncount = " + count +
				"\ndelay = " + delay;
	}
}
