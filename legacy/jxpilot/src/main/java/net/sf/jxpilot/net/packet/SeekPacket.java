package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.SeekHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Seek packet.
 * @author Vlad Firoiu
 */
public final class SeekPacket extends SeekHolder implements XPilotPacket {
	private byte pkt_type;
	@Override
	public byte getPacketType() {return pkt_type;}
	
	/**
	 * Reads the packet type, the programmer id, the robot id, and the
	 * sought id.
	 * @param in The buffer to read from.
	 */
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		programmer_id = in.getShort();
		robot_id = in.getShort();
		sought_id = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Seek Packet\npacket type = " + pkt_type +
				"\nprogrammer id = " + this.programmer_id +
				"\nrobot id = " + this.robot_id +
				"\nsought id = " + this.sought_id;
	}
}
