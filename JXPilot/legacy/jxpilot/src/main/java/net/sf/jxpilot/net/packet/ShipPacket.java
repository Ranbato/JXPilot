package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.ShipHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Ship packet.
 * @author Vlad Firoiu
 */
public final class ShipPacket extends ShipHolder implements XPilotPacket {
	
	//bit masks
	private static final byte
	SHIELD_FLAG = 1 << 0,
	CLOAK_FLAG = 1 << 1,
	EMERGENCY_SHIELD_FLAG = 1 << 2,
	PHASED_FLAG = 1 << 3,
	DEFLECTOR_FLAG = 1 << 4;
	
	private byte pkt_type, flags;
	
	@Override
	public byte getPacketType() {return pkt_type;}
	public byte getFlags(){return flags;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		id = in.getShort();
		heading = in.getByte();
		flags = in.getByte();

		shield = (flags & SHIELD_FLAG) != 0;
		cloak = (flags & CLOAK_FLAG) != 0;
		emergency_shield = (flags & EMERGENCY_SHIELD_FLAG) != 0;
		phased = (flags & PHASED_FLAG) != 0;
		deflector = (flags & DEFLECTOR_FLAG) != 0;
	}
	
	@Override
	public String toString() {
		return "Ship Packet\npacket type = " + pkt_type +
					"\nx = " + x +
					"\ny = " + y +
					"\nid = " + id +
					"\nheading = " + heading +
					"\nshield: " + shield +
					"\ncloak: " + cloak +
					"\nemergency shield: " + emergency_shield +
					"\nphased: " + phased +
					"\ndeflector: " + deflector;
	}
}
