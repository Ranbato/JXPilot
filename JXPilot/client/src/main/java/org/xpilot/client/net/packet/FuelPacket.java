package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Fuel packet.
 * @author Vlad Firoiu
 */
public final class FuelPacket implements XPilotPacket {
	private byte pkt_type;
	protected int num, fuel;

	public int getNum(){return num;}
	public int getFuel(){return fuel;}
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		num = Short.toUnsignedInt(in.getShort());
		fuel = Short.toUnsignedInt(in.getShort());
	}

	@Override
	public String toString() {
		return "Fuel Packet\npacket type = " + pkt_type +
				"\nnum = " + num +
				"\nfuel = " + fuel;
	}
}
