package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;
import net.sf.jxpilot.net.StringReadException;

/**
 * Holds data from a MOTD (Message of the Day) packet.
 * @author Vlad Firoiu
 */
public final class MOTDPacket extends XPilotPacketAdaptor {
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
		if(in.length()<=LENGTH) throw this.MOTD_READ_EXCEPTION;
		pkt_type = in.getByte();
		offset = in.getInt();
		length = in.getShort();
		size = in.getInt();
		if((motd = in.getString()) == null) throw this.MOTD_READ_EXCEPTION;
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
