package org.xpilot.common;

/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 2003-2004 by
 *
 *      Uoti Urpala          <uau@users.sourceforge.net>
 *      Kristian Söderblom   <kps@users.sourceforge.net>
 *
 * Copyright (C) 1991-2001 by
 *
 *      Bjørn Stabell        <bjoern@xpilot.org>
 *      Ken Ronny Schouten   <ken@xpilot.org>
 *      Bert Gijsbers        <bert@xpilot.org>
 *      Dick Balaska         <dick@xpilot.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;

public class Setup{
    
    private static final Logger logger = LoggerFactory.getLogger(Setup.class);

/*
 * Definitions to tell the client how the server has been setup.
 */

/*
 * If the high bit of a map block is set then the next block holds
 * the number of contiguous map blocks that have the same block type.
 */
public static final int SETUP_COMPRESSED = 0x80;

/*
 * Tell the client how and if the map is compressed.
 * This is only for client compatibility with the old protocol.
 */
public static final int SETUP_MAP_ORDER_XY = 1;
public static final int SETUP_MAP_ORDER_YX = 2;
public static final int SETUP_MAP_UNCOMPRESSED = 3;

/*
 * Definitions for the map layout which permit a compact definition
 * of map data.
 */
public static final int SETUP_SPACE = 0;
public static final int SETUP_FILLED = 1;
public static final int SETUP_FILLED_NO_DRAW = 2;
public static final int SETUP_FUEL = 3;
public static final int SETUP_REC_RU = 4;
public static final int SETUP_REC_RD = 5;
public static final int SETUP_REC_LU = 6;
public static final int SETUP_REC_LD = 7;
public static final int SETUP_ACWISE_GRAV = 8;
public static final int SETUP_CWISE_GRAV = 9;
public static final int SETUP_POS_GRAV = 10;
public static final int SETUP_NEG_GRAV = 11;
public static final int SETUP_WORM_NORMAL = 12;
public static final int SETUP_WORM_IN = 13;
public static final int SETUP_WORM_OUT = 14;
public static final int SETUP_CANNON_UP = 15;
public static final int SETUP_CANNON_RIGHT = 16;
public static final int SETUP_CANNON_DOWN = 17;
public static final int SETUP_CANNON_LEFT = 18;
public static final int SETUP_SPACE_DOT = 19;
public static final int SETUP_TREASURE = 20;	/* + team number (10) */
public static final int SETUP_BASE_LOWEST = 30;	/* lowest base number */
public static final int SETUP_BASE_UP = 30;	/* + team number (10) */
public static final int SETUP_BASE_RIGHT = 40;	/* + team number (10) */
public static final int SETUP_BASE_DOWN = 50;	/* + team number (10) */
public static final int SETUP_BASE_LEFT = 60;	/* + team number (10) */
public static final int SETUP_BASE_HIGHEST = 69;	/* highest base number */
public static final int SETUP_TARGET = 70;	/* + team number (10) */
public static final int SETUP_CHECK = 80;	/* + check point number (26) */
public static final int SETUP_ITEM_CONCENTRATOR = 110;
public static final int SETUP_DECOR_FILLED = 111;
public static final int SETUP_DECOR_RU = 112;
public static final int SETUP_DECOR_RD = 113;
public static final int SETUP_DECOR_LU = 114;
public static final int SETUP_DECOR_LD = 115;
public static final int SETUP_DECOR_DOT_FILLED = 116;
public static final int SETUP_DECOR_DOT_RU = 117;
public static final int SETUP_DECOR_DOT_RD = 118;
public static final int SETUP_DECOR_DOT_LU = 119;
public static final int SETUP_DECOR_DOT_LD = 120;
public static final int SETUP_UP_GRAV = 121;
public static final int SETUP_DOWN_GRAV = 122;
public static final int SETUP_RIGHT_GRAV = 123;
public static final int SETUP_LEFT_GRAV = 124;
public static final int SETUP_ASTEROID_CONCENTRATOR = 125;

public static final byte BLUE_UP = 0x01;
public static final byte BLUE_RIGHT = 0x02;
public static final byte BLUE_DOWN = 0x04;
public static final byte BLUE_LEFT = 0x08;
public static final byte BLUE_OPEN = 0x10;	/* diagonal botleft . rightup */
public static final byte BLUE_CLOSED = 0x20;	/* diagonal topleft . rightdown */
public static final byte BLUE_FUEL = 0x30;	/* when filled block is fuelstation */
public static final byte BLUE_BELOW = 0x40;	/* when triangle is below diagonal */
public static final byte BLUE_BIT = (byte) 0x80b;	/* set when drawn with blue lines */

public static final byte DECOR_LEFT = 0x01;
public static final byte DECOR_RIGHT = 0x02;
public static final byte DECOR_DOWN = 0x04;
public static final byte DECOR_UP = 0x08;
public static final byte DECOR_OPEN = 0x10;
public static final byte DECOR_CLOSED = 0x20;
public static final byte DECOR_BELOW = 0x40;

/*
 * Structure defining the server configuration, including the map layout.
 */

public    long		setup_size;		/* size including map data */
public    int		map_data_len;		/* num. compressed map bytes */
public  BitSet mode = new BitSet(32);			/* playing mode */
public    short		lives;			/* max. number of lives */
public    short		x;			/* OLD width in blocks */
public    short		y;			/* OLD height in blocks */
public    short		width;			/* width in pixels */
public    short		height;			/* height in pixels */
public    short		frames_per_second;	/* FPS */
public    short		map_order;		/* OLD row or col major */
public    short		unused1;		/* padding */
public    String		name;//[MAX_CHARS];	/* name of map */
public    String		author;//[MAX_CHARS];	/* name of author of map */
public    String		data_url;//[MSG_LEN];
     	/* location where client
						   can load additional data
						   like bitmaps; MSG_LEN to
						   allow >80 chars */
     	// todo this allocation is obviously incorrect,should be Setup.x * Setup.y
        // could be a byte []
 public   byte[]	map_data ;		/* compressed map data */
    /* plus more mapdata here (HACK) */

/*
TODO FPS setup for client and server

#ifndef SERVER
# ifdef FPS
#  error "FPS needs a different definition in the client"
#  undef FPS
# endif
# define FPS		(Setup.frames_per_second)
*/


    /*
     * Uncompress the map which is compressed using a simple
     * Run-Length-Encoding algorithm.
     * The map object type is encoded in the lower seven bits
     * of a byte.
     * If the high bit of a byte is set then the next byte
     * means the number of contiguous map data bytes that
     * have the same type.  Otherwise only one map byte
     * has this type.
     * Because we uncompress the map backwards to save on
     * memory usage there is some complexity involved.
     */
    public int Uncompress_map()
    {
        int	cmp,		/* compressed map pointer */
                ump,		/* uncompressed map pointer */
                p;		/* temporary search pointer */
        int		i,
                count;

        if (map_order != SETUP_MAP_ORDER_XY) {
            logger.warn("Unknown map ordering in setup ({})", map_order);
            return -1;
        }

    /* Point to last compressed map byte */
        cmp = map_data.length - 1;

    /* Point to last uncompressed map byte */
        ump =  x * y - 1;

        while (cmp >= 0) {
            for (p = cmp; p > 0; p--) {
                if ((map_data[p-1] & SETUP_COMPRESSED) == 0)
                    break;
            }
            if (p == cmp) {
                map_data[ump--] = map_data[cmp--];

                continue;
            }
            if ((cmp - p) % 2 == 0)
                map_data[ump--] = map_data[cmp--];
            while (p < cmp) {
                count = map_data[cmp--];
                if (count < 2) {
                    logger.warn("Map compress count error {}", count);
                    return -1;
                }
                map_data[cmp] &= ~SETUP_COMPRESSED;
                for (i = 0; i < count; i++)
                    map_data[ump--] = map_data[cmp];
                cmp--;
                if (ump < cmp) {
                    logger.warn("Map uncompression error ({},{})",
                            cmp, ump);
                    return -1;
                }
            }
        }
        if (ump != cmp) {
            logger.warn("map uncompress error ({},{})",
                    cmp, ump);
            return -1;
        }
        map_order = SETUP_MAP_UNCOMPRESSED;
        return 0;
    }

}
