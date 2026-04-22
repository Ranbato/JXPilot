package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.BaseHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a base packet.
 * @author Vlad Firoiu
 */
public final class BasePacket extends BaseHolder implements XPilotPacket {
	/**
	 * Length of base packet (7).
	 */
	public static final int LENGTH = 1 + 2 + 4;//7
	
	private final ReliableReadException BASE_READ_EXCEPTION = new ReliableReadException("Base");
	
	private byte pkt_type;
	
	public byte getPacketType(){return pkt_type;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.length()<LENGTH) throw BASE_READ_EXCEPTION;
		pkt_type = in.getByte();
		id = in.getShort();
		num = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Base Packet\npacket type = " + pkt_type +
				"\nid = " + id +
				"\nnum = " + num;
	}
}
