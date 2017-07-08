package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.ECMHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from an ECM packet.
 * @author Vlad Firoiu
 */
public final class ECMPacket extends ECMHolder implements XPilotPacket {
	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		size = in.getShort();
	}
	
	@Override
	public String toString() {
		return "ECM Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nsize = " + size;
	}

}
