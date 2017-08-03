package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Seek packet.
 * @author Vlad Firoiu
 */
public final class SeekPacket implements XPilotPacket {
	private byte pkt_type;

	protected short programmer_id, robot_id, sought_id;

	public short getProgrammerId(){return this.programmer_id;}
	public short getRobotId(){return this.robot_id;}
	public short getSoughtId(){return this.sought_id;}

	@Override
	public byte getPacketType() {return pkt_type;}
	
	/**
	 * Reads the packet type, the programmer id, the robot id, and the
	 * sought id.
	 * @param in The buffer to read from.
	 */
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
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
