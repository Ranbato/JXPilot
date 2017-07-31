package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.data.Items;

/**
 * Holds data from Self Items packet.
 * @author Vlad Firoiu
 */
public final class SelfItemsAbstractObject extends XPilotAbstractObject {

	private int mask;
	private byte[] items = new byte[Items.NUM_ITEMS];
	
	public int getMask() {return mask;}
	public byte[] getItems() {return items;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		mask = in.getInt();
		for (int i = 0; mask != 0; i++) {
			if ((mask & (1 << i))!=0) {
				mask ^= (1 << i);
				if (i < Items.NUM_ITEMS) {
					items[i] = in.get();
				} else {
					in.get();
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
