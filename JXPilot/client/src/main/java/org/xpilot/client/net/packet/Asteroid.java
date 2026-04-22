package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;


/**
 * Holds data from an Asteroid packet.
 * @author Vlad Firoiu
 * @since xpilot version 4.4.0
 */
public final class Asteroid extends XPilotAbstractObject {

	private byte type;
	private byte size;
	private byte rot;

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		
		byte type_size = in.get();
		type = (byte) (type_size >> 4);
		size = (byte) (type_size & 0x0F);
		
		rot = in.get();
	}
	
	@Override
	public String toString() {
		return "Asteroid Packet\npacket type = " + type +
				"\nx = " + x +
				"\ny = " + y +
				"\ntype = " + type +
				"\nsize = " + size +
				"\nrot = " + rot;
	}
}
