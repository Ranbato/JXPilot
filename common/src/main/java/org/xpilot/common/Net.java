package org.xpilot.common;
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;

public class Net{

    static Logger logger = LoggerFactory.getLogger(Net.class);

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
public static class SocketBuf{
    DatagramChannel sock;		/* socket filedescriptor */
    ByteBuffer buf;		/* i/o data buffer */
    int		size;		/* size of buffer */
    int		len;		/* amount of data in buffer (writing/reading) */
//    char	*ptr;		/* current position in buffer (reading) */
//    int		state;		/* read/write/locked/error status flags */
}

int last_packet_of_frame;
//
//int Sockbuf_init(SocketBuf sbuf, DatagramChannel sock, int size, int state)
//{
//    sbuf.buf = ByteBuffer.allocate(size);
//
//    if (sock != null)
//	sbuf.sock = sock;
//    else
//	sock_init(sbuf.sock);
//
////    sbuf.state = state;
//    sbuf.len = 0;
//    sbuf.size = size;
////    sbuf.ptr = sbuf.buf;
////    sbuf.state = state;
//    return 0;
//}
//
//int Sockbuf_cleanup(SocketBuf sbuf)
//{
//    sbuf.buf = null;
//
//    sbuf.size = 0;
//    sbuf.len = 0;
////    sbuf.state = 0;
//    return 0;
//}
//
//int Sockbuf_clear(SocketBuf sbuf)
//{
//    sbuf.len = 0;
//    sbuf.buf.clear();
//    return 0;
//}
////
////int Sockbuf_advance(SocketBuf sbuf, int len)
////{
////    /*
////     * First do a few buffer consistency checks.
////     */
////
////    if (sbuf.len > sbuf.size) {
////	logger.warn("Sockbuf len too far");
////	sbuf.len = sbuf.size;
////    }
////    if (sbuf.len < 0) {
////	logger.warn("Sockbuf len bad");
////	sbuf.len = 0;
////    }
////    if (len <= 0) {
////	if (len < 0)
////	    logger.warn("Sockbuf advance negative ({})", len);
////    }
////    else if (len >= sbuf.len) {
////	if (len > sbuf.len)
////	    logger.warn("Sockbuf advancing too far");
////
////	sbuf.len = 0;
////	sbuf.buf.rewind();
////    } else {
////	memmove(sbuf.buf, sbuf.buf + len, (size_t)(sbuf.len - len));
////	sbuf.len -= len;
////	if (sbuf.ptr - sbuf.buf <= len)
////	    sbuf.ptr = sbuf.buf;
////	else
////	    sbuf.ptr -= len;
////    }
////    return 0;
////}
////
////int Sockbuf_flush(SocketBuf *sbuf)
////{
////    int			len,
////			i;
////
////    if (sbuf.state.get( SOCKBUF_WRITE) == 0) {
////	logger.warn("No flush on non-writable socket buffer");
////	logger.warn("(state=%02x,buf=%08x,ptr=%08x,size={},len={},sock={})",
////	    sbuf.state, sbuf.buf, sbuf.ptr, sbuf.size, sbuf.len,
////	    sbuf.sock);
////	return -1;
////    }
////    if (sbuf.state.get( SOCKBUF_LOCK) != 0) {
////	logger.warn("No flush on locked socket buffer (0x%02x)", sbuf.state);
////	return -1;
////    }
////    if (sbuf.len <= 0) {
////	if (sbuf.len < 0) {
////	    logger.warn("Write socket buffer length negative");
////	    sbuf.len = 0;
////	    sbuf.ptr = sbuf.buf;
////	}
////	return 0;
////    }
////
////#if 0
////    /* maintain a few statistics */
////    {
////	static int		max = 1024, avg, count;
////
////	avg += sbuf.len;
////	count++;
////	if (sbuf.len > max) {
////	    max = sbuf.len;
////	    printf("Max packet = {}, avg = {}\n", max, avg / count);
////	}
////	else if (max > 1024 && (count & 0x03) == 0) {
////	    max--;
////	}
////    }
////#endif
////
////    if (sbuf.state.get( SOCKBUF_DGRAM) != 0) {
////	errno = 0;
////	i = 0;
////#if 0
////	if (randomMT() % 12 == 0)	/* artificial packet loss */
////	    len = sbuf.len;
////	else
////#endif
////	while ((len = sock_write(&sbuf.sock, sbuf.buf, sbuf.len)) <= 0) {
////	    if (len == 0
////		|| errno == EWOULDBLOCK
////		|| errno == EAGAIN) {
////		Sockbuf_clear(sbuf);
////		return 0;
////	    }
////	    if (errno == EINTR) {
////		errno = 0;
////		continue;
////	    }
////#if 0
////	    if (errno == ECONNREFUSED) {
////		error("Send refused");
////		Sockbuf_clear(sbuf);
////		return -1;
////	    }
////#endif
////	    if (++i > MAX_SOCKBUF_RETRIES) {
////		error("Can't send on socket ({},{})", sbuf.sock, sbuf.len);
////		Sockbuf_clear(sbuf);
////		return -1;
////	    }
////	    {
////		static int send_err;
////		if ((send_err++ & 0x3F) == 0)
////		    error("send ({})", i);
////	    }
////	    if (sock_get_error(&sbuf.sock) == -1) {
////		error("sock_get_error send");
////		return -1;
////	    }
////	    errno = 0;
////	}
////	if (len != sbuf.len)
////	    logger.warn("Can't write complete datagram ({},{})", len, sbuf.len);
////
////	Sockbuf_clear(sbuf);
////    } else {
////	errno = 0;
////	while ((len = sock_write(&sbuf.sock, sbuf.buf, sbuf.len)) <= 0) {
////	    if (errno == EINTR) {
////		errno = 0;
////		continue;
////	    }
////	    if (errno != EWOULDBLOCK && errno != EAGAIN) {
////		error("Can't write on socket");
////		return -1;
////	    }
////	    return 0;
////	}
////	Sockbuf_advance(sbuf, len);
////    }
////    return len;
////}
//
//int Sockbuf_write(SocketBuf sbuf, String buf, int len)
//{
//    if (sbuf.state.get( SOCKBUF_WRITE) == 0) {
//	logger.warn("No write to non-writable socket buffer");
//	return -1;
//    }
//    if (sbuf.size - sbuf.len < len) {
//	if (sbuf.state.get( SOCKBUF_LOCK | SOCKBUF_DGRAM) != 0) {
//	    logger.warn("No write to locked socket buffer ({},{},{},{})",
//		sbuf.state, sbuf.size, sbuf.len, len);
//	    return -1;
//	}
//	if (Sockbuf_flush(sbuf) == -1)
//	    return -1;
//
//	if (sbuf.size - sbuf.len < len)
//	    return 0;
//    }
//    memcpy(sbuf.buf + sbuf.len, buf, (size_t)len);
//    sbuf.len += len;
//
//    return len;
//}
//
//int Sockbuf_read(SocketBuf *sbuf)
//{
//    int			max,
//			i,
//			len;
//
//    if (sbuf.state.get( SOCKBUF_READ) == 0) {
//	logger.warn("No read from non-readable socket buffer ({})", sbuf.state);
//	return -1;
//    }
//    if (sbuf.state.get( SOCKBUF_LOCK) != 0)
//	return 0;
//
//    if (sbuf.ptr > sbuf.buf)
//	Sockbuf_advance(sbuf, sbuf.ptr - sbuf.buf);
//
//    if ((max = sbuf.size - sbuf.len) <= 0) {
//	static int before;
//
//	if (before++ == 0)
//	    logger.warn("Read socket buffer not big enough ({},{})",
//		  sbuf.size, sbuf.len);
//	return -1;
//    }
//    if (sbuf.state.get( SOCKBUF_DGRAM) != 0) {
//	errno = 0;
//	i = 0;
//#if 0
//	if (randomMT() % 12 == 0)		/* artificial packet loss */
//	    len = sbuf.len;
//	else
//#endif
//	while ((len = sock_read(&sbuf.sock, sbuf.buf + sbuf.len, max))
//	       <= 0) {
//	    if (len == 0)
//		return 0;
//#ifdef _WINDOWS
//	    errno = WSAGetLastError();
//#endif
//	    if (errno == EINTR) {
//		errno = 0;
//		continue;
//	    }
//	    if (errno == EWOULDBLOCK || errno == EAGAIN) {
//		return 0;
//	    }
//#if 0
//	    if (errno == ECONNREFUSED) {
//		error("Receive refused");
//		return -1;
//	    }
//#endif
///*
//		Trace("errno={} (%s) len = {} during sock_read\n",
//			errno, _GetWSockErrText(errno), len);
//*/
//	    if (++i > MAX_SOCKBUF_RETRIES) {
//		error("Can't recv on socket");
//		return -1;
//	    }
//	    {
//		static int recv_err;
//
//		if ((recv_err++ & 0x3F) == 0)
//		    error("recv ({})", i);
//	    }
//	    if (sock_get_error(&sbuf.sock) == -1) {
//		error("GetSocketError recv");
//		return -1;
//	    }
//	    errno = 0;
//	}
//	sbuf.len += len;
//    } else {
//	errno = 0;
//	while ((len = sock_read(&sbuf.sock, sbuf.buf + sbuf.len, max))
//	       <= 0) {
//	    if (len == 0)
//		return 0;
//
//	    if (errno == EINTR) {
//		errno = 0;
//		continue;
//	    }
//	    if (errno != EWOULDBLOCK && errno != EAGAIN) {
//		error("Can't read on socket");
//		return -1;
//	    }
//	    return 0;
//	}
//	sbuf.len += len;
//    }
//
//    return sbuf.len;
//}
//
//int Sockbuf_copy(SocketBuf *dest, SocketBuf *src, int len)
//{
//    if (len < dest.size - dest.len) {
//	logger.warn("Not enough room in destination copy socket buffer");
//	return -1;
//    }
//    if (len < src.len) {
//	logger.warn("Not enough data in source copy socket buffer");
//	return -1;
//    }
//    memcpy(dest.buf + dest.len, src.buf, (size_t)len);
//    dest.len += len;
//
//    return len;
//}
//
//int Packet_printf(SocketBuf *sbuf, String fmt, ...)
//{
//#define PRINTF_FMT	1
//#define PRINTF_IO	2
//#define PRINTF_SIZE	3
//
//    int			i,
//			cval,
//			ival,
//			count,
//			failure = 0,
//			max_str_size;
//    unsigned		uval;
//    short		sval;
//    unsigned short	usval;
//    long		lval;
//    unsigned long	ulval;
//    char		*str,
//			*end,
//			*buf,
//			*stop;
//    va_list		ap;
//
//    /* kps hack - let's not segfault if sbuf was not initialized yet. */
//    assert(sbuf);
//    if (sbuf.buf == null)
//	return -1;
//    /* kps hack ends */
//
//    va_start(ap, fmt);
//
//    /*
//     * Stream socket buffers should flush the buffer if running
//     * out of write space.  This is currently not needed cause
//     * only datagram sockets are used or the buffer is locked.
//     */
//
//    /*
//     * Mark the end of the available buffer space,
//     * but keep a little room for a terminating packet.
//     * This terminating packet will be from Send_end_of_frame()
//     * in netserver.c.  This is a hack, sorry.
//     * But we want to send frames even if they're bigger than
//     * our available buffer space.
//     */
//    end = sbuf.buf + sbuf.size;
//    if (last_packet_of_frame != 1)
//	end -= SOCKBUF_WRITE_SPARE;
//
//    buf = sbuf.buf + sbuf.len;
//    for (i = 0; failure == 0 && fmt[i] != '\0'; i++) {
//	if (fmt[i] == '%') {
//	    switch (fmt[++i]) {
//	    case 'c':
//		if (buf + 1 >= end) {
//		    failure = PRINTF_SIZE;
//		    break;
//		}
//		cval = va_arg(ap, int);
//		*buf++ = cval;
//		break;
//	    case 'd':
//		if (buf + 4 >= end) {
//		    failure = PRINTF_SIZE;
//		    break;
//		}
//		ival = va_arg(ap, int);
//		*buf++ = ival >> 24;
//		*buf++ = ival >> 16;
//		*buf++ = ival >> 8;
//		*buf++ = ival;
//		break;
//	    case 'u':
//		if (buf + 4 >= end) {
//		    failure = PRINTF_SIZE;
//		    break;
//		}
//		uval = va_arg(ap, unsigned);
//		*buf++ = uval >> 24;
//		*buf++ = uval >> 16;
//		*buf++ = uval >> 8;
//		*buf++ = uval;
//		break;
//	    case 'h':
//		if (buf + 2 >= end) {
//		    failure = PRINTF_SIZE;
//		    break;
//		}
//		switch (fmt[++i]) {
//		case 'd':
//		    sval = va_arg(ap, int);
//		    *buf++ = sval >> 8;
//		    *buf++ = (char)sval;
//		    break;
//		case 'u':
//		    usval = va_arg(ap, unsigned);
//		    *buf++ = usval >> 8;
//		    *buf++ = (char)usval;
//		    break;
//		default:
//		    failure = PRINTF_FMT;
//		    break;
//		}
//		break;
//	    case 'l':
//		if (buf + 4 >= end) {
//		    failure = PRINTF_SIZE;
//		    break;
//		}
//		switch (fmt[++i]) {
//		case 'd':
//		    lval = va_arg(ap, long);
//		    *buf++ = (char)(lval >> 24);
//		    *buf++ = (char)(lval >> 16);
//		    *buf++ = (char)(lval >> 8);
//		    *buf++ = (char)lval;
//		    break;
//		case 'u':
//		    ulval = va_arg(ap, unsigned long);
//		    *buf++ = (char)(ulval >> 24);
//		    *buf++ = (char)(ulval >> 16);
//		    *buf++ = (char)(ulval >> 8);
//		    *buf++ = (char)ulval;
//		    break;
//		default:
//		    failure = PRINTF_FMT;
//		    break;
//		}
//		break;
//	    case 'S':	/* Big strings */
//	    case 's':	/* Small strings */
//		max_str_size = (fmt[i] == 'S') ? MSG_LEN : MAX_CHARS;
//		str = va_arg(ap, String );
//		if (buf + max_str_size >= end)
//		    stop = end;
//		else
//		    stop = buf + max_str_size;
//		/* Send the nul byte too */
//		do {
//		    if (buf >= stop)
//			break;
//		} while ((*buf++ = *str++) != '\0');
//		if (buf > stop)
//		    failure = PRINTF_SIZE;
//		break;
//	    default:
//		failure = PRINTF_FMT;
//		break;
//	    }
//	} else
//	    failure = PRINTF_FMT;
//    }
//    if (failure != 0) {
//	count = -1;
//	if (failure == PRINTF_SIZE) {
//#if 0
//	    static int before;
//	    if ((before++ & 0x0F) == 0)
//		printf("Write socket buffer not big enough ({},{},\"%s\")\n",
//		       sbuf.size, sbuf.len, fmt);
//#endif
//	    if (sbuf.state.get( SOCKBUF_DGRAM) != 0) {
//		count = 0;
//		failure = 0;
//	    }
//	}
//	else if (failure == PRINTF_FMT)
//	    logger.warn("Error in format string (\"%s\")", fmt);
//    } else {
//	count = buf - (sbuf.buf + sbuf.len);
//	sbuf.len += count;
//    }
//
//    va_end(ap);
//
//    return count;
//}
//
//int Packet_scanf(SocketBuf *sbuf, String fmt, ...)
//{
//    int			i,
//			j,
//			k,
//			*iptr,
//			count = 0,
//			failure = 0,
//			max_str_size;
//    unsigned		*uptr;
//    short		*sptr;
//    unsigned short	*usptr;
//    long		*lptr;
//    unsigned long	*ulptr;
//    char		*cptr,
//			*str;
//    va_list		ap;
//
//    va_start(ap, fmt);
//
//    for (i = j = 0; failure == 0 && fmt[i] != '\0'; i++) {
//	if (fmt[i] == '%') {
//	    count++;
//	    switch (fmt[++i]) {
//	    case 'c':
//		if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 1]) {
//		    if (sbuf.state.get( SOCKBUF_DGRAM | SOCKBUF_LOCK) != 0) {
//			failure = 3;
//			break;
//		    }
//		    if (Sockbuf_read(sbuf) == -1) {
//			failure = 2;
//			break;
//		    }
//		    if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 1]) {
//			failure = 3;
//			break;
//		    }
//		}
//		cptr = va_arg(ap, String );
//		*cptr = sbuf.ptr[j++];
//		break;
//	    case 'd':
//		if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 4]) {
//		    if (sbuf.state.get( SOCKBUF_DGRAM | SOCKBUF_LOCK) != 0) {
//			failure = 3;
//			break;
//		    }
//		    if (Sockbuf_read(sbuf) == -1) {
//			failure = 2;
//			break;
//		    }
//		    if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 4]) {
//			failure = 3;
//			break;
//		    }
//		}
//		iptr = va_arg(ap, int *);
//		*iptr = sbuf.ptr[j++] << 24;
//		*iptr |= (sbuf.ptr[j++] & 0xFF) << 16;
//		*iptr |= (sbuf.ptr[j++] & 0xFF) << 8;
//		*iptr |= (sbuf.ptr[j++] & 0xFF);
//		break;
//	    case 'u':
//		if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 4]) {
//		    if (sbuf.state.get( SOCKBUF_DGRAM | SOCKBUF_LOCK) != 0) {
//			failure = 3;
//			break;
//		    }
//		    if (Sockbuf_read(sbuf) == -1) {
//			failure = 2;
//			break;
//		    }
//		    if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 4]) {
//			failure = 3;
//			break;
//		    }
//		}
//		uptr = va_arg(ap, unsigned *);
//		*uptr = (sbuf.ptr[j++] & 0xFF) << 24;
//		*uptr |= (sbuf.ptr[j++] & 0xFF) << 16;
//		*uptr |= (sbuf.ptr[j++] & 0xFF) << 8;
//		*uptr |= (sbuf.ptr[j++] & 0xFF);
//		break;
//	    case 'h':
//		if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 2]) {
//		    if (sbuf.state.get( SOCKBUF_DGRAM | SOCKBUF_LOCK) != 0) {
//			failure = 3;
//			break;
//		    }
//		    if (Sockbuf_read(sbuf) == -1) {
//			failure = 2;
//			break;
//		    }
//		    if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 2]) {
//			failure = 3;
//			break;
//		    }
//		}
//		switch (fmt[++i]) {
//		case 'd':
//		    sptr = va_arg(ap, short *);
//		    *sptr = sbuf.ptr[j++] << 8;
//		    *sptr |= (sbuf.ptr[j++] & 0xFF);
//		    break;
//		case 'u':
//		    usptr = va_arg(ap, unsigned short *);
//		    *usptr = (sbuf.ptr[j++] & 0xFF) << 8;
//		    *usptr |= (sbuf.ptr[j++] & 0xFF);
//		    break;
//		default:
//		    failure = 1;
//		    break;
//		}
//		break;
//	    case 'l':
//		if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 4]) {
//		    if (sbuf.state.get( SOCKBUF_DGRAM | SOCKBUF_LOCK) != 0) {
//			failure = 3;
//			break;
//		    }
//		    if (Sockbuf_read(sbuf) == -1) {
//			failure = 2;
//			break;
//		    }
//		    if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 4]) {
//			failure = 3;
//			break;
//		    }
//		}
//		switch (fmt[++i]) {
//		case 'd':
//		    lptr = va_arg(ap, long *);
//		    *lptr = sbuf.ptr[j++] << 24;
//		    *lptr |= (sbuf.ptr[j++] & 0xFF) << 16;
//		    *lptr |= (sbuf.ptr[j++] & 0xFF) << 8;
//		    *lptr |= (sbuf.ptr[j++] & 0xFF);
//		    break;
//		case 'u':
//		    ulptr = va_arg(ap, unsigned long *);
//		    *ulptr = (sbuf.ptr[j++] & 0xFF) << 24;
//		    *ulptr |= (sbuf.ptr[j++] & 0xFF) << 16;
//		    *ulptr |= (sbuf.ptr[j++] & 0xFF) << 8;
//		    *ulptr |= (sbuf.ptr[j++] & 0xFF);
//		    break;
//		default:
//		    failure = 1;
//		    break;
//		}
//		break;
//	    case 'S':	/* Big strings */
//	    case 's':	/* Small strings */
//		max_str_size = (fmt[i] == 'S') ? MSG_LEN : MAX_CHARS;
//		str = va_arg(ap, String );
//		k = 0;
//		for (;;) {
//		    if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 1]) {
//			if (sbuf.state.get( SOCKBUF_DGRAM | SOCKBUF_LOCK)
//			    != 0) {
//			    failure = 3;
//			    break;
//			}
//			if (Sockbuf_read(sbuf) == -1) {
//			    failure = 2;
//			    break;
//			}
//			if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j + 1]) {
//			    failure = 3;
//			    break;
//			}
//		    }
//		    if ((str[k++] = sbuf.ptr[j++]) == '\0')
//			break;
//		    else if (k >= max_str_size) {
//			/*
//			 * What to do now is unclear to me.
//			 * The server should drop the packet, but
//			 * the client has more difficulty with that
//			 * if this is the reliable data buffer.
//			 */
//			logger.warn("String overflow while scanning ({},{})",
//			      k, max_str_size);
//			if (sbuf.state.get( SOCKBUF_LOCK) != 0)
//			    failure = 2;
//			else
//			    failure = 3;
//			break;
//		    }
//		}
//		if (failure != 0)
//		    strcpy(str, "ErRoR");
//		break;
//	    default:
//		failure = 1;
//		break;
//	    }
//	} else
//	    failure = 1;
//    }
//    if (failure == 1)
//	logger.warn("Error in format string (%s)", fmt);
//    else if (failure == 3) {
//	/* Not enough input for one complete packet */
//	count = 0;
//	failure = 0;
//    }
//    else if (failure == 0) {
//	if (&sbuf.buf[sbuf.len] < &sbuf.ptr[j]) {
//	    logger.warn("Input buffer exceeded (%s)", fmt);
//	    failure = 1;
//	} else
//	    sbuf.ptr += j;
//    }
//
//    va_end(ap);
//
//    return (failure) ? -1 : count;
//}

}