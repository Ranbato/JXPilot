package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from an Audio packet.
 * @author Vlad Firoiu
 */
public final class AudioAbstractObject extends XPilotAbstractObject {
	private byte type, volume;
	
	public byte getType(){return type;}
	public byte getVolume(){return volume;}
	
	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.get();
		type = in.get();
		volume = in.get();
	}
	
	@Override
	public String toString() {
		return "Audio Packet\npacket type = " + pkt_type +
				"\ntype = " + type +
				"\nvolume = " + volume;
	}
}
