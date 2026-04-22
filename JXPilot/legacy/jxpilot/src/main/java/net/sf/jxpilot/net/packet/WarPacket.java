package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data for a War packet.
 * @author Vlad Firoiu
 */
public final class WarPacket extends XPilotPacketAdaptor {
	/**
	 * The number of bytes in a War packet.
	 */
	public static final int LENGTH = 1 + 2 + 2;//5

	private final ReliableReadException WAR_READ_EXCEPTION = new ReliableReadException("War");
	
	private short robot_id, killer_id;
	
	public short getRobotId(){return robot_id;}
	public short getKillerId(){return killer_id;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.length()<LENGTH) throw WAR_READ_EXCEPTION; 
		pkt_type = in.getByte();
		robot_id = in.getShort();
		killer_id = in.getShort();
	}
	@Override
	public String toString() {
		return "War Packet\npacket type = " + pkt_type +
				"\nrobot id = " + robot_id +
				"\nkiller_id = " + killer_id;
	}
}
