package org.xpilot.client;
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
 * Copyright (C) 2000-2004 Uoti Urpala <uau@users.sourceforge.net>
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
import org.xpilot.client.net.packet.*;
import org.xpilot.common.Net;
import org.xpilot.common.Setup;

import javax.inject.Inject;

import static org.xpilot.common.Const.DEBRIS_TYPES;
import static org.xpilot.common.Packet.*;
import static org.xpilot.common.Setup.SETUP_COMPRESSED;
import static org.xpilot.common.Setup.SETUP_MAP_ORDER_XY;
import static org.xpilot.common.Setup.SETUP_MAP_UNCOMPRESSED;

public class NetClient{


 static Logger logger = LoggerFactory.getLogger(NetClient.class);

public static final int MIN_RECEIVE_WINDOW_SIZE = 1;
public static final int MAX_RECEIVE_WINDOW_SIZE = 4;
public static final int MAX_SUPPORTED_FPS = 255;
static public class Display {
    int view_width;
    int view_height;
    int spark_rand;
    int num_spark_colors;
}

static public class PointerMove{
    int movement;
    double turnspeed;
    int id;
}

public static final int MAX_POINTER_MOVES = 128;




public static final int TALK_RETRY = 2;
 public static final int MAX_MAP_ACK_LEN = 500;
 public static final int KEYBOARD_STORE = 20;
/*
 * Type definitions.
 */
public static class FrameBuf {
    long		loops;
    Net.SocketBuf sbuf;
}

/*
 * Exported variables.
 */
@Inject
Setup			Setup = null;
Display               server_display;
int			receive_window_size = 3;
long			last_loops;
boolean                    packetMeasurement;
PointerMove	[]	pointer_moves = new PointerMove[MAX_POINTER_MOVES];
int			pointer_move_next;
long			last_keyboard_ack;
boolean			dirPrediction;

/*
 * Local variables.
 */
static Net.SocketBuf rbuf,
			cbuf,
			wbuf;
static FrameBuf	Frames;
XPilotPacket []		receive_tbl=new XPilotPacket[256];
	XPilotPacket[]	reliable_tbl=new XPilotPacket[256];
static int		keyboard_delta;
static long		magic;
static long           last_send_anything;
static long		last_keyboard_change,
			last_keyboard_update,
			reliable_offset,
			talk_pending,
			talk_sequence_num,
			talk_last_send;
static long	[]	keyboard_change = new long[KEYBOARD_STORE];
    static long	[]			keyboard_update = new long[KEYBOARD_STORE];
    static long	[]			keyboard_acktime = new long[KEYBOARD_STORE];
String		talk_str;



/*
 * Initialize the function dispatch tables.
 * There are two tables.  One for the semi-important unreliable
 * data like frame updates.
 * The other one is for the reliable data stream, which is
 * received as part of the unreliable data packets.
 */
 void Receive_init()
{
    int i;

    for (i = 0; i < 256; i++) {
	receive_tbl[i] = null;
	reliable_tbl[i] = null;
    }

    receive_tbl[PKT_EYES]	= new EyesAbstractObject();
    receive_tbl[PKT_TIME_LEFT]	= new TimeLeftAbstractObject();
    receive_tbl[PKT_AUDIO]	= new AudioAbstractObject();
    receive_tbl[PKT_START]	= new StartAbstractObject();
    receive_tbl[PKT_END]	= new EndAbstractObject();
    receive_tbl[PKT_SELF]	= new SelfPacket();
    receive_tbl[PKT_DAMAGED]	= new DamagedAbstractObject();
    receive_tbl[PKT_CONNECTOR]	= new ConnectorPacket();
    receive_tbl[PKT_LASER]	= new LaserPacket();
    receive_tbl[PKT_REFUEL]	= new RefuelPacket();
    receive_tbl[PKT_SHIP]	= new ShipPacket();
    receive_tbl[PKT_ECM]	= new ECMPacket();
    receive_tbl[PKT_TRANS]	= new TransAbstractObject();
    receive_tbl[PKT_PAUSED]	= new PausedAbstractObject();
    receive_tbl[PKT_APPEARING]	= new AppearingAbstractObject();
    receive_tbl[PKT_ITEM]	= new ItemAbstractObject();
    receive_tbl[PKT_MINE]	= new MinePacket();
    receive_tbl[PKT_BALL]	= new Ball();
    receive_tbl[PKT_MISSILE]	= new MissilePacket();
    receive_tbl[PKT_SHUTDOWN]	= new ShutdownAbstractObject();
    receive_tbl[PKT_DESTRUCT]	= new DestructAbstractObject();
    receive_tbl[PKT_SELF_ITEMS]	= new SelfItemsAbstractObject();
    receive_tbl[PKT_FUEL]	= new FuelPacket();
    receive_tbl[PKT_CANNON]	= new CannonPacket();
    receive_tbl[PKT_TARGET]	= new TargetAbstractObject();
    receive_tbl[PKT_RADAR]	= new RadarPacket();
    receive_tbl[PKT_FASTRADAR]	= new FastRadarAbstractObject();
    receive_tbl[PKT_RELIABLE]	= Receive_reliable;
    receive_tbl[PKT_QUIT]	= new QuitAbstractObject();
    receive_tbl[PKT_MODIFIERS]  = new ModifiersAbstractObject();
    receive_tbl[PKT_FASTSHOT]	= new FastShotAbstractObject();
    receive_tbl[PKT_THRUSTTIME] = new ThrustTimeAbstractObject();
    receive_tbl[PKT_SHIELDTIME] = new ShieldTimeAbstractObject();
    receive_tbl[PKT_PHASINGTIME]= new PhasingTimeAbstractObject();
    receive_tbl[PKT_ROUNDDELAY] = new RoundDelayAbstractObject();
    receive_tbl[PKT_LOSEITEM]	= new LoseItemAbstractObject();
    receive_tbl[PKT_WRECKAGE]	= new WreckageAbstractObject();
    receive_tbl[PKT_ASTEROID]	= new Asteroid();
    receive_tbl[PKT_WORMHOLE]	= new WormholeAbstractObject();
    receive_tbl[PKT_POLYSTYLE]	= new PolyStylePacket();
    for (i = 0; i < DEBRIS_TYPES; i++) {
        receive_tbl[PKT_DEBRIS + i] = new DebrisPacket();
    }

    reliable_tbl[PKT_MOTD]	= new MOTDAbstractObject();
    reliable_tbl[PKT_MESSAGE]	= new MessageAbstractObject();
    reliable_tbl[PKT_TEAM_SCORE] = new TeamScoreAbstractObject();
    reliable_tbl[PKT_PLAYER]	= new PlayerPacket();
    reliable_tbl[PKT_TEAM]	= new TeamPacket();
    reliable_tbl[PKT_SCORE]	= new ScoreAbstractObject();
    reliable_tbl[PKT_TIMING]	= new TimingAbstractObject();
    reliable_tbl[PKT_LEAVE]	= new LeaveAbstractObject();
    reliable_tbl[PKT_WAR]	= new WarAbstractObject();
    reliable_tbl[PKT_SEEK]	= new SeekPacket();
    reliable_tbl[PKT_BASE]	= new Base();
    reliable_tbl[PKT_QUIT]	= new QuitAbstractObject();
//    reliable_tbl[PKT_STRING]	= Receive_string;
    reliable_tbl[PKT_SCORE_OBJECT] = new ScoreObjectPacket();
    reliable_tbl[PKT_TALK_ACK]	= new TalkAckAbstractObject();
}

/*
 * Receive the map data and some game parameters from
 * the server.  The map data may be in compressed form.
 */
int Net_setup()
{
    //SetupPacket
    //@todo incorporate this retry logic
    if (todo > 0) {
        if (rbuf.ptr != rbuf.buf)
            Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
        if (rbuf.len > 0) {
            if (rbuf.ptr[0] != PKT_RELIABLE) {
                if (rbuf.ptr[0] == PKT_QUIT) {
                    logger.warn("Server closed connection");
                    return -1;
                } else {
                    logger.warn("Not a reliable packet ({}) in setup",
                            rbuf.ptr[0]);
                    return -1;
                }
            }
            if (Receive_reliable() == -1)
                return -1;
            if (Sockbuf_flush(&wbuf) == -1)
            return -1;
        }
        if (cbuf.ptr != cbuf.buf)
            Sockbuf_advance(&cbuf, cbuf.ptr - cbuf.buf);
        if (cbuf.len > 0)
            continue;
        for (retries = 0;; retries++) {
            if (retries >= 10) {
                logger.warn("Can't read setup after {} retries "
                        "(todo={}, left={})",
                        retries, todo, cbuf.len - (cbuf.ptr - cbuf.buf));
                return -1;
            }
            sock_set_timeout(&rbuf.sock, 2, 0);
            while (sock_readable(&rbuf.sock) > 0) {
                Sockbuf_clear(&rbuf);
                if (Sockbuf_read(&rbuf) == -1) {
                    logger.error("Can't read all setup data");
                    return -1;
                }
                if (rbuf.len > 0)
                    break;
                sock_set_timeout(&rbuf.sock, 0, 0);
            }
            if (rbuf.len > 0)
                break;
        }
    }
}
    return 0;
}

/*
 * Send the first packet to the server with our name,
 * nick and display contained in it.
 * The server uses this data to verify that the packet
 * is from the right UDP connection, it already has
 * this info from the ENTER_GAME_pack.
 */
#define	MAX_VERIFY_RETRIES	5
int Net_verify(String user_name, String nick_name, String disp)
{
    int		n,
		type,
		result,
		retries;
    time_t	last;

    for (retries = 0;;) {
	if (retries == 0 || time(null) - last >= 3) {
	    if (retries++ >= MAX_VERIFY_RETRIES) {
		logger.warn("Can't connect to server after {} retries", retries);
		return -1;
	    }
	    Sockbuf_clear(&wbuf);
	    /* IFWINDOWS( Trace("Verifying to sock={}\n", wbuf.sock) ); */
	    n = Packet_printf(&wbuf, "%c{}{}{}", PKT_VERIFY,
			      user_name, nick_name, disp);
	    if (n <= 0 || Sockbuf_flush(&wbuf) <= 0) {
		logger.error("Can't send verify packet");
		return -1;
	    }
	    time(&last);
	    if (retries > 1) {
		printf("Waiting for verify response\n");
		IFWINDOWS( Progress("Waiting for verify response") );
	    }
	}
	sock_set_timeout(&rbuf.sock, 1, 0);
	if (sock_readable(&rbuf.sock) == 0)
	    continue;
	Sockbuf_clear(&rbuf);
	if (Sockbuf_read(&rbuf) == -1) {
	    logger.error("Can't read verify reply packet");
	    return -1;
	}
	if (rbuf.len <= 0)
	    continue;
	if (rbuf.ptr[0] != PKT_RELIABLE) {
	    if (rbuf.ptr[0] == PKT_QUIT) {
		logger.warn("Server closed connection");
		return -1;
	    } else {
		logger.warn("Bad packet type when verifying ({})", rbuf.ptr[0]);
		return -1;
	    }
	}
	if (Receive_reliable() == -1)
	    return -1;
	if (Sockbuf_flush(&wbuf) == -1)
	    return -1;
	if (cbuf.len == 0)
	    continue;
	if (Receive_reply(&type, &result) <= 0) {
	    logger.warn("Can't receive verify reply packet");
	    return -1;
	}
	if (type != PKT_VERIFY) {
	    logger.warn("Verify wrong reply type ({})", type);
	    return -1;
	}
	if (result != PKT_SUCCESS) {
	    logger.warn("Verification failed ({})", result);
	    return -1;
	}
	if (Receive_magic() <= 0) {
	    logger.error("Can't receive magic packet after verify");
	    return -1;
	}
	break;
    }
    if (retries > 1) {
	printf("Verified correctly\n");
	IFWINDOWS( Progress("Verified correctly") );
    }
    return 0;
}

/*
 * Open the datagram socket and allocate the network data
 * structures like buffers.
 * Currently there are three different buffers used:
 * 1) wbuf is used only for sending packets (write/printf).
 * 2) rbuf is used for receiving packets in (read/scanf).
 * 3) cbuf is used to copy the reliable data stream
 *    into from the raw and unreliable rbuf packets.
 */
int Net_init(String server, int port)
{
    int			i;
    size_t		size;
    sock_t		sock;

    assert(server != null);

#ifndef _WINDOWS
    signal(SIGPIPE, SIG_IGN);
#endif

    server_display.view_width = 0;
    server_display.view_height = 0;
    server_display.spark_rand = 0;
    server_display.num_spark_colors = 0;

    Receive_init();
    if (!clientPortStart || !clientPortEnd ||
	(clientPortStart > clientPortEnd)) {
	if (sock_open_udp(&sock, null, 0) == SOCK_IS_ERROR) {
	    logger.error("Cannot create datagram socket ({})", sock.error.error);
	    return -1;
	}
    } else {
	int found_socket = 0;
	for (i = clientPortStart; i <= clientPortEnd; i++) {
	    if (sock_open_udp(&sock, null, i) != SOCK_IS_ERROR) {
		found_socket = 1;
		break;
	    }
	}
	if (found_socket == 0) {
	    logger.error("Could not find a usable port in given port range");
	    return -1;
	}
    }

    if (sock_connect(&sock, server, port) == -1) {
	logger.error("Can't connect to server {} on port {}", server, port);
	sock_close(&sock);
	return -1;
    }
    wbuf.sock = sock;
    if (sock_set_non_blocking(&sock, 1) == -1) {
	logger.error("Can't make socket non-blocking");
	return -1;
    }
    if (sock_set_send_buffer_size(&sock, CLIENT_SEND_SIZE + 256) == -1)
	logger.error("Can't set send buffer size to {}", CLIENT_SEND_SIZE + 256);

    if (sock_set_receive_buffer_size(&sock, CLIENT_RECV_SIZE + 256) == -1)
	logger.error("Can't set receive buffer size to {}", CLIENT_RECV_SIZE + 256);

    size = receive_window_size * sizeof(FrameBuf);
    if ((Frames = (FrameBuf *) malloc(size)) == null) {
	logger.error("No memory (%u)", size);
	return -1;
    }
    for (i = 0; i < receive_window_size; i++) {
	Frames[i].loops = 0;
	if (Sockbuf_init(&Frames[i].sbuf, &sock, CLIENT_RECV_SIZE,
			 SOCKBUF_READ | SOCKBUF_DGRAM) == -1) {
	    logger.error("No memory for read buffer (%u)", CLIENT_RECV_SIZE);
	    return -1;
	}
    }

    /* reliable data buffer, not a valid socket filedescriptor needed */
    if (Sockbuf_init(&cbuf, null, CLIENT_RECV_SIZE,
		     SOCKBUF_WRITE | SOCKBUF_READ | SOCKBUF_LOCK) == -1) {
	logger.error("No memory for control buffer (%u)", CLIENT_RECV_SIZE);
	return -1;
    }

    /* write buffer */
    if (Sockbuf_init(&wbuf, &sock, CLIENT_SEND_SIZE,
		     SOCKBUF_WRITE | SOCKBUF_DGRAM) == -1) {
	logger.error("No memory for write buffer (%u)", CLIENT_SEND_SIZE);
	return -1;
    }

    /* read buffer */
    rbuf = Frames[0].sbuf;

    /* reliable data byte stream offset */
    reliable_offset = 0;

    /* reset talk status */
    talk_sequence_num = 0;
    talk_pending = 0;

    return 0;
}

/*
 * Cleanup all the network buffers and close the datagram socket.
 * Also try to send the server a quit packet if possible.
 * Because this quit packet may get lost we send one at the
 * beginning and one at the end.
 */
void Net_cleanup()
{
    int i;
    sock_t sock = wbuf.sock;
    char ch;

    if (sock.fd > 2) {
	ch = PKT_QUIT;
	if (sock_write(&sock, &ch, 1) != 1) {
	    sock_get_error(&sock);
	    sock_write(&sock, &ch, 1);
	}
	TimeUnit.MICROSECONDS.sleep((unsigned)50*1000);
    }
    if (Frames != null) {
	for (i = 0; i < receive_window_size; i++) {
	    if (Frames[i].sbuf.buf != null)
		Sockbuf_cleanup(&Frames[i].sbuf);
	    else
		break;
	}
	XFREE(Frames);
    }
    Sockbuf_cleanup(&cbuf);
    Sockbuf_cleanup(&wbuf);
    XFREE(Setup);
    if (sock.fd > 2) {
	ch = PKT_QUIT;
	if (sock_write(&sock, &ch, 1) != 1) {
	    sock_get_error(&sock);
	    sock_write(&sock, &ch, 1);
	}
	TimeUnit.MICROSECONDS.sleep((unsigned)50*1000);
	if (sock_write(&sock, &ch, 1) != 1) {
	    sock_get_error(&sock);
	    sock_write(&sock, &ch, 1);
	}
	sock_close(&sock);
    }
    sock_cleanup();
}

/*
 * Calculate a new 'keyboard-changed-id' which the server has to ack.
 */
void Net_key_change()
{
    last_keyboard_change++;
    Key_update();
}

/*
 * Flush the network output buffer if it has some data in it.
 * Called by the main loop before blocking on a select(2) call.
 */
int Net_flush()
{
    if (wbuf.len == 0) {
	wbuf.ptr = wbuf.buf;
	return 0;
    }
    if (last_keyboard_ack != last_keyboard_change &&
	last_keyboard_update != last_loops)
	/*
	 * Since 3.2.10: just call Key_update to add our keyboard vector.
	 * Key_update will call Send_keyboard to flush our buffer.
	 */
	return Key_update();

    Send_talk();
    if (Sockbuf_flush(&wbuf) == -1)
	return -1;
    Sockbuf_clear(&wbuf);
    last_send_anything = time(null);
    return 1;
}

/*
 * Return the socket filedescriptor for use in a select(2) call.
 */
int Net_fd()
{
    return rbuf.sock.fd;
}

/*
 * Try to send a 'start play' packet to the server and get an
 * acknowledgement from the server.  This is called after
 * we have initialized all our other stuff like the user interface
 * and we also have the map already.
 */
int Net_start()
{
    int			retries,
			type,
			result;
    time_t		last;

    for (retries = 0;;) {
	if (retries == 0
	    || (time(null) - last) > 1) {
	    if (retries++ >= 10) {
		logger.warn("Can't start play after {} retries", retries);
		return -1;
	    }
	    Sockbuf_clear(&wbuf);
	    /*
	     * Some networks have trouble receiving big packets.
	     * Therefore we don't transmit the shipshape when
	     * we have had 5 unsuccesful attempts.
	     */
	    if ((retries < 5 && Send_shape(shipShape) == -1)
		|| Packet_printf(&wbuf, "%c", PKT_PLAY) <= 0
		|| Client_power() == -1
#ifdef SOUND
		|| Send_audio_request(1) == -1
#endif
		|| Client_fps_request() == -1
		|| Sockbuf_flush(&wbuf) == -1) {
		logger.error("Can't send start play packet");
		return -1;
	    }
	    time(&last);
	}
	if (cbuf.ptr > cbuf.buf)
	    Sockbuf_advance(&cbuf, cbuf.ptr - cbuf.buf);
	sock_set_timeout(&rbuf.sock, 2, 0);
	while (cbuf.len <= 0
	    && sock_readable(&rbuf.sock) != 0) {
	    Sockbuf_clear(&rbuf);
	    if (Sockbuf_read(&rbuf) == -1) {
		logger.error("Error reading play reply");
		return -1;
	    }
	    if (rbuf.len <= 0)
		continue;
	    if (rbuf.ptr[0] != PKT_RELIABLE) {
		if (rbuf.ptr[0] == PKT_QUIT) {
		    logger.warn("Server closed connection");
		    return -1;
		}
		else if (rbuf.ptr[0] == PKT_START) {
		    /*
		     * Packet out of order, drop it or...
		     * Skip the frame and check for a Reliable Data Packet.
		     * (HACK)
		     * In a future version we may not want a reply to
		     * the PKT_PLAY request and accept frames immediately.
		     */
		    while (++rbuf.ptr < rbuf.buf + rbuf.len) {
			if (rbuf.ptr[0] == PKT_END
			    && rbuf.ptr + 5 < rbuf.buf + rbuf.len
			    && rbuf.ptr[5] == PKT_RELIABLE) {
			    rbuf.ptr += 5;
			    break;
			}
		    }
		    if (rbuf.ptr + 11 >= rbuf.buf + rbuf.len) {
			printf("skipping unexpected frame while starting\n");
			continue;
		    }
		    printf("abusing unexpected frame while starting\n");
		} else {
		    printf("strange packet type while starting ({})\n",
			rbuf.ptr[0]);
		    /*
		     * What the hack do we care when we wanna play.
		     * Just drop the packet for now.
		     */
		    Sockbuf_clear(&rbuf);
		    continue;
		}
	    }
	    if (Receive_reliable() == -1)
		return -1;
	    if (Sockbuf_flush(&wbuf) == -1)
		return -1;
	}
	if (cbuf.ptr - cbuf.buf >= cbuf.len)
	    continue;
	if (cbuf.ptr[0] != PKT_REPLY) {
	    logger.warn("Not a reply packet after play ({},{},{})",
		 cbuf.ptr[0], cbuf.ptr - cbuf.buf, cbuf.len);
	    return -1;
	}
	if (Receive_reply(&type, &result) <= 0) {
	    logger.warn("Can't receive reply packet after play");
	    return -1;
	}
	if (type != PKT_PLAY) {
	    logger.warn("Can't receive reply packet after play");
	    return -1;
	}
	if (result != PKT_SUCCESS) {
	    logger.warn("Start play not allowed ({})", result);
	    return -1;
	}
	break;
    }
    packet_measure = null;
    packetMeasurement = true;
    Net_init_measurement();
    Net_init_lag_measurement();
    errno = 0;
    return 0;
}


void Net_init_measurement()
{
    packet_loss = 0;
    packet_drop = 0;
    packet_loop = 0;
    if (packetMeasurement) {
	if (packet_measure == null) {
	    /*
	     * Server FPS can change so we had better allocate enough.
	     */
	    if ((packet_measure = XMALLOC(char, MAX_SUPPORTED_FPS)) == null) {
		logger.error("No memory for packet measurement");
		packetMeasurement = false;
	    } else
		memset(packet_measure, PACKET_DRAW, MAX_SUPPORTED_FPS);
	}
    }
    else
	XFREE(packet_measure);
}

void Net_init_lag_measurement()
{
    int i;

    packet_lag = 0;
    keyboard_delta = 0;
    for (i = 0; i < KEYBOARD_STORE; i++) {
	keyboard_change[i] = -1;
	keyboard_update[i] = -1;
	keyboard_acktime[i] = -1;
    }
}

/*
 * Process a packet which most likely is a frame update,
 * perhaps with some reliable data in it.
 */
static int Net_packet()
{
    int		type,
		prev_type = 0,
		result,
		replyto,
		status;

    while (rbuf.buf + rbuf.len > rbuf.ptr) {
	type = (*rbuf.ptr & 0xFF);

	if (receive_tbl[type] == null) {
	    logger.warn("Received unknown packet type ({}, {}), "
		 "dropping frame.", type, prev_type);
	    Sockbuf_clear(&rbuf);
	    break;
	}
	else if ((result = (*receive_tbl[type])()) <= 0) {
	    if (result == -1) {
		if (type != PKT_QUIT)
		    logger.warn("Processing packet type ({}, {}) failed",
			 type, prev_type);
		return -1;
	    }
	    /* Drop rest of incomplete packet */
	    Sockbuf_clear(&rbuf);
	    break;
	}
	prev_type = type;
    }
    while (cbuf.buf + cbuf.len > cbuf.ptr) {
	type = (*cbuf.ptr & 0xFF);
	if (type == PKT_REPLY) {
	    if ((result = Receive_reply(&replyto, &status)) <= 0) {
		if (result == 0)
		    break;
		return -1;
	    }
	    /* should do something more appropriate than this with the reply */
	    logger.warn("Got reply packet ({},{})", replyto, status);
	}
	else if (reliable_tbl[type] == null) {
	    int i;

	    logger.warn("Received unknown reliable data packet type ({},{},{})",
		 type, cbuf.ptr - cbuf.buf, cbuf.len);
	    printf("\tdumping buffer for debugging:\n");
	    for (i = 0; i < cbuf.len; i++) {
		printf("%3d", cbuf.buf[i] & 0xFF);
		if (i % 20 == 0)
		    printf("\n");
		else
		    printf(" ");
	    }
	    printf("\n");
	    return -1;
	}
	else if ((result = (*reliable_tbl[type])()) <= 0) {
	    if (result == 0)
		break;
	    return -1;
	}
    }

    return 0;
}

static void Net_keyboard_track()
{
    int i, ind = -1;
    long maxtime = 0;

    /* look for a match for the keyboard change */
    for (i = 0; i < KEYBOARD_STORE; i++) {
	if (keyboard_change[i] == last_keyboard_change)
	    return;
    }

    /* look for a keyboard_change of -1 or an acknowledged change */
    if (ind == -1) {
	for (i = 0; i < KEYBOARD_STORE; i++) {
	    if (keyboard_change[i] == -1 || keyboard_acktime[i] != -1) {
		ind = i;
		break;
	    }
	}
    }

    /* next, look for the oldest update */
    if (ind == -1) {
	for (i = 0; i < KEYBOARD_STORE; i++) {
	    if (keyboard_update[i] > maxtime) {
		ind = i;
		maxtime = keyboard_update[i];
	    }
	}
    }

    if (ind == -1)
	return;

    keyboard_change[ind] = last_keyboard_change;
    keyboard_update[ind] = last_keyboard_update;
    keyboard_acktime[ind] = -1;

#if 0
    printf("T;{};%ld;%ld ", ind, last_keyboard_change, last_keyboard_update);
#endif
}

/*
 * Do some (simple) packet loss/drop measurement
 * the results of which can be drawn on the display.
 * This is mainly for debugging and analysis.
 */
static void Net_measurement(long loop, int status)
{
    int		i;
    long	delta;

    if (packet_measure == null)
	return;
    if ((delta = loop - packet_loop) < 0) {
	/*
	 * Duplicate or out of order.
	 */
	return;
    }
    if (delta >= FPS) {
	if (packet_loop == 0) {
	    packet_loop = loop - (loop % FPS);
	    return;
	}
	packet_loop = loop - (loop % FPS);
	packet_loss = 0;
	packet_drop = 0;
	for (i = 0; i < FPS; i++) {
	    switch (packet_measure[i]) {
	    case PACKET_LOSS:
		packet_loss++;
		continue;
	    case PACKET_DROP:
		packet_drop++;
		break;
	    default:
		/* no drop or loss */
		break;
	    }
	    packet_measure[i] = PACKET_LOSS;
	}
	delta = loop - packet_loop;
   }
   if (packet_measure[(int)delta] != PACKET_DRAW)
       packet_measure[(int)delta] = status;
}

/* Do some lag measurement the results of which can
 * be drawn on the display.  This is mainly for
 * debugging and analysis.
 */
static void Net_lag_measurement(long key_ack)
{
    int		i, num;
    long	sum;

    for (i = 0; i < KEYBOARD_STORE; i++) {
	if (keyboard_change[i] == key_ack
	    && keyboard_acktime[i] == -1) {
	    keyboard_acktime[i] =last_loops - 1;
#if 0
	    printf("A;{};%ld;%ld ",
		   i, keyboard_change[i], keyboard_acktime[i]);
#endif
	    break;
	}
    }

#if 0
    if (i == KEYBOARD_STORE)
	printf("N ");
#endif

    num = 0;
    sum = 0;
    for (i = 0; i < KEYBOARD_STORE; i++) {
	if (keyboard_acktime[i] != -1L) {
	    num++;
	    sum += keyboard_acktime[i] - keyboard_update[i];
	}
    }

    if (num != 0)
	packet_lag = (int) (sum / num);
}

/*
 * Read a packet into one of the input buffers.
 * If it is a frame update then we check to see
 * if it is an old or duplicate one.  If it isn't
 * a new frame then the packet is discarded and
 * we retry to read a packet once more.
 * It's a non-blocking read.
 */
static int Net_read(FrameBuf *frame)
{
    int		n;
    long	loop;
    byte	ch;

    frame.loops = 0;
    for (;;) {
	Sockbuf_clear(&frame.sbuf);
	if (Sockbuf_read(&frame.sbuf) == -1) {
	    logger.error("Net input error");
	    return -1;
	}
	if (frame.sbuf.len <= 0) {
	    Sockbuf_clear(&frame.sbuf);
	    return 0;
	}
	/*IFWINDOWS( Trace("Net_read: read {} bytes type={}\n",
	  frame.sbuf.len, frame.sbuf.ptr[0]) ); */
	if (frame.sbuf.ptr[0] != PKT_START)
	    /*
	     * Don't know which type of packet this is
	     * and if it contains a frame at all (not likely).
	     * It could be a quit packet.
	     */
	    return 1;

	/* Peek at the frame loop number. */
	n = Packet_scanf(&frame.sbuf, "%c%ld", &ch, &loop);
	/*IFWINDOWS( Trace("Net_read: frame # {}\n", loop) );*/
	frame.sbuf.ptr = frame.sbuf.buf;
	if (n <= 0) {
	    if (n == -1) {
		Sockbuf_clear(&frame.sbuf);
		return -1;
	    }
	    continue;
	}
	else if (loop > last_loops) {
	    frame.loops = loop;
	    return 2;
	} else {
	    /*
	     * Packet out of order.  Drop it.
	     * We may have already drawn it if it is duplicate.
	     * Perhaps we should try to extract any reliable data
	     * from it before dropping it.
	     */
	}
    }
    /*IFWINDOWS( Trace("Net_read: wbuf.len={}\n", wbuf.len) );*/
}

/*
 * Read frames from the net until there are no more available.
 * If the server has floaded us with frame updates then we should
 * discard everything except the most recent ones.  The X server
 * may be too slow to keep up with the rate of the XPilot server
 * or there may have been a network hickup if the net is overloaded.
 */
int Net_input()
{
    int		i, j, n;
    int		num_buffered_packets;
    FrameBuf	*frame,
		*last_frame,
		*oldest_frame = &Frames[0],
		tmpframe;
    time_t      time_now;

    for (i = 0; i < receive_window_size; i++) {
	frame = &Frames[i];
	if (!frame)
	    continue;
	if (frame.loops != 0) {
	    /*
	     * Already contains a frame.
	     */
	    if (frame.loops < oldest_frame.loops || oldest_frame.loops == 0)
		oldest_frame = frame;
	}
	else if (frame.sbuf.len > 0 && frame.sbuf.ptr == frame.sbuf.buf) {
	    /*
	     * Contains an unidentifiable packet.
	     * No more input until this one is processed.
	     */
	    break;
	} else {
	    /*
	     * Empty buffer.  Read a frame.
	     */
	    if ((n = Net_read(frame)) <= 0) {
		if (n == 0) {
		    /* No more new packets available. */
		    if (i == 0)
			/* No frames to be processed. */
			return 0;
		    break;
		} else
		    return n;
	    }
	    else if (n == 1) {
		/*
		 * Contains an unidentifiable packet.
		 * No more input until this one is processed.
		 */
		break;
	    } else {
		/*
		 * Check for duplicate packets.
		 */
		for (j = i - 1; j >= 0; j--) {
		    if (frame.loops == Frames[j].loops)
			break;
		}
		if (j >= 0) {
		    /*
		     * Duplicate.  Drop it.
		     */
		    Net_measurement(frame.loops, PACKET_DROP);
		    Sockbuf_clear(&frame.sbuf);
		    frame.loops = 0;
		    i--;	/* correct for for loop increment. */
		    continue;
		}
		if (frame.loops < oldest_frame.loops)
		    oldest_frame = frame;
	    }
	}
	if ((i == receive_window_size - 1 && i > 0)
#if 0
	    || drawPending
	    || (ThreadedDraw &&
		!WaitForSingleObject(dinfo.eventNotDrawing, 0)
		== WAIT_OBJECT_0)
#endif
		) {
	    /*
	     * Drop oldest packet.
	     */
	    if (oldest_frame.loops < frame.loops) {
		/*
		 * Switch buffers to prevent gaps.
		 */
		tmpframe = *oldest_frame;
		*oldest_frame = *frame;
		*frame = tmpframe;
	    }
	    Net_measurement(frame.loops, PACKET_DROP);
	    Sockbuf_clear(&frame.sbuf);
	    frame.loops = 0;
	    oldest_frame = &Frames[0];
	    /*
	     * Reset loop index.
	     */
	    i = -1;	/* correct for for loop increment. */
	    continue;
	}
    }

    /*
     * Find oldest packet.
     */
    last_frame = oldest_frame = &Frames[0];
    num_buffered_packets = 1; /* Could be 0, but returns before using this */
    for (i = 1; i < receive_window_size; i++, last_frame++) {
	frame = &Frames[i];
	if (frame.loops == 0) {
	    if (frame.sbuf.len > 0) {
		/*
		 * This is an unidentifiable packet.
		 * Process it last, because it arrived last.
		 */
		num_buffered_packets++;
		continue;
	    } else
		/*
		 * Empty.  The rest should be empty too,
		 * because we have taken care not to have gaps.
		 */
		break;
	}
	else {
	    num_buffered_packets++;
	    if (frame.loops < oldest_frame.loops
		|| oldest_frame.loops == 0)
		oldest_frame = frame;
	}
    }

    if (oldest_frame.sbuf.len <= 0) {
	/*
	 * Couldn't find a non-empty packet.
	 */
	if (oldest_frame.loops > 0) {
	    logger.warn("bug {},{}", __FILE__, __LINE__);
	    oldest_frame.loops = 0;
	}
	return 0;
    }

    /*
     * Let the packet processing routines know which
     * packet they should process.
     */
    rbuf = oldest_frame.sbuf;

    /*
     * Process the packet.
     */
    n = Net_packet();

    if (last_frame != oldest_frame) {
	/*
	 * Switch buffers to prevent gaps.
	 */
	tmpframe = *oldest_frame;
	*oldest_frame = *last_frame;
	*last_frame = tmpframe;
    }
    Sockbuf_clear(&last_frame.sbuf);
    last_frame.loops = 0;
    rbuf = last_frame.sbuf;

    if (n == -1)
	return -1;

    /*
     * If the server hasn't yet acked our last keyboard change
     * and we haven't updated it in the (previous) current time frame
     * or if we haven't sent anything for a while (keepalive)
     * then we send our current keyboard state.
     */
    time_now = time(null);
    if ((last_keyboard_ack != last_keyboard_change
	    && last_keyboard_update /*+ 1*/ < last_loops)
	|| time_now - last_send_anything > 5) {
	Key_update();
	last_send_anything = time_now;
    } else
	/*
	 * 4.5.4a2: flush if non-empty
	 * This should help speedup the map update speed
	 * for maps with large number of targets or cannons.
	 */
	Net_flush();

    return num_buffered_packets;
}

/*
 * Receive the beginning of a new frame update packet,
 * which contains the loops number.
 */
int Receive_start()
{
    int		n;
    long	loops_num;
    byte	ch;
    long	key_ack;

    if ((n = Packet_scanf(&rbuf,
			  "%c%ld%ld",
			  &ch, &loops_num, &key_ack)) <= 0)
	return n;

    if (last_loops >= loops_num) {
	/*
	 * Packet is duplicate or out of order.
	 */
	Net_measurement(loops_num, PACKET_DROP);
	printf("ignoring frame (%ld)\n", last_loops - loops_num);
	return 0;
    }
    last_loops = loops_num;
    if (key_ack > last_keyboard_ack) {
	if (key_ack > last_keyboard_change) {
	    printf("Premature keyboard ack by server (%ld,%ld,%ld)\n",
		   last_keyboard_change, last_keyboard_ack, key_ack);
	    /*
	     * Packet could be corrupt???
	     * Some blokes turn off checksumming.
	     */
	    return 0;
	}
	else
	    last_keyboard_ack = key_ack;
    }
    Net_lag_measurement(key_ack);
    if ((n = Handle_start(loops_num)) == -1)
	return -1;
    return 1;
}

/*
 * Receive the end of a new frame update packet,
 * which should contain the same loops number
 * as the frame head.  If this terminating packet
 * is missing then the packet is corrupt or incomplete.
 */
int Receive_end()
{
    int		n;
    long	loops_num;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%ld", &ch, &loops_num)) <= 0)
	return n;
    Net_measurement(loops_num, PACKET_DRAW);
    if ((n = Handle_end(loops_num)) == -1)
	return -1;
    return 1;
}

/*
 * Receive a message string.  This currently is rather
 * inefficiently encoded as an ascii string.
 */
int Receive_message()
{
    int		n;
    byte	ch;
    char	msg[MSG_LEN];

    if ((n = Packet_scanf(&cbuf, "%c{}", &ch, msg)) <= 0)
	return n;
    if ((n = Handle_message(msg)) == -1)
	return -1;
    return 1;
}

/*
 * Receive the remaining playing time.
 */
int Receive_time_left()
{
    int		n;
    byte	ch;
    long	sec;

    if ((n = Packet_scanf(&rbuf, "%c%ld", &ch, &sec)) <= 0)
	return n;
    if ((n = Handle_time_left(sec)) == -1)
	return -1;
    return 1;
}

/*
 * Receive the id of the player we get frame updates for (game over mode).
 */
int Receive_eyes()
{
    int			n,
			id;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd", &ch, &id)) <= 0)
	return n;
    if ((n = Handle_eyes(id)) == -1)
	return -1;
    return 1;
}

/*
 * Receive the server MOTD.
 */
int Receive_motd()
{
    byte		ch;
    long		off,
			size;
    short		len;
    int			n;
    char		*cbuf_ptr = cbuf.ptr;

    if ((n = Packet_scanf(&cbuf, "%c%ld%hd%ld",
			  &ch, &off, &len, &size)) <= 0)
	return n;
    if (cbuf.ptr + len > &cbuf.buf[cbuf.len]) {
	cbuf.ptr = cbuf_ptr;
	return 0;
    }
    Handle_motd(off, cbuf.ptr, (int)len, size);
    cbuf.ptr += len;

    return 1;
}

/*
 * Ask the server to send us the server MOTD.
 */
int Net_ask_for_motd(long offset, long maxlen)
{
    if (offset < 0 || maxlen <= 0) {
	logger.warn("Bad motd request (%ld, %ld)", offset, maxlen);
	return -1;
    }
    if (Packet_printf(&wbuf, "%c%ld%ld", PKT_MOTD, offset, maxlen) <= 0) {
	logger.warn("Can't ask motd");
	return -1;
    }

    return 0;
}

/*
 * Receive the packet with counts for all the items.
 * New since pack version 4203.
 */
int Receive_self_items()
{
    unsigned		mask;
    int			i, n;
    byte		ch;
    char		*rbuf_ptr_start = rbuf.ptr;
    byte		num_items[NUM_ITEMS];

    n = Packet_scanf(&rbuf, "%c%u", &ch, &mask);
    if (n <= 0)
	return n;

    memset(num_items, 0, sizeof num_items);
    for (i = 0; mask != 0; i++) {
	if (mask & (1 << i)) {
	    mask ^= (1 << i);
	    if (rbuf.ptr - rbuf.buf < rbuf.len) {
		if (i < NUM_ITEMS)
		    num_items[i] = *rbuf.ptr++;
		else
		    rbuf.ptr++;
	    }
	}
    }
    Handle_self_items(num_items);
    return (rbuf.ptr - rbuf_ptr_start);
}

/*
 * Receive the packet with all player information for the HUD.
 * If this packet is missing from the frame update then the player
 * isn't actively playing, which means he's either damaged, dead,
 * paused or has game over.
 */
int Receive_self()
{
    int		n;
    short	x, y, vx, vy, lockId, lockDist,
		sFuelSum, sFuelMax, sViewWidth, sViewHeight;
    byte	ch, sNumSparkColors, sHeading, sPower, sTurnSpeed,
		sTurnResistance, sNextCheckPoint, lockDir, sAutopilotLight,
		currentTank, sStat;
    byte	num_items[NUM_ITEMS];

    n = Packet_scanf(&rbuf,
		     "%c"
		     "%hd%hd%hd%hd%c"
		     "%c%c%c"
		     "%hd%hd%c%c",
		     &ch,
		     &x, &y, &vx, &vy, &sHeading,
		     &sPower, &sTurnSpeed, &sTurnResistance,
		     &lockId, &lockDist, &lockDir, &sNextCheckPoint);
    if (n <= 0)
	return n;

    memset(num_items, 0, sizeof num_items);

    n = Packet_scanf(&rbuf,
		     "%c%hd%hd"
		     "%hd%hd%c"
		     "%c%c",

		     &currentTank, &sFuelSum, &sFuelMax,
		     &sViewWidth, &sViewHeight, &sNumSparkColors,
		     &sStat, &sAutopilotLight
		     );
    if (n <= 0)
	return n;

    /*
     * These assignments are done here because the server_display
     * structure members are not of the type that Packet_scanf()
     * expects, which breaks things on big endian architectures.
     */
    server_display.view_width = sViewWidth;
    server_display.view_height = sViewHeight;
    LIMIT(server_display.view_width, MIN_VIEW_SIZE, MAX_VIEW_SIZE);
    if (sViewWidth != server_display.view_width)
	logger.warn("unsupported view width from server");
    LIMIT(server_display.view_height, MIN_VIEW_SIZE, MAX_VIEW_SIZE);
    if (sViewHeight != server_display.view_height)
	logger.warn("unsupported view height from server");
    server_display.num_spark_colors = sNumSparkColors;

    Handle_self(x, y, vx, vy, sHeading,
		(double) sPower,
		(double) sTurnSpeed,
		(double) sTurnResistance / 255.0,
		lockId, lockDist, lockDir,
		sNextCheckPoint, sAutopilotLight,
		num_items,
		currentTank, (double)sFuelSum, (double)sFuelMax, rbuf.len,
		(int)sStat);

    return 1;
}

int Receive_modifiers()
{
    int		n;
    char	sMods[MAX_CHARS];
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c{}", &ch, sMods)) <= 0)
	return n;
    if ((n = Handle_modifiers(sMods)) == -1)
	return -1;
    return 1;
}

int Receive_refuel()
{
    int		n;
    short	x_0, y_0, x_1, y_1;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd%hd",
			  &ch, &x_0, &y_0, &x_1, &y_1)) <= 0)
	return n;
    if ((n = Handle_refuel(x_0, y_0, x_1, y_1)) == -1)
	return -1;
    return 1;
}

int Receive_connector()
{
    int		n;
    short	x_0, y_0, x_1, y_1;
    byte	ch, tractor;

    n = Packet_scanf(&rbuf, "%c%hd%hd%hd%hd%c",
		     &ch, &x_0, &y_0, &x_1, &y_1, &tractor);
    if (n <= 0)
	return n;
    if ((n = Handle_connector(x_0, y_0, x_1, y_1, tractor)) == -1)
	return -1;
    return 1;
}

int Receive_laser()
{
    int		n;
    short	x, y, len;
    byte	ch, color, dir;

    if ((n = Packet_scanf(&rbuf, "%c%c%hd%hd%hd%c",
			  &ch, &color, &x, &y, &len, &dir)) <= 0)
	return n;
    if ((n = Handle_laser(color, x, y, len, dir)) == -1)
	return -1;
    return 1;
}

int Receive_missile()
{
    int		n;
    short	x, y;
    byte	ch, dir, len;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%c%c", &ch, &x, &y, &len, &dir))
	<= 0)
	return n;

    if ((n = Handle_missile(x, y, len, dir)) == -1)
	return -1;
    return 1;
}

int Receive_ball()
{
    int		n;
    short	x, y, id;
    byte	ch, style = 0xff /* no style */;

    if (version < 0x4F14) {
	if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd", &ch, &x, &y, &id)) <= 0)
	    return n;
    } else {
	if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd%c", &ch, &x, &y, &id,
			      &style)) <= 0)
	    return n;
    }
    if ((n = Handle_ball(x, y, id, style)) == -1)
	return -1;
    return 1;
}

int Receive_ship()
{
    int		n, shield, cloak, eshield, phased, deflector;
    short	x, y, id;
    byte	ch, dir, flags;

    if ((n = Packet_scanf(&rbuf,
			  "%c%hd%hd%hd" "%c%c",
			  &ch, &x, &y, &id,
			  &dir, &flags)) <= 0)
	return n;
    shield = ((flags & 1) != 0);
    cloak = ((flags & 2) != 0);
    eshield = ((flags & 4) != 0);
    phased = ((flags & 8) != 0);
    deflector = ((flags & 0x10) != 0);

    if ((n = Handle_ship(x, y, id, dir, shield,
			 cloak, eshield, phased, deflector)) == -1)
	return -1;
    return 1;
}

int Receive_mine()
{
    int		n;
    short	x, y, id;
    byte	ch, teammine;

    n = Packet_scanf(&rbuf, "%c%hd%hd%c%hd", &ch, &x, &y, &teammine, &id);
    if (n <= 0)
	return n;
    if ((n = Handle_mine(x, y, teammine, id)) == -1)
	return -1;
    return 1;
}

int Receive_item()
{
    int		n;
    short	x, y;
    byte	ch, type;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%c", &ch, &x, &y, &type)) <= 0)
	return n;
    if (type < NUM_ITEMS) {
	if ((n = Handle_item(x, y, type)) == -1)
	    return -1;
    }
    return 1;
}

int Receive_destruct()
{
    int		n;
    short	count;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd", &ch, &count)) <= 0)
	return n;
    if ((n = Handle_destruct(count)) == -1)
	return -1;
    return 1;
}

int Receive_shutdown()
{
    int		n;
    short	count, delay;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd", &ch, &count, &delay)) <= 0)
	return n;
    if ((n = Handle_shutdown(count, delay)) == -1)
	return -1;
    return 1;
}

int Receive_thrusttime()
{
    int		n;
    short	count, max;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd", &ch, &count, &max)) <= 0)
	return n;
    if ((n = Handle_thrusttime(count, max)) == -1)
	return -1;
    return 1;
}

int Receive_shieldtime()
{
    int		n;
    short	count, max;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd", &ch, &count, &max)) <= 0)
	return n;
    if ((n = Handle_shieldtime(count, max)) == -1)
	return -1;
    return 1;
}

int Receive_phasingtime()
{
    int		n;
    short	count, max;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd", &ch, &count, &max)) <= 0)
	return n;
    if ((n = Handle_phasingtime(count, max)) == -1)
	return -1;
    return 1;
}

int Receive_rounddelay()
{
    int		n;
    short	count, max;
    byte	ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd", &ch, &count, &max)) <= 0)
	return n;
    if ((n = Handle_rounddelay(count, max)) == -1)
	return -1;
    return 1;
}

int Receive_fastshot()
{
    int			n, r, type;

    rbuf.ptr++;	/* skip PKT_FASTSHOT packet id */

    if (rbuf.ptr - rbuf.buf + 2 >= rbuf.len)
	return 0;
    type = (*rbuf.ptr++ & 0xFF);
    n = (*rbuf.ptr++ & 0xFF);
    if (rbuf.ptr - rbuf.buf + (n * 2) > rbuf.len)
	return 0;
    r = Handle_fastshot(type, (byte*)rbuf.ptr, n);
    rbuf.ptr += n * 2;

    return (r == -1) ? -1 : 1;
}

int Receive_debris()
{
    int			n, r, type;

    if (rbuf.ptr - rbuf.buf + 2 >= rbuf.len)
	return 0;
    type = (*rbuf.ptr++ & 0xFF);
    n = (*rbuf.ptr++ & 0xFF);
    if (rbuf.ptr - rbuf.buf + (n * 2) > rbuf.len)
	return 0;
    r = Handle_debris(type - PKT_DEBRIS, (byte*)rbuf.ptr, n);
    rbuf.ptr += n * 2;

    return (r == -1) ? -1 : 1;
}

int Receive_wreckage()	/* since 3.8.0 */
{
    int			n;
    short		x, y;
    byte		ch, wrecktype, size, rot;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%c%c%c", &ch, &x, &y,
			  &wrecktype, &size, &rot)) <= 0)
	return n;
    if ((n = Handle_wreckage(x, y, wrecktype, size, rot)) == -1)
	return -1;
    return 1;
}

int Receive_asteroid()	/* since 4.4.0 */
{
    int			n;
    short		x, y;
    byte		ch, type_size, type, size, rot;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%c%c", &ch, &x, &y,
			  &type_size, &rot)) <= 0)
	return n;

    type = ((type_size >> 4) & 0x0F);
    size = (type_size & 0x0F);

    if ((n = Handle_asteroid(x, y, type, size, rot)) == -1)
	return -1;
    return 1;
}

int Receive_wormhole()	/* since 4.5.0 */
{
    int			n;
    short		x, y;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd", &ch, &x, &y)) <= 0)
	return n;

    if ((n = Handle_wormhole(x, y)) == -1)
	return -1;
    return 1;
}

int Receive_ecm()
{
    int			n;
    short		x, y, size;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd", &ch, &x, &y, &size)) <= 0)
	return n;
    if ((n = Handle_ecm(x, y, size)) == -1)
	return -1;
    return 1;
}

int Receive_trans()
{
    int			n;
    short		x_1, y_1, x_2, y_2;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd%hd",
			  &ch, &x_1, &y_1, &x_2, &y_2)) <= 0)
	return n;
    if ((n = Handle_trans(x_1, y_1, x_2, y_2)) == -1)
	return -1;
    return 1;
}

int Receive_paused()
{
    int			n;
    short		x, y, count;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd", &ch, &x, &y, &count)) <= 0)
	return n;
    if ((n = Handle_paused(x, y, count)) == -1)
	return -1;
    return 1;
}

int Receive_appearing()
{
    int			n;
    short		x, y, id, count;
    byte		ch;
    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%hd%hd", &ch, &x, &y, &id,
			  &count)) <= 0)
	return n;
    if ((n = Handle_appearing(x, y, id, count)) == -1)
	return -1;
    return 1;
}

int Receive_radar()
{
    int			n;
    short		x, y;
    byte		ch, size;

    if ((n = Packet_scanf(&rbuf, "%c%hd%hd%c", &ch, &x, &y, &size)) <= 0)
	return n;

    if ((n = Handle_radar(x, y, size)) == -1)
	return -1;
    return 1;
}

int Receive_fastradar()
{
    int			n, i, r = 1;
    int			x, y, size;
    unsigned char	*ptr;

    rbuf.ptr++;	/* skip PKT_FASTRADAR packet id */

    if (rbuf.ptr - rbuf.buf >= rbuf.len)
	return 0;
    n = (*rbuf.ptr++ & 0xFF);
    if (rbuf.ptr - rbuf.buf + (n * 3) > rbuf.len)
	return 0;
    ptr = (unsigned String ) rbuf.ptr;
    for (i = 0; i < n; i++) {
	x = *ptr++;
	y = *ptr++;
	y |= (*ptr & 0xC0) << 2;
	size = (*ptr & 0x07);
	if (*ptr & 0x20)
	    size |= 0x80;
	ptr++;
	r = Handle_fastradar(x, y, size);
	if (r == -1)
	    break;
    }
    rbuf.ptr += n * 3;

    return (r == -1) ? -1 : 1;
}

int Receive_damaged()
{
    int			n;
    byte		ch, dmgd;

    if ((n = Packet_scanf(&rbuf, "%c%c", &ch, &dmgd)) <= 0)
	return n;
    if ((n = Handle_damaged(dmgd)) == -1)
	return -1;
    return 1;
}

int Receive_leave()
{
    int			n;
    short		id;
    byte		ch;

    if ((n = Packet_scanf(&cbuf, "%c%hd", &ch, &id)) <= 0)
	return n;
    if ((n = Handle_leave(id)) == -1)
	return -1;
    return 1;
}

int Receive_war()
{
    int			n;
    short		robot_id, killer_id;
    byte		ch;

    if ((n = Packet_scanf(&cbuf, "%c%hd%hd",
			  &ch, &robot_id, &killer_id)) <= 0)
	return n;
    /* not interested */
    return 1;
}

int Receive_seek()
{
    int			n;
    short		programmer_id, robot_id, sought_id;
    byte		ch;

    if ((n = Packet_scanf(&cbuf, "%c%hd%hd%hd", &ch,
			  &programmer_id, &robot_id, &sought_id)) <= 0)
	return n;
    /* not interested */
    return 1;
}

int Receive_player()
{
    int			n;
    short		id;
    byte		ch, myteam, mychar, myself = 0;
    char		nick_name[MAX_CHARS],
			user_name[MAX_CHARS],
			host_name[MAX_CHARS],
			shape[2*MSG_LEN],
			*cbuf_ptr = cbuf.ptr;

    if ((n = Packet_scanf(&cbuf,
			  "%c%hd%c%c" "{}{}{}" "{}",
			  &ch, &id, &myteam, &mychar,
			  nick_name, user_name, host_name,
			  shape)) <= 0)
	return n;
    nick_name[MAX_NAME_LEN - 1] = '\0';
    user_name[MAX_NAME_LEN - 1] = '\0';
    host_name[MAX_HOST_LEN - 1] = '\0';

    if (version < 0x4F10)
	n = Packet_scanf(&cbuf, "{}", &shape[strlen(shape)]);
    else
	n = Packet_scanf(&cbuf, "{}%c", &shape[strlen(shape)], &myself);
    if (n <= 0) {
	cbuf.ptr = cbuf_ptr;
	return n;
    }

    if ((n = Handle_player(id, myteam, mychar, nick_name, user_name, host_name,
			   shape, myself)) == -1)
	return -1;
    return 1;
}

int Receive_team()
{
    int		n;
    short	id;
    byte	ch, pl_team;

    if ((n = Packet_scanf(&cbuf, "%c%hd%c", &ch, &id, &pl_team)) <= 0)
	return n;
    if (Handle_team(id, pl_team) == -1)
	return -1;
    return 1;
}

int Receive_score_object()
{
    int			n;
    unsigned short	x, y;
    double		score = 0;
    char		msg[MAX_CHARS];
    byte		ch;

    if (version < 0x4500 || (version >= 0x4F09 && version < 0x4F11)) {
	short	rcv_score;
	n = Packet_scanf(&cbuf, "%c%hd%hu%hu{}",
			 &ch, &rcv_score, &x, &y, msg);
	score = rcv_score;
    } else {
	/* newer servers send scores with two decimals */
	int	rcv_score;
	n = Packet_scanf(&cbuf, "%c{}%hu%hu{}",
			 &ch, &rcv_score, &x, &y, msg);
	score = (double)rcv_score / 100;
    }
    if (n <= 0)
	return n;
    if ((n = Handle_score_object(score, x, y, msg)) == -1)
	return -1;

    return 1;
}

int Receive_score()
{
    int			n;
    short		id, life;
    double		score = 0;
    byte		ch, mychar, alliance = ' ';

    if (version < 0x4500 || (version >= 0x4F09 && version < 0x4F11)) {
	short	rcv_score;
	n = Packet_scanf(&cbuf, "%c%hd%hd%hd%c", &ch,
			 &id, &rcv_score, &life, &mychar);
	score = rcv_score;
	alliance = ' ';
    } else {
	/* newer servers send scores with two decimals */
	int	rcv_score;
	n = Packet_scanf(&cbuf, "%c%hd{}%hd%c%c", &ch,
			 &id, &rcv_score, &life, &mychar, &alliance);
	score = (double)rcv_score / 100;
    }
    if (n <= 0)
	return n;
    if ((n = Handle_score(id, score, life, mychar, alliance)) == -1)
	return -1;
    return 1;
}

int Receive_team_score()
{
    int			n;
    byte		ch;
    short		team;
    int			rcv_score;
    double		score;

    if ((n = Packet_scanf(&cbuf, "%c%hd{}", &ch, &team, &rcv_score)) <= 0)
	return n;
    score = (double)rcv_score / 100;
    if ((n = Handle_team_score(team, score)) == -1)
	return -1;
    return 1;
}

int Receive_timing()
{
    int			n,
			check,
			round;
    short		id;
    unsigned short	timing;
    byte		ch;

    n = Packet_scanf(&cbuf, "%c%hd%hu", &ch, &id, &timing);
    if (n <= 0)
	return n;
    check = timing % checks.size();
    round = timing / checks.size();
    if ((n = Handle_timing(id, check, round, last_loops)) == -1)
	return -1;
    return 1;
}

int Receive_fuel()
{
    int			n;
    unsigned short	num, fuel;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hu%hu", &ch, &num, &fuel)) <= 0)
	return n;
    if ((n = Handle_fuel(num, (double)fuel)) == -1)
	return -1;
    if (wbuf.len < MAX_MAP_ACK_LEN)
	Packet_printf(&wbuf, "%c%ld%hu", PKT_ACK_FUEL, last_loops, num);
    return 1;
}

int Receive_cannon()
{
    int			n;
    unsigned short	num, dead_time;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hu%hu", &ch, &num, &dead_time)) <= 0)
	return n;
    if ((n = Handle_cannon(num, dead_time)) == -1)
	return -1;
    if (wbuf.len < MAX_MAP_ACK_LEN)
	Packet_printf(&wbuf, "%c%ld%hu", PKT_ACK_CANNON, last_loops, num);
    return 1;
}

int Receive_target()
{
    int			n;
    unsigned short	num,
			dead_time,
			damage;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hu%hu%hu", &ch,
			  &num, &dead_time, &damage)) <= 0)
	return n;
    if ((n = Handle_target(num, dead_time, (double)damage / 256.0)) == -1)
	return -1;
    if (wbuf.len < MAX_MAP_ACK_LEN)
	Packet_printf(&wbuf, "%c%ld%hu", PKT_ACK_TARGET, last_loops, num);
    return 1;
}

int Receive_polystyle()	/* since ng 4.7.0 */
{
    int			n;
    unsigned short	num, newstyle;
    byte		ch;

    if ((n = Packet_scanf(&rbuf, "%c%hu%hu", &ch, &num, &newstyle)) <= 0)
	return n;
    if ((n = Handle_polystyle(num, newstyle)) == -1)
	return -1;
    if (wbuf.len < MAX_MAP_ACK_LEN)
	Packet_printf(&wbuf, "%c%ld%hu", PKT_ACK_POLYSTYLE, last_loops, num);
    return 1;
}

int Receive_base()
{
    int			n;
    short		id;
    unsigned short	num;
    byte		ch;

    if ((n = Packet_scanf(&cbuf, "%c%hd%hu", &ch, &id, &num)) <= 0)
	return n;
    if ((n = Handle_base(id, num)) == -1)
	return -1;
    return 1;
}

int Receive_magic()
{
    int			n;
    byte		ch;

    if ((n = Packet_scanf(&cbuf, "%c%u", &ch, &magic)) <= 0)
	return n;
    return 1;
}

int Receive_string()
{
    int			n;
    byte		ch,
			type;
    unsigned short	arg1,
			arg2;

    if ((n = Packet_scanf(&cbuf, "%c%c%hu%hu", &ch, &type, &arg1, &arg2)) <= 0)
	return n;
    /*
     * Not implemented yet.
     */
    return 1;
}

int Receive_loseitem()
{
    int		n;
    byte	pkt;
				/* Most of the Receive_ funcs call a */
				/* Handle_ func but that seems */
				/* unecessary here */
    if ((n = Packet_scanf(&rbuf, "%c%c", &pkt, &lose_item)) <= 0)
	return n;
    return 1;
}

int Send_ack(long rel_loops)
{
    int			n;

    if ((n = Packet_printf(&wbuf, "%c%ld%ld", PKT_ACK,
			   reliable_offset, rel_loops)) <= 0) {
	if (n == 0)
	    return 0;
	logger.error("Can't ack reliable data");
	return -1;
    }
    return 1;
}

int Receive_reliable()
{
    int			n;
    short		len;
    byte		ch;
    long		rel,
			rel_loops;

    if ((n = Packet_scanf(&rbuf, "%c%hd%ld%ld",
			  &ch, &len, &rel, &rel_loops)) == -1)
	return -1;
    if (n == 0) {
	logger.warn("Incomplete reliable data packet");
	return 0;
    }
#ifdef DEBUG
    if (reliable_offset >= rel + len)
	printf("Reliable my=%ld pkt=%ld len={} loops=%ld\n",
	       reliable_offset, rel, len, rel_loops);
#endif
    if (len <= 0) {
	logger.warn("Bad reliable data length ({})", len);
	return -1;
    }
    if (rbuf.ptr + len > rbuf.buf + rbuf.len) {
	logger.warn("Not all reliable data in packet ({},{},{})",
	     rbuf.ptr - rbuf.buf, len, rbuf.len);
	rbuf.ptr += len;
	Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
	return -1;
    }
    if (rel > reliable_offset) {
	/*
	 * We miss one or more packets.
	 * For now we drop this packet.
	 * We could have kept it until the missing packet(s) arrived.
	 */
	rbuf.ptr += len;
	Sockbuf_advance(&rbuf, rbuf.ptr - rbuf.buf);
	if (Send_ack(rel_loops) == -1)
	    return -1;
	return 1;
    }
    if (rel + len <= reliable_offset) {
	/*
	 * Duplicate data.  Probably an ack got lost.
	 * Send an ack for our current stream position.
	 */
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
	logger.warn("Can't copy reliable data to buffer");
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

int Receive_reply(int *replyto, int *result)
{
    int		n;
    byte	type, ch1, ch2;

    n = Packet_scanf(&cbuf, "%c%c%c", &type, &ch1, &ch2);
    if (n <= 0)
	return n;
    if (n != 3 || type != PKT_REPLY) {
	logger.error("Can't receive reply packet");
	return -1;
    }
    *replyto = ch1;
    *result = ch2;
    return 1;
}

int Send_keyboard(byte *keyboard_vector)
{
    int		size = KEYBOARD_SIZE;

    if (wbuf.size - wbuf.len < size + 1 + 4)
	/* Not enough write buffer space for keyboard state */
	return 0;
    Packet_printf(&wbuf, "%c%ld", PKT_KEYBOARD, last_keyboard_change);
    memcpy(&wbuf.buf[wbuf.len], keyboard_vector, (size_t)size);
    wbuf.len += size;
    last_keyboard_update = last_loops;
    Net_keyboard_track();
    Send_talk();
    if (Sockbuf_flush(&wbuf) == -1) {
	logger.error("Can't send keyboard update");
	return -1;
    }

    return 0;
}

int Send_shape(String str)
{
    ShipShape 		*w;
    char		buf[MSG_LEN], ext[MSG_LEN];

    w = Convert_shape_str(str);
    Convert_ship_2_string(w, buf, ext, 0x3200);
    Free_ship_shape(w);
    if (Packet_printf(&wbuf, "%c{}", PKT_SHAPE, buf) <= 0)
	return -1;
    if (Packet_printf(&wbuf, "{}", ext) <= 0)
	return -1;
    return 0;
}

int Send_power(double pwr)
{
    if (Packet_printf(&wbuf, "%c%hd", PKT_POWER,
		      (int) (pwr * 256.0)) == -1)
	return -1;
    return 0;
}

int Send_power_s(double pwr_s)
{
    if (Packet_printf(&wbuf, "%c%hd", PKT_POWER_S,
		      (int)(pwr_s * 256.0)) == -1)
	return -1;
    return 0;
}

int Send_turnspeed(double turnspd)
{
    if (Packet_printf(&wbuf, "%c%hd", PKT_TURNSPEED,
		      (int) (turnspd * 256.0)) == -1)
	return -1;
    return 0;
}

int Send_turnspeed_s(double turnspd_s)
{
    if (Packet_printf(&wbuf, "%c%hd", PKT_TURNSPEED_S,
		      (int) (turnspd_s * 256.0)) == -1)
	return -1;
    return 0;
}

int Send_turnresistance(double turnres)
{
    if (Packet_printf(&wbuf, "%c%hd", PKT_TURNRESISTANCE,
		      (int) (turnres * 256.0)) == -1)
	return -1;
    return 0;
}

int Send_turnresistance_s(double turnres_s)
{
    if (Packet_printf(&wbuf, "%c%hd", PKT_TURNRESISTANCE_S,
		      (int) (turnres_s * 256.0)) == -1)
	return -1;
    return 0;
}

int Receive_quit()
{
    unsigned char	pkt;
    SocketBuf		*sbuf;
    char		reason[MAX_CHARS];

    if (rbuf.ptr < rbuf.buf + rbuf.len)
	sbuf = &rbuf;
    else
	sbuf = &cbuf;
    if (Packet_scanf(sbuf, "%c", &pkt) != 1)
	logger.warn("Can't read quit packet");
    else {
	if (Packet_scanf(sbuf, "{}", reason) <= 0)
	    strlcpy(reason, "unknown reason", MAX_CHARS);
	logger.warn("Got quit packet: \"{}\"", reason);
    }
    return -1;
}


int Receive_audio()
{
    int			n;
    unsigned char	pkt, type, vol;

    if ((n = Packet_scanf(&rbuf, "%c%c%c", &pkt, &type, &vol)) <= 0)
	return n;
#ifdef SOUND
    if ((n = Handle_audio(type, vol)) == -1)
	return -1;
#endif /* SOUND */
    return 1;
}


int Receive_talk_ack()
{
    int			n;
    unsigned char	pkt;
    long		talk_ack;

    if ((n = Packet_scanf(&cbuf, "%c%ld", &pkt, &talk_ack)) <= 0)
	return n;
    if (talk_ack >= talk_pending)
	talk_pending = 0;
    return 1;
}


int Net_talk(String str)
{
    strlcpy(talk_str, str, sizeof talk_str);
    if (talk_str[0] == '\\')	/* it's a clientcommand! */
	executeCommand(talk_str + 1);
    else {
	talk_pending = ++talk_sequence_num;
	talk_last_send = last_loops - TALK_RETRY;
	Send_talk();
    }
    return 0;
}


int Send_talk()
{
    if (talk_pending == 0)
	return 0;
    if (last_loops - talk_last_send < TALK_RETRY)
	return 0;
    if (Packet_printf(&wbuf, "%c%ld{}", PKT_TALK, talk_pending, talk_str) ==-1)
	return -1;
    talk_last_send = last_loops;
    return 0;
}


int Send_display(int width, int height, int sparks, int spark_colors)
{
    int	width_wanted = width;
    int	height_wanted = height;

    if (width_wanted == server_display.view_width &&
	height_wanted == server_display.view_height &&
	spark_colors == server_display.num_spark_colors &&
	sparks == server_display.spark_rand &&
	last_loops != 0) {
	return 0;
    }

    if (Packet_printf(&wbuf, "%c%hd%hd%c%c", PKT_DISPLAY,
		      width_wanted, height_wanted,
		      spark_colors,
		      sparks) == -1)
	return -1;

    server_display.spark_rand = sparks;

    return 0;
}


int Send_modifier_bank(int bank)
{
    if (bank < 0 || bank >= NUM_MODBANKS)
	return -1;
    if (Packet_printf(&wbuf, "%c%c{}", PKT_MODIFIERBANK,
		      bank, modBankStr[bank]) == -1)
	return -1;
    return 0;
}

int Send_pointer_move(int movement)
{
    static int total;

#if 0
    struct timeval tv;
    static struct timeval old_tv;
    double s, u, t;
    static double oldt = 0;
    static int num = 1;

    gettimeofday(&tv, null);

    s = tv.tv_sec;
    u = tv.tv_usec;
    t = s + u * 1e-6;

    if (tv.tv_sec != old_tv.tv_sec) {
	logger.warn("Send_pointer_moves = {}", num);
	num = 1;
    } else
	num ++;

    /*logger.warn("{} %.2f: {}", num, t - oldt, movement);*/

    oldt = t;
    old_tv = tv;
#endif

    if (dirPrediction) {
	pointer_moves[pointer_move_next].movement = movement;
	pointer_moves[pointer_move_next].turnspeed = turnspeed;
	pointer_moves[pointer_move_next].id = last_keyboard_change + 1;
	
        pointer_move_next++;
	if (pointer_move_next >= MAX_POINTER_MOVES)
	    pointer_move_next = 0;
    }

    if (version >= 0x4F13) {
	total += movement;
	movement = total;
    }

    if (Packet_printf(&wbuf, "%c%hd", PKT_POINTER_MOVE, movement) == -1)
	return -1;
    
    if (dirPrediction)
	Net_key_change();	
    
    return 0;
}

int Send_audio_request(int on)
{
#ifdef DEBUG_SOUND
    printf("Send_audio_request {}\n", on);
#endif

#ifndef SOUND
    on = 0;
#endif
    if (Packet_printf(&wbuf, "%c%c", PKT_REQUEST_AUDIO, (on != 0)) == -1)
	return -1;
    return 0;
}

int Send_fps_request(int fps)
{
    assert(fps > 0);
    assert(fps <= MAX_SUPPORTED_FPS);
    if (Packet_printf(&wbuf, "%c%c", PKT_ASYNC_FPS, fps) == -1)
	return -1;
    return 0;
}
