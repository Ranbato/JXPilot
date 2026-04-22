package net.sf.jxpilot.net;

import static net.sf.jxpilot.net.packet.Packet.PKT_ACK;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data for an Acknowledgement.
 * @author Vlad Firoiu
 */
public class Ack {
	public static Ack ack = new Ack();
	
	private byte pkt_type = PKT_ACK;
	private int reliable_offset, reliable_loops;
	
	public Ack setAck(int offset, int loops) {
		reliable_offset = offset;
		reliable_loops = loops;
		return this;
	}
	
	public Ack setAck(ReliableData data) {
		return setAck(data.getOffset(), data.getRelLoops());
	}
	
	public static void putAck(ByteBuffer buf, Ack ack) {
		//buf.clear();
		buf.putByte(ack.getPacketType());
		buf.putInt(ack.getOffset());
		buf.putInt(ack.getLoops());
	}
	
	public void putPacket(ByteBuffer out) {
		out.putByte(pkt_type).putInt(reliable_offset).putInt(reliable_loops);
	}
	
	public byte getPacketType(){return pkt_type;}
	public int getOffset(){return reliable_offset;}
	public int getLoops(){return reliable_loops;}
	
	@Override
	public String toString() {
		return  "Acknowledgement"
				+ "\ntype = " + String.format("%x", pkt_type)
				+ "\nreliable offset = " + reliable_offset
				+ "\nreliable loops = " + reliable_loops;
	}
}
