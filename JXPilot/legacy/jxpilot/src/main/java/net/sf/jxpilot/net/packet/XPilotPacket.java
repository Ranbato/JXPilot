package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data for an XPilot packet.
 * @author Vlad Firoiu
 */
public interface XPilotPacket {
	/**
	 * XPilot packets are always preceded by a 1-byte code representing
	 * the type of packet.
	 * @return The unique byte representing this packet type.
	 */
	public byte getPacketType();
	
	/**
	 * Reads an XPilot packet from the input buffer.
	 * @param in The input buffer.
	 * @throws PacketReadException If a problem occurs while reading.
	 */
	public void readPacket(ByteBuffer in) throws PacketReadException;
}
