package org.xpilot.client.net.packet;

import java.io.IOException;

/**
 * Thrown to indicate that an exception occurred while reading an XPilot packet.
 * @author Vlad Firoiu
 */
public class PacketReadException extends IOException {
	private static final long serialVersionUID = 1L;
	public PacketReadException(){}
	public PacketReadException(String message){super(message);}
}
