package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.RadarHolder;

/**
 * Holds data from a Radar packet.
 * @author Vlad Firiou
 */
public final class RadarPacket extends RadarHolder implements XPilotPacket {
	private byte pkt_type;
	public byte getPacketType(){return pkt_type;}
	
	/**
	 * Reads the packet type, x, y, and size.
	 * @param in The buffer to read from.
	 */
	public void readPacket(ByteBuffer in) {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		size= Byte.toUnsignedInt(in.get());
		
		//x = (int)((double)(x * 256) / Setup->width + 0.5);
		//y = (int)((double)(y * RadarHeight) / Setup->height + 0.5);
	}
	
	@Override
	public String toString() {
		return "Radar Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nsize = " + size;
	}
}
