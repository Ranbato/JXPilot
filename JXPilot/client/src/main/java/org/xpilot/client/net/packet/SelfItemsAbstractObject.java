package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

import static org.xpilot.common.Item.NUM_ITEMS;

/**
 * Holds data from Self Items packet.
 * @author Vlad Firoiu
 */
public final class SelfItemsAbstractObject extends XPilotAbstractObject {

	private int mask;
	private byte[] items = new byte[NUM_ITEMS.ord];
	
	public int getMask() {return mask;}
	public byte[] getItems() {return items;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		mask = in.getInt();
		for (int i = 0; mask != 0; i++) {
			if ((mask & (1 << i))!=0) {
				mask ^= (1 << i);
				if (i < NUM_ITEMS.ord) {
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
