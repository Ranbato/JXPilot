package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Fast Radar packet.
 * @author Vlad Firoiu
 */
public final class FastRadarAbstractObject extends XPilotAbstractObject {

	/**
	 * Unsigned byte.
	 */
	private short num;

	public short getNum(){return num;}


	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		num = (short)Byte.toUnsignedInt(in.get());
		short x, y, size;

		for (short i =0;i<num;i++) {
			x = (short)Byte.toUnsignedInt(in.get());
			y= (short)Byte.toUnsignedInt(in.get());

			byte b = in.get();

			y |= (b & 0xC0) << 2;

			size = (short) (b & 0x07);
			if ((b & 0x20)!=0) {
				size |= 0x80;
			}

		}
	}

	@Override
	public String toString() {
		return "Fast Radar Packet\npacket type = " + pkt_type +
				"\nnum = " + num;
	}
}
