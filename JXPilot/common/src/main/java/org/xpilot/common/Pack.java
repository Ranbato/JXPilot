package org.xpilot.common;
/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 2000-2004 Uoti Urpala <uau@users.sourceforge.net>
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

public class Pack {

public char CAP_LETTER(char c) {return Character.toUpperCase(c);}

public static final int SERVER_PORT = 15345;		/* Port which server listens to. */

/*
 * Magic contact word.
 * The low 16 bits are the real magic word.
 * Bits 31-28 are the major version number.
 * Bits 27-24 are the minor version number.
 * Bits 23-20 are the patchlevel number.
 * Bits 19-16 are free to mean beta release or so.
 * These high bits only need to be changed when a new
 * client can't talk to an old server or vise versa.
 * Please don't change it more often than strictly necessary.
 * PLEASE NOTE: if you make your own private incompatible changes
 * justifying an increment of the version word then please
 * set the 4th bit.  Like 0x3108 and 0x3109 till 0x310F, etc.
 * This way we can use the lower 8 values for public releases.
 *
 * Reasons why it changed in the past:
 * 3.0.1: rewrite of contact pack protocol, because of
 * different structure layout rules on different architectures.
 * 3.0.2: rewrite of setup transmit from server to client to
 * make it possible for 64-bit machines and 32-bit machines
 * to join in the same game.  This was the last hardcoded
 * structure that was shared between client and server.
 * 3.0.3: implemented a version awareness system, so that
 * newer clients can join older servers and so that
 * newer servers can support older clients.
 * The client maintains a 'version' variable indicating
 * the version of the server it has joined and the server
 * maintains for each connection a 'connection_t.version'
 * and a 'player.version' variable.
 * 3.0.4: the so-called 'pizza-mode' introduced a new packet type.
 * The score packet now also includes pl.mychar.
 * 3.0.4.1: new laser weapon introduces another packet change.
 * Because there is an unofficial (and forbidden) 3.0.4 version floating
 * around the sub patchlevel number is used to distinguish versions.
 * A new display packet to tell the server what the view sizes are
 * and how many different debris intensities the client wants.
 * 3.0.4.2: new player-self status byte in self packet.
 * 3.0.4.3: different and incompatible laser packet.
 * New eyes packet to tell the client through wich players eyes we're
 * looking through in case the client is in game over move and it is locked
 * on someone else.
 * 3.1.0.0: new big patches implementing loads of new incompatible features.
 * Major cleanup.  Old clients (before 310) can't join new servers anymore.
 * 3.1.0.1: key-change-ids are now send as longs instead of bytes.
 * 3.1.0.2: different player status byte.
 * 3.1.0.3: different mine packet.
 * 3.2.0.0: New ship shape definition and big patches.
 * 3.2.0.1: Extended buffer for very large ship shape definitions.
 * 3.2.0.2: New mouse pointer control packet.
 * 3.2.5.0: Now client must ask for audio packets in order to get them.
 * 3.2.6.0: New map update packet.
 * 3.2.6.1: New player timing packet.
 * 3.2.8.0: New asyn packet.
 * 3.3.1.0: Different owner-only commands.
 * 3.3.2.0: Map decorations.
 * 3.4.0.0: Lose/drop item key.
 * 3.5.0.0: Player waiting queue.
 * 3.8.0.0: new items (deflector, hyperjump, phasing), keyboardsize and rounddelay.
 * 4.1.0.0: new item (mirror).
 * 4.2.0.0: new power/turnspeed behavior
 * 4.2.0.1: new item (armor).
 * 4.2.0.2: highest bit on in wreckagetype when deadly.
 * 4.2.0.3: different way of sending player item info.
 * 4.2.1.0: high bit in radar size means player is a teammate.
 * 4.3.0.0: transmit phasing separately from cloaking
 * 4.4.0.0: new object (asteroid)
 * 4.4.0.1: fast radar packet
 * 4.5.0.0: new team score packet; score packet made larger to send decimals
 * 4.5.0.1: temporary wormholes

 * Polygon branch
 * 4.F.0.9: 4.3.0.0 + xp2 map format
 * 4.F.1.0: Send_player(): Additional %c (1 when sending player's own info).
 * 4.F.1.1: support for everything in 4.5.0.1
 * 4.F.1.2: Show ships about to appear on bases, new team change packet.
 * 4.F.1.3: cumulative turning
 * 4.F.1.4: balls use polygon styles
 * 4.F.1.5: Possibility to change polygon styles.
 */
public static final int MAGIC_WORD = 	0xF4ED;
public static final int POLYGON_VERSION = 0x4F15;
public static final int OLD_VERSION = 0x4501;

// todo
//#ifdef SERVER
public static final int MAGIC = (false//is_polygon_map
               ? VERSION2MAGIC(POLYGON_VERSION)
               : VERSION2MAGIC(OLD_VERSION));
//#else
//public static final int MAGIC = (VERSION2MAGIC(protocolVersion));
//#endif

public static int MAGIC2VERSION(int M)	{return (((M) >> 16) & 0xFFFF);}
public static int VERSION2MAGIC(int V)	{return ((((V) & 0xFFFF) << 16) | MAGIC_WORD);}
public static final int MY_VERSION = MAGIC2VERSION(MAGIC);

/*
 * Which client versions can join this server.
 */
public static final int MIN_CLIENT_VERSION = 0x4203;
public static final int MAX_CLIENT_VERSION = MY_VERSION;

/*
 * Which server versions can this client join.
 */
public static final int MIN_SERVER_VERSION = 0x4F09;
public static final int MAX_SERVER_VERSION = MY_VERSION;

/*
 * We want to keep support for servers using the old map format in the client,
 * but make incompatible changes while developing the new format. Therefore
 * there is a separate "old" range of allowed servers.
 */
public static final int MIN_OLD_SERVER_VERSION = 0x4203;
public static final int MAX_OLD_SERVER_VERSION = 0x4501;
/* Which old-style (non-polygon) protocol version we support. */
public static final int COMPATIBILITY_MAGIC = 0x4501F4ED;

public static final int MAX_STR_LEN = 4096;
public static final int MAX_DISP_LEN = 80;
public static final int MAX_NAME_LEN = 16;
public static final int MAX_HOST_LEN = 64;

/*
 * Different contact pack types.
 */
public static final int ENTER_GAME_pack = 0x00;
public static final int ENTER_QUEUE_pack = 0x01;
public static final int REPLY_pack = 0x10;
public static final int REPORT_STATUS_pack = 0x21;
public static final int OPTION_LIST_pack = 0x28;
/*public static final int CORE_pack = 0x30*/;
public static final int CONTACT_pack = 0x31;
/* The owner-only commands have a common bit high. */
public static final int PRIVILEGE_PACK_MASK = 0x40;
public static final int LOCK_GAME_pack = 0x62;
public static final int MESSAGE_pack = 0x63;
public static final int SHUTDOWN_pack = 0x64;
public static final int KICK_PLAYER_pack = 0x65;
/*public static final int MAX_ROBOT_pack = 0x66*/;
public static final int OPTION_TUNE_pack = 0x67;
public static final int CREDENTIALS_pack = 0x69;

/*
 * Possible error codes returned.
 */
public static final int SUCCESS = 0x00;		/* Operation successful */
public static final int E_NOT_OWNER = 0x01;		/* Permission denied, not owner */
public static final int E_GAME_FULL = 0x02;		/* Game is full, play denied */
public static final int E_TEAM_FULL = 0x03;		/* Team is full, play denied */
public static final int E_TEAM_NOT_SET = 0x04;		/* Need to specify a team */
public static final int E_GAME_LOCKED = 0x05;		/* Game is locked, entry denied */
public static final int E_NOT_FOUND = 0x07;		/* Player was not found */
public static final int E_IN_USE = 0x08;		/* Name is already in use */
public static final int E_SOCKET = 0x09;		/* Can't setup socket */
public static final int E_INVAL = 0x0A;		/* Invalid input parameters */
public static final int E_VERSION = 0x0C;		/* Incompatible version */
public static final int E_NOENT = 0x0D;		/* No such variable */
public static final int E_UNDEFINED = 0x0E;		/* Operation undefined */

}