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

public class XPConfig{

    public static final String CONF_DATADIR = "lib/";


public static final String CONF_LOCALGURU = PACKAGE_BUGREPORT;


public static final String CONF_DEFAULT_MAP = "ndh.xp2";



public static final String CONF_MAPDIR = CONF_DATADIR +"maps/";



public static final String CONF_TEXTUREDIR = CONF_DATADIR +"textures/";



public static final String CONF_SOUNDDIR = CONF_DATADIR +"sound/";



public static final String CONF_FONTDIR = CONF_DATADIR +"fonts/";



public static final String CONF_DEFAULTS_FILE_NAME = CONF_DATADIR +"defaults.txt";



public static final String CONF_PASSWORD_FILE_NAME = CONF_DATADIR +"password.txt";


/* not used currently */

public static final String CONF_PLAYER_PASSWORDS_FILE_NAME = CONF_DATADIR +"player_passwords.txt";



public static final String CONF_ROBOTFILE = CONF_DATADIR +"robots.txt";



public static final String CONF_SERVERMOTDFILE = CONF_DATADIR +"servermotd.txt";



public static final String CONF_LOCALMOTDFILE = CONF_DATADIR+ "localmotd.txt";



public static final String CONF_LOGFILE = CONF_DATADIR +"log.txt";



public static final String CONF_SOUNDFILE = CONF_SOUNDDIR +"sounds.txt";



public static final String CONF_SHIP_FILE = CONF_DATADIR +"shipshapes.txt";




/*
 * The following macros decide the speed of the game and
 * how often the server should draw a frame.  (Hmm...)
 */

public static final int CONF_UPDATES_PR_FRAME = 1;

/*
 * If COMPRESSED_MAPS is defined, the server will attempt to uncompress
 * maps on the fly (but only if neccessary). CONF_ZCAT_FORMAT should produce
 * a command that will unpack the given .gz file to stdout (for use in popen).
 * CONF_ZCAT_EXT should define the proper compressed file extension.
 */

public static final String CONF_ZCAT_EXT = ".gz";
public static final String CONF_ZCAT_FORMAT = "gzip -d -c < %s";

}