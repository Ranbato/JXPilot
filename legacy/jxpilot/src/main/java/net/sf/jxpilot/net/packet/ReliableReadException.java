package net.sf.jxpilot.net.packet;


/**
 * This exception is thrown when the reliable data stream doesn't have 
 * enough data left for a reliable packet. This is normal, since reliable data comes as
 * stream and XPilot packets may be split up among UDP packets (unfortunately).
 * @author Vlad Firoiu
 */
public class ReliableReadException extends PacketReadException {
	private static final long serialVersionUID = 1L;
	public ReliableReadException(){}
	public ReliableReadException(String message){super(message);}	
}
