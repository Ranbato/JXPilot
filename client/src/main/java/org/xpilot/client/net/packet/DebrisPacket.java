package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Debris packet.
 * @author Vlad Firoiu
 */
public final class DebrisPacket implements XPilotPacket {
	/**
	 * Unsigned byte.
	 */
	private short pkt_type, num;
	
	@Override
	public byte getPacketType() {return (byte)pkt_type;}
	public short getType(){return pkt_type;}
	public short getNum(){return num;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = (short)Byte.toUnsignedInt(in.get());
		num = (short)Byte.toUnsignedInt(in.get());
	}
	
	@Override
	public String toString() {
		return "Debris Packet\npacket type = " + pkt_type +
				"\nnum = " + num;
	}
}
