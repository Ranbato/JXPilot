package net.sf.jxpilot.net;

import static net.sf.jxpilot.net.ReliableDataError.*;
import static net.sf.jxpilot.net.packet.Packet.PKT_RELIABLE;
import net.sf.jgamelibrary.util.ByteBuffer;
import net.sf.jxpilot.net.packet.PacketReadException;
import net.sf.jxpilot.net.packet.XPilotPacketAdaptor;
import static net.sf.jxpilot.JXPilot.PRINT_RELIABLE;

/**
 * Class to hold reliable data packets.
 * Note that this class takes complete care of the reliable data stream, including
 * dropping duplicate/out of order packets, copying reliable data, and sending acks(through a client).
 * 
 * @author Vlad Firoiu
 */
public class ReliableData extends XPilotPacketAdaptor {
	public static final int LENGTH = 1+2+4+4;

	private int offset = 0;

	private short len;
	private int rel;
	private int rel_loops;

	private Ack ack = new Ack();
	
	/*
	int Receive_reliable(void)
	{
	    int			n;
	    short		len;
	    u_byte		ch;
	    long		rel,
				rel_loops;

	    if ((n = Packet_scanf(&rbuf, "%c%hd%ld%ld",
				  &ch, &len, &rel, &rel_loops)) == -1)
		return -1;
	    if (n == 0) {
		warn("Incomplete reliable data packet");
		return 0;
	    }
	#ifdef DEBUG
	    if (reliable_offset >= rel + len)
		printf("Reliable my=%ld pkt=%ld len=%d loops=%ld\n",
		       reliable_offset, rel, len, rel_loops);
	#endif
	    if (len <= 0) {
		warn("Bad reliable data length (%d)", len);
		return -1;
	    }
	    if (rbuf.ptr + len > rbuf.buf + rbuf.len) {
		warn("Not all reliable data in packet (%d,%d,%d)",
		     rbuf.ptr - rbuf.buf, len, rbuf.len);
		rbuf.ptr += len;
		Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
		return -1;
	    }
	    if (rel > reliable_offset) {
		
		// We miss one or more packets.
		// For now we drop this packet.
		// We could have kept it until the missing packet(s) arrived.
		
		rbuf.ptr += len;
		Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
		if (Send_ack(rel_loops) == -1)
		    return -1;
		return 1;
	    }
	    if (rel + len <= reliable_offset) {
		
		// Duplicate data.  Probably an ack got lost.
		// Send an ack for our current stream position.
		
		rbuf.ptr += len;
		Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
		if (Send_ack(rel_loops) == -1)
		    return -1;
		return 1;
	    }
	    if (rel < reliable_offset) {
		len -= (short)(reliable_offset - rel);
		rbuf.ptr += reliable_offset - rel;
		rel = reliable_offset;
	    }
	    if (cbuf.ptr > cbuf.buf)
		Sockbuf_advance(&cbuf, cbuf.ptr - cbuf.buf);
	    if (Sockbuf_write(&cbuf, rbuf.ptr, len) != len) {
		warn("Can't copy reliable data to buffer");
		rbuf.ptr += len;
		Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
		return -1;
	    }
	    reliable_offset += len;
	    rbuf.ptr += len;
	    Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
	    if (Send_ack(rel_loops) == -1)
		return -1;
	    return 1;
	}
	 */
	
	public ReliableDataError readReliableData(ByteBuffer in, NetClient client) throws PacketReadException {
		if (in.length()<LENGTH) return BAD_PACKET;
		
		readPacket(in);
		
		if (pkt_type != PKT_RELIABLE) {
			//in.position(in.position()-1);
			return NOT_RELIABLE_DATA;
		}
		if (len > in.length()) {
			
			in.clear();
			System.out.println("Not all reliable data in packet");
			//Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.in);
			return BAD_PACKET;
		}
		if (rel > offset) {
			/*
			 * We miss one or more packets.
			 * For now we drop this packet.
			 * We could have kept it until the missing packet(s) arrived.
			 */
			in.advanceReader(len);
			//System.out.println("Packet out of order");
			client.sendAck(ack.setAck(this));
			return OUT_OF_ORDER;
		}
		if (rel + len <= offset) {
			/*
			 * Duplicate data.  Probably an ack got lost.
			 * Send an ack for our current stream position.
			 */
			
			in.advanceReader(len);
			client.sendAck(ack.setAck(this));
			if(PRINT_RELIABLE) System.out.println("Duplicate Data");
			return DUPLICATE_DATA;
		}
		
		/*
		 * I am not sure what this does, but the C client has this code
		 * and without it errors sometimes occur.
		 */
		if (rel < offset) {
			len -= (short)(offset - rel);
			in.advanceReader(offset - rel);
			rel = offset;
		}
		
		if (PRINT_RELIABLE) System.out.println(this);
		
		offset += len;
		client.sendAck(ack.setAck(this));
		
		return NO_ERROR;
	}
	
	public ReliableDataError readReliableData(ByteBuffer in, NetClient client, ByteBuffer reliableBuf) throws PacketReadException {
		ReliableDataError error = readReliableData(in, client);

		if (error == NO_ERROR) {
			reliableBuf.putBytes(in, len);
			in.advanceReader(len);
		}

		return error;
	}

	public ReliableData setData(byte pkt_type, short len, int rel, int rel_loops) {
		this.pkt_type = pkt_type;
		this.len = len;
		this.rel = rel;
		this.rel_loops = rel_loops;
		return this;
	}

	public byte getPktType() {return pkt_type;}
	public short getLen() {return len;}
	public int getRel() {return rel;}
	public int getRelLoops() {return rel_loops;}

	public int getOffset() {return offset;}

	@Override
	public String toString() {
		return "\nReliable Data" +
		"\npacket type = " + pkt_type +
		"\nlen = " + len +
		"\nrel = " + rel +
		"\nrel loops = " + rel_loops;
	}

	@Override
	public void readPacket(ByteBuffer in) throws PacketReadException {
		pkt_type = in.getByte();
		len = in.getShort();
		rel = in.getInt();
		rel_loops = in.getInt();
	}
}