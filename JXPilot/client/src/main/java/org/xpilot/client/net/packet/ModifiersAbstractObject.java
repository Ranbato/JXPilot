package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a modifiers packet.
 * @author Vlad Firoiu
 */
public final class ModifiersAbstractObject extends XPilotAbstractObject {
	private final PacketReadException MODIFIERS_READ_EXCEPTION = new PacketReadException("Modifiers");

	private String modifiers;
	public String getModifiers(){return modifiers;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		if((modifiers = in.asCharBuffer().toString()) == null) throw MODIFIERS_READ_EXCEPTION;
	}

	@Override
	public String toString() {
		return "Modifiers Packet\npacket type = " + pkt_type +
				"\nmodifiers = " + modifiers;
	}
}
