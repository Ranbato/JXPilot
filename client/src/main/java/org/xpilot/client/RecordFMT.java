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

public class RecordFMT {
/*
 * Protocol version history:
 * 0.0: first protocol implementation.
 * 0.1: addition of tiled fills.
 */
public static final int RC_MAJORVERSION = '0';
public static final int RC_MINORVERSION = '1';

public static final int RC_NEWFRAME = 11;
public static final int RC_DRAWARC = 12;
public static final int RC_DRAWLINES = 13;
public static final int RC_DRAWLINE = 14;
public static final int RC_DRAWRECTANGLE = 15;
public static final int RC_DRAWSTRING = 16;
public static final int RC_FILLARC = 17;
public static final int RC_FILLPOLYGON = 18;
public static final int RC_PAINTITEMSYMBOL = 19;
public static final int RC_FILLRECTANGLE = 20;
public static final int RC_ENDFRAME = 21;
public static final int RC_FILLRECTANGLES = 22;
public static final int RC_DRAWARCS = 23;
public static final int RC_DRAWSEGMENTS = 24;
public static final int RC_GC = 25;
public static final int RC_NOGC = 26;
public static final int RC_DAMAGED = 27;
public static final int RC_TILE = 28;
public static final int RC_NEW_TILE = 29;

public static final int RC_GC_FG = (1 << 0);
public static final int RC_GC_BG = (1 << 1);
public static final int RC_GC_LW = (1 << 2);
public static final int RC_GC_LS = (1 << 3);
public static final int RC_GC_DO = (1 << 4);
public static final int RC_GC_FU = (1 << 5);
public static final int RC_GC_DA = (1 << 6);
public static final int RC_GC_B2 = (1 << 7);
public static final int RC_GC_FS = (1 << 8);
public static final int RC_GC_XO = (1 << 9);
public static final int RC_GC_YO = (1 << 10);
public static final int RC_GC_TI = (1 << 11);

}
