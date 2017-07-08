package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Damaged packet.
 * @author Vlad Firoiu
 */
public final class DamagedPacket extends XPilotPacketAdaptor {

	private byte damaged;
	
	public byte getDamaged(){return damaged;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		damaged = in.getByte();
	}

	@Override
	public String toString() {
		return "Damaged Packet\npacket type = " + pkt_type +
				"\ndamaged = " + damaged;
	}
}
