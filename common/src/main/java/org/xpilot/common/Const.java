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
public class Const {


public static final int LINE_MAX = 2048;

/* No comment. */
public static final int PATH_MAX = 1023;

public static final int RES = 128;

public static final int BLOCK_SZ = 35;

public static final int TABLE_SIZE = RES;

static double		tbl_sin[];
static double		tbl_cos[];


  /* New table lookup with optional range checking and no extra calculations. */
# define tsin(x)	(tbl_sin[CHK2(x, TABLE_SIZE)])
# define tcos(x)	(tbl_cos[CHK2(x, TABLE_SIZE)])

#define NELEM(a)	((int)(sizeof(a) / sizeof((a)[0])))

#define DELTA(a, b)		(((a) >= (b)) ? ((a) - (b)) : ((b) - (a)))
#define LENGTH(x, y)		( hypot( (double) (x), (double) (y) ) )
#define VECTOR_LENGTH(v)	( hypot( (double) (v).x, (double) (v).y ) )
#define QUICK_LENGTH(x,y)	( Math.abs(x)+ABS(y) ) /*-BA Only approx, but v. quick */
#define LIMIT(val, lo, hi)	( val=(val)>(hi)?(hi):((val)<(lo)?(lo):(val)) )


static final public double MOD2(double x,double m){		( (x) & ((m) - 1) );}

/* Do NOT change these! */
public static final int OLD_MAX_CHECKS = 26;
public static final int MAX_TEAMS = 10;

public static final int EXPIRED_MINE_ID = 4096   /* assume no player has this id */;

public static final int MAX_CHARS = 80;
public static final int MSG_LEN = 256;

public static final int NUM_MODBANKS = 4;

public static final float SPEED_LIMIT = 65.0f;
public static final float MAX_PLAYER_TURNSPEED = 64.0f;
public static final float MIN_PLAYER_TURNSPEED = 0.0f;
public static final float MAX_PLAYER_POWER = 55.0f;
public static final float MIN_PLAYER_POWER = 5.0f;
public static final float MAX_PLAYER_TURNRESISTANCE = 1.0f;
public static final float MIN_PLAYER_TURNRESISTANCE = 0.0f;

public static final float MAX_STATION_FUEL = 500.0f;
public static final float TARGET_DAMAGE = 250.0f;
public static final float SELF_DESTRUCT_DELAY = 150.0f;

/*
 * Size (pixels) of radius for legal HIT!
 * Was 14 until 4.2. Increased due to 'analytical collision detection'
 * which inspects a real circle and not just a square anymore.
 */
public static final int SHIP_SZ = 16;

public static final float VISIBILITY_DISTANCE = 1000.0;

public static final int BALL_RADIUS = 10;

public static final int MISSILE_LEN = 15;

public static final int TEAM_NOT_SET = 0xffff;

public static final int DEBRIS_TYPES = (8 * 4 * 4);


/*
 * The server supports only 4 colors, except for spark/debris, which
 * may have 8 different colors.
 */
public static final int NUM_COLORS = 4;

public static final int BLACK = 0;
public static final int WHITE = 1;
public static final int BLUE = 2;
public static final int RED = 3;


/*
 * The minimum and maximum playing window sizes supported by the server.
 */
public static final int MIN_VIEW_SIZE = 384;
public static final int MAX_VIEW_SIZE = 1024;
public static final int DEF_VIEW_SIZE = 768;

/*
 * Spark rand limits.
 */
public static final int MIN_SPARK_RAND = 0		/* Not display spark */;
public static final int MAX_SPARK_RAND = 0x80	/* Always display spark */;
public static final int DEF_SPARK_RAND = 0x55	/* 66% */;

public static final int	UPDATE_SCORE_DELAY = 	(FPS);

/*
 * Polygon style flags
 */
public static final int STYLE_FILLED = (1 << 0);
public static final int STYLE_TEXTURED = (1 << 1);
public static final int STYLE_INVISIBLE = (1 << 2);
public static final int STYLE_INVISIBLE_RADAR = (1 << 3);

}