package net.sf.jxpilot.net.packet;


/**
 * Convenience class that takes care of packet type.
 * @author Vlad Firoiu
 */
public abstract class XPilotPacketAdaptor implements XPilotPacket {
	/**
	 * The packet type.
	 */
	protected byte pkt_type;
	
	/**
	 * @return The packet type.
	 */
	@Override
	public byte getPacketType(){return pkt_type;}
}
