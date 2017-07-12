package org.xpilot.common;/*
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

public class Packet {
 /* before version 3.8.0 this was 8 bytes. */;
public static final int KEYBOARD_SIZE = 9;

/*
 * Definition of various client/server packet types.
 */

/* packet types: 0 - 9 */
public static final int PKT_UNDEFINED = 0;
public static final int PKT_VERIFY = 1;
public static final int PKT_REPLY = 2;
public static final int PKT_PLAY = 3;
public static final int PKT_QUIT = 4;
public static final int PKT_MESSAGE = 5;
public static final int PKT_START = 6;
public static final int PKT_END = 7;
public static final int PKT_SELF = 8;
public static final int PKT_DAMAGED = 9;

/* packet types: 10 - 19 */
public static final int PKT_CONNECTOR = 10;
public static final int PKT_REFUEL = 11;
public static final int PKT_SHIP = 12;
public static final int PKT_ECM = 13;
public static final int PKT_PAUSED = 14;
public static final int PKT_ITEM = 15;
public static final int PKT_MINE = 16;
public static final int PKT_BALL = 17;
public static final int PKT_MISSILE = 18;
public static final int PKT_SHUTDOWN = 19;

/* packet types: 20 - 29 */
public static final int PKT_STRING = 20;
public static final int PKT_DESTRUCT = 21;
public static final int PKT_RADAR = 22;
public static final int PKT_TARGET = 23;
public static final int PKT_KEYBOARD = 24;
public static final int PKT_SEEK = 25;
public static final int PKT_SELF_ITEMS = 26	/* still under development */;
public static final int PKT_TEAM_SCORE = 27	/* was PKT_SEND_BUFSIZE */;
public static final int PKT_PLAYER = 28;
public static final int PKT_SCORE = 29;

/* packet types: 30 - 39 */
public static final int PKT_FUEL = 30;
public static final int PKT_BASE = 31;
public static final int PKT_CANNON = 32;
public static final int PKT_LEAVE = 33;
public static final int PKT_POWER = 34;
public static final int PKT_POWER_S = 35;
public static final int PKT_TURNSPEED = 36;
public static final int PKT_TURNSPEED_S = 37;
public static final int PKT_TURNRESISTANCE = 38;
public static final int PKT_TURNRESISTANCE_S = 39;

/* packet types: 40 - 49 */
public static final int PKT_WAR = 40;
public static final int PKT_MAGIC = 41;
public static final int PKT_RELIABLE = 42;
public static final int PKT_ACK = 43;
public static final int PKT_FASTRADAR = 44;
public static final int PKT_TRANS = 45;
public static final int PKT_ACK_CANNON = 46;
public static final int PKT_ACK_FUEL = 47;
public static final int PKT_ACK_TARGET = 48;
public static final int PKT_SCORE_OBJECT = 49;

/* packet types: 50 - 59 */
public static final int PKT_AUDIO = 50;
public static final int PKT_TALK = 51;
public static final int PKT_TALK_ACK = 52;
public static final int PKT_TIME_LEFT = 53;
public static final int PKT_LASER = 54;
public static final int PKT_DISPLAY = 55;
public static final int PKT_EYES = 56;
public static final int PKT_SHAPE = 57;
public static final int PKT_MOTD = 58;
public static final int PKT_LOSEITEM = 59;

/* packet types: 60 - 69 */
public static final int PKT_APPEARING = 60;
public static final int PKT_TEAM = 61;
public static final int PKT_POLYSTYLE = 62;
public static final int PKT_ACK_POLYSTYLE = 63;
public static final int PKT_NOT_USED_64 = 64;
public static final int PKT_NOT_USED_65 = 65;
public static final int PKT_NOT_USED_66 = 66;
public static final int PKT_NOT_USED_67 = 67;
public static final int PKT_MODIFIERS = 68;
public static final int PKT_FASTSHOT = 69;	/* replaces SHOT/TEAMSHOT */

/* packet types: 70 - 79 */
public static final int PKT_THRUSTTIME = 70;
public static final int PKT_MODIFIERBANK = 71;
public static final int PKT_SHIELDTIME = 72;
public static final int PKT_POINTER_MOVE = 73;
public static final int PKT_REQUEST_AUDIO = 74;
public static final int PKT_ASYNC_FPS = 75;
public static final int PKT_TIMING = 76;
public static final int PKT_PHASINGTIME = 77;
public static final int PKT_ROUNDDELAY = 78;
public static final int PKT_WRECKAGE = 79;

/* packet types: 80 - 89 */
public static final int PKT_ASTEROID = 80;
public static final int PKT_WORMHOLE = 81;
public static final int PKT_NOT_USED_82 = 82;
public static final int PKT_NOT_USED_83 = 83;
public static final int PKT_NOT_USED_84 = 84;
public static final int PKT_NOT_USED_85 = 85;
public static final int PKT_NOT_USED_86 = 86;
public static final int PKT_NOT_USED_87 = 87;
public static final int PKT_NOT_USED_88 = 88;
public static final int PKT_NOT_USED_89 = 89;

/* packet types: 90 - 99 */
/*
 * Use these 10 packet type numbers for
 * experimenting with new packet types.
 */

/* status reports: 101 - 102 */
public static final int PKT_FAILURE = 101;
public static final int PKT_SUCCESS = 102;

/* optimized packet types: 128 - 255 */
public static final int PKT_DEBRIS = 128;		/* + color + x + y */

}
