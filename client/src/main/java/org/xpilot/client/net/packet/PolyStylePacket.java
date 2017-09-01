package org.xpilot.client.net.packet;

import org.xpilot.common.ShipShape;

import java.nio.ByteBuffer;

import static org.xpilot.client.NetClient.MAX_MAP_ACK_LEN;
import static org.xpilot.common.Packet.PKT_ACK_POLYSTYLE;


public class PolyStylePacket implements XPilotPacket {
	
	public static final int LENGTH = 1 + 2 + 2;
	

	protected byte pkt_type;
    private short	num, newstyle;



	public byte getPacketType() {return pkt_type;}

	@Override
	public void readPacket(ByteBuffer in) throws ReliableReadException {
			pkt_type = in.get();
		num = in.get();
		newstyle = in.get();
		
		//@todo send ack
//        		if (wbuf.len < MAX_MAP_ACK_LEN)
//        Packet_printf(&wbuf, "%c%ld%hu", PKT_ACK_POLYSTYLE, last_loops, num);

	}
	
	@Override
	public String toString() {
		return "Polystyle Packet\npacket type = " + pkt_type +
				"\nnum = " + num +
				"\nnewstyle = " + newstyle;
	}


}
