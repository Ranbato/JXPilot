package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.RadarHolder;
import net.sf.jgamelibrary.util.ByteBuffer;
import net.sf.jxpilot.util.Factory;
import net.sf.jxpilot.util.HolderList;

/**
 * Holds data from a Fast Radar packet.
 * @author Vlad Firoiu
 */
public final class FastRadarPacket extends XPilotPacketAdaptor {

	/**
	 * Unsigned byte.
	 */
	private short num;
	private RadarHolder radarHolder = new RadarHolder();
	private HolderList<RadarHolder> radarHolders = new HolderList<RadarHolder>(
			new Factory<RadarHolder>() {
				public RadarHolder newInstance(){return new RadarHolder();}
			});
	
	public short getNum(){return num;}
	public HolderList<RadarHolder> getRadarHolders(){return radarHolders;}
	
	/**
	 * Clears all {@code RadarHolders} in this {@code FastRadarPacket}.
	 * This should be used before reading a new Fast Radar packet.
	 */
	public void clear() {
		radarHolders.clear();
	}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		num = in.getUnsignedByte();
		short x, y, size;

		for (short i =0;i<num;i++) {
			x = in.getUnsignedByte();
			y= in.getUnsignedByte();

			byte b = in.getByte();

			y |= (b & 0xC0) << 2;

			size = (short) (b & 0x07);
			if ((b & 0x20)!=0) {
				size |= 0x80;
			}

			radarHolders.add(radarHolder.setRadar(x, y, size));
		}
	}

	@Override
	public String toString() {
		return "Fast Radar Packet\npacket type = " + pkt_type +
				"\nnum = " + num;
	}
}
