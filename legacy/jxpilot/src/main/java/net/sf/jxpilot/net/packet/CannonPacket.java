package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.CannonHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Cannon packet.
 * @author Vlad Firoiu
 */
public final class CannonPacket extends CannonHolder implements XPilotPacket {
	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		num = in.getUnsignedShort();
		dead_time = in.getUnsignedShort();
	}

	@Override
	public String toString() {
		return "Cannon Packet\npacket type = " + pkt_type + 
				"\nnum = " + num +
				"\ndead time = " + dead_time;
	}
}
