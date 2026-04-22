package org.xpilot.client.net.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpilot.client.Client;
import org.xpilot.common.Setup;

import java.nio.ByteBuffer;
import java.util.BitSet;

import static org.xpilot.client.Client.oldServer;
import static org.xpilot.common.Const.BLOCK_SZ;
import static org.xpilot.common.Setup.SETUP_MAP_ORDER_XY;
import static org.xpilot.common.Setup.SETUP_MAP_UNCOMPRESSED;


public class SetupPacket extends XPilotAbstractObject {
    public static final int LENGTH = 1 + 1 + 1;//3

    private static final Logger logger = LoggerFactory.getLogger(SetupPacket.class);

    public SetupPacket(Setup setup) {
        this.setup = setup;
    }

    Setup setup;

    @Override
    public String toString() {
        return String.format("Setup\npacket type = %x\n$s", pkt_type, setup);
    }

    @Override
    public void readPacket(ByteBuffer in) throws ReliableReadException {

        int size;

        if (oldServer) {

            setup.map_data_len = (int) in.getLong();
            setup.mode = BitSet.valueOf(new long[]{in.getLong()});
            setup.lives = in.getShort();
            setup.x = in.getShort();
            setup.y = in.getShort();
            setup.frames_per_second = in.getShort();
            setup.map_order = in.getShort();
            setup.name = in.asCharBuffer().toString();
            setup.author = in.asCharBuffer().toString();
            setup.width = (short) (setup.x * BLOCK_SZ);
            setup.height = (short) (setup.y * BLOCK_SZ);
        } else {
            setup.map_data_len = (int) in.getLong();
            setup.mode = BitSet.valueOf(new long[]{in.getLong()});
            setup.lives = in.getShort();
            setup.width = in.getShort();
            setup.height = in.getShort();
            setup.frames_per_second = in.getShort();
            setup.name = in.asCharBuffer().toString();
            setup.author = in.asCharBuffer().toString();
            setup.data_url = in.asCharBuffer().toString();
        }
        if (false) {
            logger.warn("Can't read setup info from reliable data buffer");
            throw new ReliableReadException("Unable to read setup data");
        }

		/*
         * Do some consistency checks on the server setup structure.
		 */
        if (setup.map_data_len <= 0
                || setup.width <= 0
                || setup.height <= 0
                || (oldServer && setup.map_data_len >
                setup.x * setup.y)) {
            logger.warn("Got bad map specs from server ({},{},{})",
                    setup.map_data_len, setup.width, setup.height);
            throw new ReliableReadException("Bad map specs");
        }
        if (oldServer && setup.map_order != SETUP_MAP_ORDER_XY
                && setup.map_order != SETUP_MAP_UNCOMPRESSED) {
            logger.warn("Unknown map order type ({})", setup.map_order);
            throw new ReliableReadException("Unknown map order");
        }
        if (oldServer) {
            size = setup.x * setup.y;
        } else {
            size = setup.map_data_len;
        }

        setup.map_data = new byte[size];

        if (Client.oldServer && (setup.map_order != SETUP_MAP_UNCOMPRESSED)) {
            if (setup.Uncompress_map() == -1) {
                throw new ReliableReadException("Unable to uncompress map");
            }
        }
    }

}