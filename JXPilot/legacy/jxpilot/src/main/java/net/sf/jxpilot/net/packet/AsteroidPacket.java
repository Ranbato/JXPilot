package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;
import net.sf.jxpilot.game.AsteroidHolder;

/**
 * Holds data from an Asteroid packet.
 * @author Vlad Firoiu
 * @since xpilot version 4.4.0
 */
public final class AsteroidPacket extends AsteroidHolder implements XPilotPacket {

	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		
		type_size = in.getByte();
		type = (byte) (type_size >> 4);
		size = (byte) (type_size & 0x0F);
		
		rot = in.getByte();
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
