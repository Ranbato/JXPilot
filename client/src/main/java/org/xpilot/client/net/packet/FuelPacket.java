package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.FuelHolder;

/**
 * Holds data from a Fuel packet.
 * @author Vlad Firoiu
 */
public final class FuelPacket extends FuelHolder implements XPilotPacket {
	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		num = in.getUnsignedShort();
		fuel = in.getUnsignedShort();
	}

	@Override
	public String toString() {
		return "Fuel Packet\npacket type = " + pkt_type +
				"\nnum = " + num +
				"\nfuel = " + fuel;
	}
}
