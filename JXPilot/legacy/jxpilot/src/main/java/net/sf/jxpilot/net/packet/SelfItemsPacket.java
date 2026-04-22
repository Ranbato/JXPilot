package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.data.Items;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from Self Items packet.
 * @author Vlad Firoiu
 */
public final class SelfItemsPacket extends XPilotPacketAdaptor {

	private int mask;
	private byte[] items = new byte[Items.NUM_ITEMS];
	
	public int getMask() {return mask;}
	public byte[] getItems() {return items;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		mask = in.getInt();
		for (int i = 0; mask != 0; i++) {
			if ((mask & (1 << i))!=0) {
				mask ^= (1 << i);
				if (i < Items.NUM_ITEMS) {
					items[i] = in.getByte();
				} else {
					in.getByte();
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return "Self Items Packet\npacket type = " + pkt_type +
				"\nmask = " + String.format("%x", mask);
	}

}
