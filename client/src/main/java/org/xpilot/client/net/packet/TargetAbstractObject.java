package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a target packet.
 * @author Vlad Firoiu
 */
public class TargetAbstractObject extends XPilotAbstractObject {

	private short num, dead_time, damage;
	
	public short getNum(){return num;}
	public short getDeadTime(){return dead_time;}
	public short getDamage(){return damage;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		num = in.getShort();
		dead_time = in.getShort();
		damage = in.getShort();
	}
	
	@Override
	public String toString() {
		return "Target Packet\npacket type = " + pkt_type +
				"\nnum = " + num +
				"\ndead time = " + dead_time +
				"\ndamage = " + damage;
	}

}
