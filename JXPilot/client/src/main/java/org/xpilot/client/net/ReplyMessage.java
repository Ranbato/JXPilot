package org.xpilot.client.net;

import java.nio.ByteBuffer;
import net.sf.jxpilot.net.packet.Status;

public class ReplyMessage {
	private int magic;
	private byte pack_type;
	private Status status;
	private short value;//unsigned short
	
	public ReplyMessage setMessage(int magic, byte pack_type, byte status, short value) {
		this.magic = magic;
		this.pack_type = pack_type;
		this.status = Status.getStatus(status);
		this.value = value;
		return this;
	}
	
	public ReplyMessage setMessage(int magic, byte pack_type, Status status, short value) {
		this.magic = magic;
		this.pack_type = pack_type;
		this.status = status;
		this.value = value;
		return this;
	}
	
	public int getMagic(){return magic;}
	public byte getPack(){return pack_type;}
	public Status getStatus(){return status;}
	public short getValue(){return value;}
	
	public String toString() {
		return "Reply Message" +
				"\nmagic = " + String.format("%x", magic) +
				"\npack type = " + String.format("%x", pack_type) +
				"\nstatus = " + status +
				"\nvalue = " + String.format("%x", value);
	}
	
	public static ReplyMessage readReplyMessage(ByteBuffer buf, ReplyMessage message) {
		message.setMessage(buf.getInt(), buf.getByte(), buf.getByte(), buf.length()>=2 ? buf.getShort(): 0);
		return message;
	}

}
