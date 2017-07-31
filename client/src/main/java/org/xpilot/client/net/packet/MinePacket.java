package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.MineHolder;

/**
 * Holds data from a Mine packet.
 * @author Vlad Firoiu
 */
public final class MinePacket extends MineHolder implements XPilotPacket {
	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		x = in.getShort();
		y = in.getShort();
		team_mine = in.get();
		id = in.getShort();
	}

	@Override
	public String toString() {
		return "Mine Packet\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nteam mine = " + team_mine +
				"\nid = " + id;
	}
	
}
