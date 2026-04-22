package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a FastShot packet.
 * @author Vlad Firoiu
 */
public class FastShotPacket extends XPilotPacketAdaptor {
	
	private byte type;
	private short num;
	
	public byte getType(){return type;}
	public short getNum(){return num;}
	
	@Override
	public void readPacket(ByteBuffer in) {
		pkt_type = in.getByte();
		type = in.getByte();
		num = in.getUnsignedByte();
	}
	
	@Override
	public String toString() {
		return "Fast Shot Packet\npacket type = " + pkt_type +
				"\ntype = " + type +
				"\nnum = " + num;
	}
}
