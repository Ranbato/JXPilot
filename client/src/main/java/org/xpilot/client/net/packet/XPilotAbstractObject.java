package org.xpilot.client.net.packet;


import java.nio.ByteBuffer;

/**
 * Convenience class that takes care of packet type.
 * @author Vlad Firoiu
 */
public abstract class XPilotAbstractObject implements XPilotPacket {
    /**
     * The packet type.
     */
    protected byte pkt_type;

    protected int x;
    protected int y;
    protected int id;

    /**
     * @return The packet type.
     */
    @Override
    public byte getPacketType() {
        return pkt_type;
    }

//	public String getString(ByteBuffer in) {
//		for(int i = this.reader; i < this.writer; ++i) {
//			if (this.buf[i] == 0) {
//				char[] temp = new char[i - this.reader];
//
//				for(int j = this.reader; j < i; ++j) {
//					temp[j - this.reader] = (char)this.buf[j];
//				}
//
//				this.reader = i + 1;
//				return new String(temp);
//			}
//		}
//}
}
