package net.sf.jxpilot.net.packet;

import net.sf.jxpilot.game.SelfHolder;
import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a Self packet.
 * @author Vlad Firoiu
 */
public final class SelfPacket extends SelfHolder implements XPilotPacket {
	
	/**
	 * The number of bytes in a self packet.
	 */
	public static final int LENGTH = (1 + 2 + 2 + 2 + 2 + 1) + (1 + 1 + 1 + 2 + 2 + 1 + 1 + 1) + (2 + 2 + 2 + 2 + 1 + 1) + 1;//31
	
	private final PacketReadException SELF_READ_EXCEPTION = new PacketReadException();
	
	private byte pkt_type;
	
	@Override
	public byte getPacketType() {return pkt_type;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		if(in.length()<LENGTH) throw SELF_READ_EXCEPTION;//should not happen
		
		pkt_type = in.getByte();
		x = in.getShort();
		y = in.getShort();
		vx = in.getShort();
		vy = in.getShort();
		heading = in.getByte();

		power = in.getByte();
		turnspeed = in.getByte();
		turnresistance = in.getByte();
		lockId = in.getShort();
		lockDist = in.getShort();
		lockDir = in.getByte();
		nextCheckPoint = in.getByte();

		currentTank = in.getByte();
		fuelSum = in.getShort();
		fuelMax = in.getShort();
		ext_view_width = in.getShort();
		ext_view_height = in.getShort();
		debris_colors = in.getByte();
		stat = in.getByte();
		autopilot_light = in.getByte();
	}
	
	@Override
	public String toString() {
		return "Packet Self\npacket type = " + pkt_type +
				"\nx = " + x +
				"\ny = " + y +
				"\nvx = " + vx +
				"\nvy = " + vy +
				"\nheading = " + heading +
				"\nautopilotLight = " + autopilot_light;
	}
}
