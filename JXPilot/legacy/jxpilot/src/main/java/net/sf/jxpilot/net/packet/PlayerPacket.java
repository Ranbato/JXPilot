package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.ShipShape;
import net.sf.jxpilot.game.PlayerHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Represents data from a Player packet.
 * @author Vlad Firoiu
 */
public class PlayerPacket extends PlayerHolder implements XPilotPacket {
	
	public static final int LENGTH = 1 + 2 + 1 + 1;
	
	/**
	 * Default packet read exception to throw.
	 */
	protected final ReliableReadException PLAYER_READ_EXCEPTION = new ReliableReadException("Player");
	
	protected byte pkt_type;
	
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.length() < LENGTH) throw PLAYER_READ_EXCEPTION;
		
		pkt_type = in.getByte();
		id = in.getShort();
		my_team = in.getByte();
		my_char = in.getByte();
		
		if((name = in.getString()) == null) throw PLAYER_READ_EXCEPTION;
		if((real = in.getString()) == null) throw PLAYER_READ_EXCEPTION;
		if((host = in.getString()) == null) throw PLAYER_READ_EXCEPTION;
		
		String s1 = in.getString();
		if(s1 == null) throw PLAYER_READ_EXCEPTION;
		String s2 = in.getString();
		if(s2 == null) throw PLAYER_READ_EXCEPTION;
		ship_shape = ShipShape.parseShip(s1, s2);
	}
	
	@Override
	public String toString() {
		return "Player Packet\npacket type = " + pkt_type +
				"\nid = " + id +
				"\nmy team = " + my_team +
				"\nmy char = " + my_char +
				"\nname = " + name +
				"\nreal = " + real +
				"\nhost = " + host +
				"\nship shape = " + ship_shape;
	}
}
