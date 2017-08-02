package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Mine packet.
 * @author Vlad Firoiu
 */
public final class MinePacket  implements XPilotPacket {
	private byte pkt_type;
	/**
	 * Assumed that no player has this id.
	 */
	public static final int EXPIRED_MINE_ID = 4096;

	protected short x,y,id;
	protected byte team_mine;

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
