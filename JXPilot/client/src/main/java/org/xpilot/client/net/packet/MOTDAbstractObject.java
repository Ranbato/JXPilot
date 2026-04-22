package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a MOTD (Message of the Day) packet.
 * @author Vlad Firoiu
 */
public final class MOTDAbstractObject extends XPilotAbstractObject {
	/**
	 * The number of bytes in MOTD packet.
	 */
	public static final int LENGTH = 1 + 4 + 2 + 4;//11
	
	private final ReliableReadException MOTD_READ_EXCEPTION = new ReliableReadException("MOTD");
	
	private int offset, size;
	private short length;
	private String motd;
	
	public int getOffset(){return offset;}
	public short getLength(){return length;}
	public int getSize(){return size;}
	public String getMOTD(){return motd;}
	
	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.remaining()<=LENGTH) throw this.MOTD_READ_EXCEPTION;
		pkt_type = in.get();
		offset = in.getInt();
		length = in.getShort();
		size = in.getInt();
		if((motd = in.asCharBuffer().toString()) == null) throw this.MOTD_READ_EXCEPTION;
	}

	@Override
	public String toString() {
		return "MOTD Packet\npacket type = " + pkt_type +
				"\noffset = " + offset +
				"\nlength = " + length +
				"\nsize = " + size +
				"\nmotd = " + motd;
	}
}
