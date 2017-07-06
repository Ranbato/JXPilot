/* 
 * XPilot NG, a multiplayer space war game.
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

public class Net{

public static final int MIN_SOCKBUF_SIZE = 1024;
public static final int MAX_SOCKBUF_SIZE = (50*1024);

public static final int SERVER_RECV_SIZE = MIN_SOCKBUF_SIZE;
public static final int SERVER_SEND_SIZE = (4*1024);

public static final int CLIENT_SEND_SIZE = SERVER_RECV_SIZE;
/* I added 1024 to this because the client can get 4 1035 byte packets
   at once when starting a game (from Handle_setup). Seems there is some
   overhead in storing multiple packets - I had to increase this by at
   least 657 to avoid losing packets on Linux. That's why the change here
   instead of changing the size to 1024 in netserver.c */
public static final int CLIENT_RECV_SIZE = (SERVER_SEND_SIZE + 1024);

/*
 * Definitions for the states a socket buffer can be in.
 */
public static final int SOCKBUF_READ = 0x01	/* if readable */;
public static final int SOCKBUF_WRITE = 0x02	/* if writeable */;
public static final int SOCKBUF_LOCK = 0x04	/* if locked against kernel i/o */;
public static final int SOCKBUF_ERROR = 0x08	/* if i/o error occurred */;
public static final int SOCKBUF_DGRAM = 0x10	/* if datagram socket */;

/*
 * Hack: leave some spare room for the last terminating packet
 * of a frame update.
 */
public static final int SOCKBUF_WRITE_SPARE = 8;

/*
 * Maximum number of socket i/o retries if datagram socket.
 */
public static final int MAX_SOCKBUF_RETRIES = 2;

/*
 * A buffer to reduce the number of system calls made and to reduce
 * the number of network packets.
 */
typedef struct {
    sock_t	sock;		/* socket filedescriptor */
    char	*buf;		/* i/o data buffer */
    int		size;		/* size of buffer */
    int		len;		/* amount of data in buffer (writing/reading) */
    char	*ptr;		/* current position in buffer (reading) */
    int		state;		/* read/write/locked/error status flags */
} sockbuf_t;
}
