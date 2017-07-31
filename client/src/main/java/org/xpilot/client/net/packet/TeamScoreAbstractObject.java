package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Team Score packet.
 * @author Vlad Firoiu
 */
public final class TeamScoreAbstractObject extends XPilotAbstractObject {
	private short team;
	private double score;
	
	public short getTeam() {return team;}
	public double getScore() {return score;}
	
	/**
	 * Reads the type, team, and score.
	 * @param in The buffer from which to read the data.
	 */
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		team = in.getShort();
		score = (double)in.getInt()/100.0;
	}

	@Override
	public String toString() {
		return "Team Score Packet\npacket type = " + pkt_type +
				"\nteam = " + team +
				"\nscore = " + score;
	}
}
