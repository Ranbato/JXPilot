package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Lose Item packet.
 * @author Vlad Firoiu
 */
public final class LoseItemPacket extends XPilotPacketAdaptor {
	/**
	 * Unsigned byte.
	 */
	private short lose_item;
	public short getLoseItem(){return lose_item;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		lose_item = in.getUnsignedByte();
	}
	
	@Override
	public String toString() {
		return "Lose Item Packet\npacket type = " + pkt_type +
				"\nlose item = " + lose_item;
	}
}
