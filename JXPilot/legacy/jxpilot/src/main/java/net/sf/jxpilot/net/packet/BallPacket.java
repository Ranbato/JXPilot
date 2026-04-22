package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;
import net.sf.jxpilot.game.BallHolder;

/**
 * Holds data from a Ball packet.
 * @author Vlad Firoiu
 */
public final class BallPacket extends BallHolder implements XPilotPacket {

	private byte pkt_type;
	
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		id = in.getShort();
	}
	
	@Override
	public String toString() {
		return "\nBall Packet\ntype = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nid = " + id;
	}
}
