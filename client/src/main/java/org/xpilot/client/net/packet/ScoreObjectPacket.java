package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;
import net.sf.jxpilot.game.ScoreObjectHolder;

/**
 * Holds data from a Score Object packet.
 * @author Vlad Firoiu
 */
public class ScoreObjectPacket extends ScoreObjectHolder implements XPilotPacket {
	/**
	 * The minimum number of bytes in a Score Object packet.
	 */
	public static final int LENGTH = 1 + 2 + 2 + 2 + 1;//8
	
	private final ReliableReadException SCORE_OBJECT_READ_EXCEPTION = new ReliableReadException("Score Object");
	private byte pkt_type;
	
	@Override
	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if (in.length()<LENGTH) throw SCORE_OBJECT_READ_EXCEPTION;
		
		pkt_type = in.get();
		//float score = (float)(in.getInt()/100.0);
		score = in.getShort();
		x  = Short.toUnsignedInt(in.getShort());
		y = Short.toUnsignedInt(in.getShort());
		if((message = in.asCharBuffer().toString()) == null) throw SCORE_OBJECT_READ_EXCEPTION;
	}

	@Override
	public String toString() {
		return "Score Object Packet\npacket type = " + pkt_type +
					"\nscore = " + score +
					"\nx = " + x +
					"\ny = " + y +
					"\nmessage: " + message;
	}

	/*
		int Receive_score_object(void)
		{
			int			n;
			unsigned short	x, y;
			DFLOAT		score = 0;
			char		msg[MAX_CHARS];
			u_byte		ch;

			if (version < 0x4500) {
				short	rcv_score;
				n = Packet_scanf(&cbuf, "%c%hd%hu%hu%s",
						&ch, &rcv_score, &x, &y, msg);
				score = rcv_score;
			} else {
				// newer servers send scores with two decimals
				int	rcv_score;
				n = Packet_scanf(&cbuf, "%c%d%hu%hu%s",
						&ch, &rcv_score, &x, &y, msg);
				score = (DFLOAT)rcv_score / 100;
			}
			if (n <= 0)
				return n;
			if ((n = Handle_score_object(score, x, y, msg)) == -1) {
				return -1;
			}

			return 1;
		} 
	 */
}
