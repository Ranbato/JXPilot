package org.xpilot.client.net.packet;


/**
 * Convenience class that takes care of packet type.
 * @author Vlad Firoiu
 */
public abstract class XPilotAbstractObject implements XPilotPacket {
	/**
	 * The packet type.
	 */
	protected byte pkt_type;

	protected int x;
	protected int y;
	protected int id;
	
	/**
	 * @return The packet type.
	 */
	@Override
	public byte getPacketType(){return pkt_type;}
}
