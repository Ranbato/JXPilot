package net.sf.jxpilot.net.packet;

import net.sf.jgamelibrary.util.ByteBuffer;

/**
 * Holds data from a modifiers packet.
 * @author Vlad Firoiu
 */
public final class ModifiersPacket extends XPilotPacketAdaptor {
	private final PacketReadException MODIFIERS_READ_EXCEPTION = new PacketReadException("Modifiers");

	private String modifiers;
	public String getModifiers(){return modifiers;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		if((modifiers = in.getString()) == null) throw MODIFIERS_READ_EXCEPTION;
	}

	@Override
	public String toString() {
		return "Modifiers Packet\npacket type = " + pkt_type +
				"\nmodifiers = " + modifiers;
	}
}
