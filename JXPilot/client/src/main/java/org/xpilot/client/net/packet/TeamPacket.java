package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Team Score packet.
 * @author Vlad Firoiu
 */
public final class TeamPacket extends XPilotAbstractObject {
	private byte team;

	public short getTeam() {return team;}

	/**
	 * Reads the type, team, and score.
	 * @param in The buffer from which to read the data.
	 */
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		id = Short.toUnsignedInt(in.getShort());
		team = in.get();

	}

	@Override
	public String toString() {
		return "Team Score Packet\npacket type = " + pkt_type +
				"\nid = " + id +
				"\nteam = " + team;
	}
}
