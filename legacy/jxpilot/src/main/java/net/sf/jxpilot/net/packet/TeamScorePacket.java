package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Team Score packet.
 * @author Vlad Firoiu
 */
public final class TeamScorePacket extends XPilotPacketAdaptor {
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
		pkt_type = in.getByte();
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
