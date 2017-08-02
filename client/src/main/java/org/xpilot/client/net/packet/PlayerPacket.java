package org.xpilot.client.net.packet;

import org.xpilot.common.ShipShape;

import java.nio.ByteBuffer;


/**
 * Represents data from a Player packet.
 * @author Vlad Firoiu
 */
public class PlayerPacket implements XPilotPacket {
	
	public static final int LENGTH = 1 + 2 + 1 + 1;
	
	/**
	 * Default packet read exception to throw.
	 */
	protected final ReliableReadException PLAYER_READ_EXCEPTION = new ReliableReadException("Player");
	
	protected byte pkt_type;
	protected short id;
	protected byte my_team, my_char;
	protected String name, real, host;
	protected ShipShape ship_shape;

	public short getId(){return id;}
	public byte getTeam(){return my_team;}
	public byte getChar(){return my_char;}
	public String getName(){return name;}
	public String getReal(){return real;}
	public String getHost(){return host;}
	public ShipShape getShipShape(){return ship_shape;}
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.remaining() < LENGTH) throw PLAYER_READ_EXCEPTION;
		
		pkt_type = in.get();
		id = in.getShort();
		my_team = in.get();
		my_char = in.get();
		
		if((name = in.asCharBuffer().toString()) == null) throw PLAYER_READ_EXCEPTION;
		if((real = in.asCharBuffer().toString()) == null) throw PLAYER_READ_EXCEPTION;
		if((host = in.asCharBuffer().toString()) == null) throw PLAYER_READ_EXCEPTION;
		
		String s1 = in.asCharBuffer().toString();
		if(s1 == null) throw PLAYER_READ_EXCEPTION;
		String s2 = in.asCharBuffer().toString();
		if(s2 == null) throw PLAYER_READ_EXCEPTION;
		// todo verify we can just concatenate these
		ship_shape = new ShipShape(s1 + s2);
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
