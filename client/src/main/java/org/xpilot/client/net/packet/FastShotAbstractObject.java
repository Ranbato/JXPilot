package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a FastShot packet.
 * @author Vlad Firoiu
 */
public class FastShotAbstractObject extends XPilotAbstractObject {
	
	private byte type;
	private short num;
	
	public byte getType(){return type;}
	public short getNum(){return num;}
	
	@Override
	public void readPacket(ByteBuffer in) {
		pkt_type = in.get();
		type = in.get();
		num = in.getUnsignedByte();
	}
	
	@Override
	public String toString() {
		return "Fast Shot Packet\npacket type = " + pkt_type +
				"\ntype = " + type +
				"\nnum = " + num;
	}
}
