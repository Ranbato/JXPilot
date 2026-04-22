package org.xpilot.client.net.packet;

import java.nio.ByteBuffer;

/**
 * Holds data from a Score packet.
 * @author Vlad Firoiu
 */
public final class ScoreAbstractObject extends XPilotAbstractObject {
	/**
	 * The length of a score packet (8).
	 */
	public static final int LENGTH = 1+2+2+2+1;//8
	
	private final ReliableReadException SCORE_READ_EXCEPTION = new ReliableReadException();
	
	private short id, score, life;
	private byte my_char;
	
	public short getId(){return id;}
	public short getScore(){return score;}
	public short getLife(){return life;}
	public byte getMyChar(){return my_char;}

	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
		if(in.remaining()<LENGTH) throw SCORE_READ_EXCEPTION;
		pkt_type = in.get();
		id = in.getShort();
		score = in.getShort();
		life = in.getShort();
		my_char = in.get();
	}

	@Override
	public String toString() {
		return "\nScore Packet\npacket type = " + pkt_type +
				"\nid = " + id +
				"\nscore = " + score +
				"\nlife = " + life +
				"\nmy char = " + my_char;
	
	}
	
	/*
		int Receive_score(void)
		{
			int			n;
			short		id, life;
			DFLOAT		score = 0;
			u_byte		ch, mychar, alliance = ' ';

			if (version < 0x4500) {
				short	rcv_score;
				n = Packet_scanf(&cbuf, "%c%hd%hd%hd%c", &ch,
						&id, &rcv_score, &life, &mychar);
				score = rcv_score;
				alliance = ' ';
			} else {
				// newer servers send scores with two decimals 
				int	rcv_score;
				n = Packet_scanf(&cbuf, "%c%hd%d%hd%c%c", &ch,
						&id, &rcv_score, &life, &mychar, &alliance);
				score = (DFLOAT)rcv_score / 100;
			}
			if (n <= 0) {
				return n;
			}
			if ((n = Handle_score(id, score, life, mychar, alliance)) == -1) {
				return -1;
			}
			return 1;
		}

	 */
}
