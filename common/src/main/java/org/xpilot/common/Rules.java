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

public class Rules{

/*
 * Bitfield definitions for playing mode.
 */
public static final int CRASH_WITH_PLAYER = (1<<0);
public static final int BOUNCE_WITH_PLAYER = (1<<1);
public static final int PLAYER_KILLINGS = (1<<2);
public static final int LIMITED_LIVES = (1<<3);
public static final int TIMING = (1<<4);
public static final int PLAYER_SHIELDING = (1<<6);
public static final int LIMITED_VISIBILITY = (1<<7);
public static final int TEAM_PLAY = (1<<8);
public static final int WRAP_PLAY = (1<<9);
public static final int ALLOW_NUKES = (1<<10);
public static final int ALLOW_CLUSTERS = (1<<11);
public static final int ALLOW_MODIFIERS = (1<<12);
public static final int ALLOW_LASER_MODIFIERS = (1<<13);
public static final int ALLIANCES = (1<<14);

/*
 * Client uses only a subset of them:
 */
public static final int CLIENT_RULES_MASK = (WRAP_PLAY|TEAM_PLAY|TIMING|LIMITED_LIVES|ALLIANCES);
/*
 * Old player status bits, currently only used in network protocol.
 * The bits that the client needs must fit into a byte,
 * so the first 8 bitvalues are reserved for that purpose.
 */
public static final int OLD_PLAYING = (1L<<0)	;	/* alive or killed */
public static final int OLD_PAUSE = (1L<<1); 	/* paused */
public static final int OLD_GAME_OVER = (1L<<2);		/* waiting or dead */

    int		lives;
    long	mode;

}
