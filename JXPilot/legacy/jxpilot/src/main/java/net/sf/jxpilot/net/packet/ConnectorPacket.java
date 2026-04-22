package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.ConnectorHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Connector packet.
 * @author Vlad Firoiu
 */
public final class ConnectorPacket extends ConnectorHolder implements XPilotPacket {

	private byte pkt_type;
	
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		super.x0 = in.getShort();
		super.y0 = in.getShort();
		super.x1 = in.getShort();
		super.y1 = in.getShort();
		super.tractor = in.getByte();
	}
	@Override
	public String toString() {
		return "Connector Packet\npacket type = " + pkt_type +
			"\nx0 = " + x0 +
			"\ny0 = " + y0 +
			"\nx1 = " + x1 +
			"\ny1 = " + y1 +
			"\ntractor = " + tractor;
	}
}
