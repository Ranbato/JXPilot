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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpilot.common.Setup;
import org.xpilot.common.ShipShape;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.xpilot.common.Const.*;
import static org.xpilot.common.Item.NUM_ITEMS;
import static org.xpilot.common.Rules.WRAP_PLAY;
import static org.xpilot.common.Setup.*;

public class Client {

    Logger logger = LoggerFactory.getLogger(Client.class);

public static class ClientData {
    boolean	talking;	/* Some talk window is open? */
    boolean	pointerControl;	/* Pointer (mouse) control is on? */
    boolean	restorePointerControl;
				/* Pointer control should be restored later? */
    boolean	quitMode;	/* Client is in quit mode? */
    double	clientLag;
    double	scaleFactor;
    double	scale;
    float	fscale;
    double	altScaleFactor;
} 

public static class Instruments {
    boolean clientRanker;
    boolean clockAMPM;
    boolean filledDecor;
    boolean filledWorld;
    boolean outlineDecor;
    boolean outlineWorld;
    boolean showDecor;
    boolean showItems;
    boolean showLivesByShip;
    boolean showMessages;
    boolean showMyShipShape;
    boolean showNastyShots;
    boolean showShipShapes;
    boolean showShipShapesHack;
    boolean slidingRadar;
    boolean texturedDecor;
    boolean texturedWalls;
} 

public static class XPArgs {
    boolean help;
    boolean version;
    boolean text;
    boolean list_servers; /* list */
    boolean auto_connect; /* join */
    String shutdown_reason; /* shutdown reason */
} 

public static final int PACKET_LOSS = 0;
 public static final int PACKET_DROP = 1;
  public static final int PACKET_DRAW = 2; 
public static final int MAX_SCORE_OBJECTS = 10; 
public static final int MAX_SPARK_SIZE = 8;
 public static final int MIN_SPARK_SIZE = 1;
  public static final int MAX_MAP_POINT_SIZE = 8;
   public static final int MIN_MAP_POINT_SIZE = 0;
    public static final int MAX_SHOT_SIZE = 20;
     public static final int MIN_SHOT_SIZE = 1; 
     public static final int MAX_TEAMSHOT_SIZE = 20;
      public static final int MIN_TEAMSHOT_SIZE = 1; 
static public final float MIN_SHOW_ITEMS_TIME= 0.0f;
static public final float MAX_SHOW_ITEMS_TIME= 300.0f;

static public final float MIN_SCALEFACTOR= 0.1f;
static public final float MAX_SCALEFACTOR= 20.0f;

static public final float FUEL_NOTIFY_TIME= 3.0f;
static public final float CONTROL_TIME= 8.0f;

public static final int MAX_MSGS = 15; /* Max. messages displayed ever */
public static final int MAX_HIST_MSGS = 128; /* Max. messages in history */

static public final float MSG_LIFE_TIME= 120.0f;	/* Seconds */
static public final float MSG_FLASH_TIME= 105.0f;	/* Old messages have life time less
					   than this */
public static final int MAX_POINTER_BUTTONS = 5; 


public static final class Other {
    double	score;
    short	id;
    short	team;
    short	check;
    short	round;
    long	timing_loops;
    short	timing;
    short	life;
    short	mychar;
    short	alliance;
    short	name_width;	/* In pixels */
    short	name_len;	/* In bytes */
    short	max_chars_in_names;	/* name_width was calculated
					   for this value of maxCharsInNames */
    short	ignorelevel;
    ShipShape ship;
    String	nick_name;
    String	user_name;
    String	host_name;
    String	id_string;
} 

public static final class Homebase{
    int		pos;		/* Block index */
    short	id;		/* Id of owner or -1 */
    short	team;		/* Team this base belongs to */
    Rectangle bounds;		/* Location on map */
    int		type;		/* orientation */
    long	appeartime;	/* For base warning */
} 

public static final class CannonTime{
    int		pos;		/* Block index */
    short	dead_time;	/* Frames inactive */
	short	dot;		/* Draw dot if inactive */
}

public static final class Target{
    int		pos;		/* Block index */
    short	dead_time;	/* Frames inactive */
    double	damage;		/* Damage to target */
}

public static final class Checkpoint{
    int		pos;		/* Block index */
    Rectangle bounds;		/* Location on map */
}

public static final class EdgeStyle{
    int width;			/* Line width, -1 means no line */
     long color;	/* Line color */
    int rgb;			/* RGB values corresponding to color */
    int style;			/* 0=LineSolid, 1=LineOnOffDash, 2=LineDoubleDash */
}

public static final class PolygonStyle {
     long color;	/* The color if drawn in filled mode */
    int rgb;			/* RGB values corresponding to color */
    int texture;		/* The texture if drawn in texture mode */
    int flags;			/* Flags about this style (see draw.h) */
    int def_edge_style;		/* The default style for edges */
}

public static final class XPPolygon{
    Point []points;		/* points[0] is absolute, rest are relative */
    int num_points;		/* number of points */
    Rectangle bounds;		/* bounding box for the polygon */
    int []edge_styles;		/* optional array of indexes to edge_styles */
    int style;			/* index to polygon_styles array */
}


/*
 * Types for dynamic game data
 */

public static final class Refuel{
    short		x0, y0, x1, y1;
}

public static final class Connector{
    short		x0, y0, x1, y1;
    byte		tractor;
}

public static final class Laser{
    byte	color, dir;
    short		x, y, len;
}

public static final class Missile {
    short		x, y, dir;
    byte	len;
}

public static final class Ball{
    short		x, y, id;
    byte		style;
}

public static final class Ship{
    short		x, y, id, dir;
    byte		shield, cloak, eshield;
    byte		phased, deflector;
}

public static final class Mine{
    short		x, y, teammine, id;
}

public static final class ItemType{
    short		x, y, type;
}

public static final class ECM{
    short		x, y, size;
}

public static final class Trans{
    short		x1, y1, x2, y2;
}

public static final class Paused{
    short		x, y, count;
}

public static final class Appearing{
    short		x, y, id, count;
} ;

public enum RadarType{
    RadarEnemy,
    RadarFriend
}

public static final class Radar{
    short		x, y, size;
    RadarType        type;
}

public static final class VCannon{
    short		x, y, type;
}

public static final class VFuel{
    short		x, y;
    double		fuel;
}

public static final class VBase{
    short		x, y, xi, yi, type;
}

public static final class Debris{
    byte		x, y;
}

public static final class VDecor {
    short		x, y, xi, yi, type;
}

public static final class Wreckage{
    short		x, y;
    byte		wrecktype, size, rotation;
}

public static final class Asteroid{
    short		x, y;
    byte		type, size, rotation;
}

public static final class Wormhole{
    short		x, y;
}


/*static public final float SCORE_OBJECT_COUNT= 100;*/
public static final class ScoreObject{
    double	score,
		life_time;
    int		x,
		y,
		hud_msg_len,
		hud_msg_width,
		msg_width,
		msg_len;
    String	msg;
	String	hud_msg;
} ;


/*
 * is a selection pending (in progress), done, drawn emphasized?
 */
public static final int SEL_NONE    =   (1 << 0);
    public static final int SEL_PENDING  =  (1 << 1);
    public static final int SEL_SELECTED =  (1 << 2);
    public static final int SEL_EMPHASIZED= (1 << 3);

/*
 * a selection (text, string indices, state,...)
 */
public static class Selection {
    /* a selection in the talk window */
    public static class Talk {
        boolean    state;	/* current state of the selection */
        int  x1;	/* string indices */
        int  x2;
        boolean    incl_nl;/* include a '\n'? */
    }
    public Talk talk = new Talk();
    /* a selection in the draw window */
    public static class Draw {
        boolean    state;
        int     x1;	/* string indices (for TalkMsg[].txt) */
        int     x2;	/* they are modified when the emphasized area */
        int     y1;	/* is scrolled down by new messages coming in */
        int     y2;
    }
    public Draw draw = new Draw();
    String	txt;   /* allocated when needed */
    int	txt_size;	/* size of txt buffer */
    int	len;
    /* when a message 'jumps' from talk window to the player messages: */
    boolean	keep_emphasizing;
}

/* typedefs begin */
 public enum MsgBms{
    BmsNone ,
    BmsBall,
    BmsSafe,
    BmsCover,
    BmsPop
}

public static final class Message{
    String		txt;
    int		len;
    /*short		pixelLen;*/
    double		lifeTime;
    MsgBms		bmsinfo;
}


ClientData	clData = new ClientData();

//char	*geometry;
XPArgs xpArgs;
ConnectParam connectParam;

boolean	newbie;
int	baseWarningType;	/* Which type of base warning you prefer */
int	maxCharsInNames;
int	hudRadarDotSize;	/* Size for hudradar dot drawing */
double	hudRadarScale;		/* Scale for hudradar drawing */
double	hudRadarLimit;		/* Hudradar dots are not drawn if closer to
				   your ship than this factor of visible
				   range */
int	hudSize;		/* Size for HUD drawing, depends on hudScale */

boolean	is_server = false;	/* used in common code */

boolean	scoresChanged = true;
int RadarHeight = 0;
int RadarWidth = 256;	/* radar width at the server */
boolean     UpdateRadar = false;   /* radar update because of polystyle changes? */

int     oldServer;
Point 	selfPos;
Point 	selfVel;
short	heading;
short	nextCheckPoint;

byte	[]numItems = new byte[NUM_ITEMS.ord];	/* Count of currently owned items */
byte	[]lastNumItems=new byte[NUM_ITEMS.ord];/* Last item count shown */
int	[]numItemsTime=new int[NUM_ITEMS.ord];/* Number of frames to show this item count */
double	showItemsTime;		/* How long to show changed item count for */
double	scoreObjectTime;	/* How long to flash score objects */

short	autopilotLight;

int	showScoreDecimals;

short	lock_id;		/* Id of player locked onto */
short	lock_dir;		/* Direction of lock */
short	lock_dist;		/* Distance to player locked onto */

int	eyesId;                 /* Player we get frame updates for */
Other	eyes = null;	        /* Player we get frame updates for */
boolean	snooping;               /* are we snooping on someone else? */
int	eyeTeam = TEAM_NOT_SET;

Other	self = null;	        /* player info */
short	selfVisible;		/* Are we alive and playing? */
short	damaged;		/* Damaged by ECM */
short	destruct;		/* If self destructing */
short	shutdown_delay;
short	shutdown_count;
short	thrusttime;
short	thrusttimemax;
short	shieldtime;
short	shieldtimemax;
short	phasingtime;
short	phasingtimemax;

int	roundDelay;		/* != 0 means we're in a delay */
int	roundDelayMax;		/* (not yet) used for graph of time
				   remaining in delay */

int	backgroundPointDist;	/* spacing of navigation points */
int	backgroundPointSize;	/* size of navigation points */
int	sparkSize;		/* size of debris and spark */
int	shotSize;		/* size of shot */
int	teamShotSize;		/* size of team shot */
double	controlTime;		/* Display control for how long? */
byte	spark_rand;		/* Sparkling effect */
byte	old_spark_rand;		/* previous value of spark_rand */

double	fuelSum;		/* Sum of fuel in all tanks */
double	fuelMax;		/* How much fuel can you take? */
short	fuelCurrent;		/* Number of currently used tank */
short	numTanks;		/* Number of tanks */
double	fuelTime;		/* Display fuel for how long? */
double	fuelCritical;		/* Fuel critical level */
double	fuelWarning;		/* Fuel warning level */
double	fuelNotify;		/* Fuel notify level */

String	shipShape = null;	/* Shape of player's ship */
double	power;			/* Force of thrust */
double	power_s;		/* Saved power fiks */
double	turnspeed;		/* How fast player acc-turns */
double	turnspeed_s;		/* Saved turnspeed */
double	turnresistance;		/* How much is lost in % */
double	turnresistance_s;	/* Saved (see above) */
double	displayedPower;		/* What the server is sending us */
double	displayedTurnspeed;	/* What the server is sending us */
double	displayedTurnresistance;/* What the server is sending us */
double	sparkProb;		/* Sparkling effect user configurable */
int     charsPerSecond;         /* Message output speed (configurable) */

double	hud_move_fact;		/* scale the hud-movement (speed) */
double	ptr_move_fact;		/* scale the speed pointer length */
Instruments	instruments;		/* Instruments on screen */
	String mods;	/* Current modifiers in effect */
int	packet_size;		/* Current frame update packet size */
int	packet_loss;		/* lost packets per second */
int	packet_drop;		/* dropped packets per second */
int	packet_lag;		/* approximate lag in frames */
int [] packet_measure;	/* packet measurement in a second */
long	packet_loop;		/* start of measurement */

boolean	showUserName = false;	/* Show user name instead of nick name */
String	servername;	/* Name of server connecting to */
int	version;	/* Version of the server */
boolean	toggle_shield;		/* Are shields toggled by a press? */
boolean	shields = true;		/* When shields are considered up */
boolean	auto_shield = true;	/* shield drops for fire */
String[]	modBankStr = new String[NUM_MODBANKS]; /* modifier banks */

int	maxFPS;			/* Max FPS player wants from server */
int	oldMaxFPS = 0;
double	clientFPS = 1.0;	/* FPS client is drawing at */
int	recordFPS = 0;		/* What FPS to record at */
long	currentTime = 0;	/* Current value of time() */
boolean	newSecond = false;	/* Did time() increment this frame? */

int	maxMouseTurnsPS = 0;
int	mouseMovementInterval = 0;
int	cumulativeMouseMovement = 0;

int	clientPortStart = 0;	/* First UDP port for clients */
int	clientPortEnd = 0;	/* Last one (these are for firewalls) */

byte	lose_item;		/* index for dropping owned item */
int	lose_item_active;	/* one of the lose keys is pressed */

static double[] teamscores = new double[MAX_TEAMS];
List<CannonTime> cannons = null;
static List<Target> targets = null;
static int          num_targets = 0;

List<Fuelstation> fuels = null;
List<Homebase>          bases = null;
List<Checkpoint> checks = null;
List<XPPolygon> polygons = null;
int                 num_polygons = 0;
List<EdgeStyle> edge_styles = null;
int                 num_edge_styles = 0;
List<PolygonStyle> polygon_styles = null;
int                 num_polygon_styles = 0;

ScoreObject  []    score_objects = new ScoreObject[MAX_SCORE_OBJECTS];
int                 score_object = 0;
List<Other> Others = null;
int                 num_others = 0, max_others = 0;
List<Refuel> refuel_ptr;
int                 num_refuel, max_refuel;
List<Connector> connector_ptr;
int                 num_connector, max_connector;
List<Laser> laser_ptr;
int                 num_laser, max_laser;
List<Missile> missile_ptr;
int                 num_missile, max_missile;
List<Ball> ball_ptr;
int                 num_ball, max_ball;
List<Ship> ship_ptr;
int                 num_ship, max_ship;
List<Mine> mine_ptr;
int                 num_mine, max_mine;
List<ItemType> itemtype_ptr;
int                 num_itemtype, max_itemtype;
List<ECM> ecm_ptr;
int                 num_ecm, max_ecm;
List<Trans> trans_ptr;
int                 num_trans, max_trans;
List<Paused> paused_ptr;
int                 num_paused, max_paused;
List<Appearing> appearing_ptr;
int                 num_appearing, max_appearing;
List<Radar> radar_ptr;
int                 num_radar, max_radar;
List<VCannon> vcannon_ptr;
int                 num_vcannon, max_vcannon;
List<VFuel> vfuel_ptr;
int                 num_vfuel, max_vfuel;
List<VBase> vbase_ptr;
int                 num_vbase, max_vbase;
List<List<Debris>> debris_ptr = new ArrayList<>(DEBRIS_TYPES);
int    []             num_debris = new int[DEBRIS_TYPES];
    int    []                   max_debris = new int[DEBRIS_TYPES];
    List<List<Debris>> fastshot_ptr =new ArrayList<>(DEBRIS_TYPES*2);
int      []           num_fastshot = new int[DEBRIS_TYPES*  2];
    int      []           max_fastshot = new int[DEBRIS_TYPES*  2];

List<VDecor> vdecor_ptr;
int                 num_vdecor, max_vdecor;
List<Wreckage> wreckage_ptr;
int                 num_wreckage, max_wreckage;
List<Asteroid> asteroid_ptr;
int                 num_asteroids, max_asteroids;
List<Wormhole> wormhole_ptr;
int                 num_wormholes, max_wormholes;

int                 num_playing_teams = 0;
long		    time_left = -1;
long		    start_loops, end_loops;

public Fuelstation Fuelstation_by_pos(int x, int y)
{
    int			i, lo, hi, pos;

    lo = 0;
    hi = fuels.size() - 1;
    pos = x * Setup.y + y;
    while (lo < hi) {
	i = (lo + hi) >> 1;
	if (pos > fuels.get(i).pos)
	    lo = i + 1;
	else
	    hi = i;
    }
    if (lo == hi && pos == fuels.get(lo).pos)
	return fuels.get(lo);
    logger.warn("No fuelstation at ({},{})", x, y);
    return null;
}

double Fuel_by_pos(int x, int y)
{
    Fuelstation	fuelp;

    if ((fuelp = Fuelstation_by_pos(x, y)) == null)
	return 0;
    return fuelp.fuel;
}

/** Unused
int Target_by_index(int ind, int *xp, int *yp, int *dead_time, double *damage)
{
    if (ind < 0 || ind >= num_targets)
	return -1;
    *xp = targets[ind].pos / Setup.y;
    *yp = targets[ind].pos % Setup.y;
    *dead_time = targets[ind].dead_time;
    *damage = targets[ind].damage;
    return 0;
}
*/

Target Target_alive(int x, int y)
{
    int 		i, lo, hi, pos;

    lo = 0;
    hi = num_targets - 1;
    pos = x * Setup.y + y;
    while (lo < hi) {
	i = (lo + hi) >> 1;
	if (pos > targets.get(i).pos)
	    lo = i + 1;
	else
	    hi = i;
    }
    if (lo == hi && pos == targets.get(lo).pos) {
	return targets.get(lo);
    }
    logger.warn("No targets at ({},{})", x, y);
    return null;
}

int Handle_fuel(int ind, double fuel)
{
    if (ind < 0 || ind >= fuels.size()) {
	logger.warn("Bad fuelstation index ({})", ind);
	return -1;
    }
    fuels.get(ind).fuel = fuel;
    return 0;
}

public CannonTime Cannon_by_pos(int x, int y)
{
    int			i, lo, hi, pos;

    lo = 0;
    hi = cannons.size() - 1;
    pos = x * Setup.y + y;
    while (lo < hi) {
	i = (lo + hi) >> 1;
	if (pos > cannons.get(i).pos)
	    lo = i + 1;
	else
	    hi = i;
    }
    if (lo == hi && pos == cannons.get(lo).pos)
	return cannons.get(lo);
    logger.warn("No cannon at ({},{})", x, y);
    return null;
}

CannonTime Cannon_dead_time_by_pos(int x, int y)
{
    CannonTime	cannonp;

    if ((cannonp = Cannon_by_pos(x, y)) == null) {
        return null;
    }
    return cannonp;
}

int Handle_cannon(int ind, int dead_time)
{
    if (ind < 0 || ind >= cannons.size()) {
	logger.warn("Bad cannon index ({})", ind);
	return 0;
    }
    cannons.get(ind).dead_time = (short)dead_time;
    return 0;
}

int Handle_target(int num, int dead_time, double damage)
{
    if (num < 0 || num >= num_targets) {
	logger.warn("Bad target index ({})", num);
	return 0;
    }
    if (dead_time == 0
	&& (damage <= 0.0 || damage > TARGET_DAMAGE))
	logger.warn("BUG target {}, dead {}, damage {}", num, dead_time, damage);

    Target target = targets.get(num);
    if (target.dead_time > 0 && dead_time == 0) {
	int pos = target.pos;
	// todo SDL not implemented
//	Radar_show_target(pos / Setup.y, pos % Setup.y);
    }
    else if (target.dead_time == 0 && dead_time > 0) {
	int pos = target.pos;
        // todo SDL not implemented
//	Radar_hide_target(pos / Setup.y, pos % Setup.y);
    }

    target.dead_time = (short)dead_time;
    target.damage = damage;

    return 0;
}

public Homebase Homebase_by_pos(int x, int y)
{
    int			i, lo, hi, pos;

    lo = 0;
    hi = bases.size() - 1;
    pos = x * Setup.y + y;
    while (lo < hi) {
	i = (lo + hi) >> 1;
	if (pos > bases.get(i).pos)
	    lo = i + 1;
	else
	    hi = i;
    }
    if (lo == hi && pos == bases.get(lo).pos)
	return bases.get(lo);
    logger.warn("No homebase at ({},{})", x, y);
    return null;
}

    Homebase Base_info_by_pos(int x, int y)
{
    return Homebase_by_pos(x, y);

}

int Handle_base(int id, int ind)
{
    int		i;

    if (ind < 0 || ind >= bases.size()) {
	logger.warn("Bad homebase index ({})", ind);
	return -1;
    }
    for (i = 0; i < bases.size(); i++) {
	if (bases.get(i).id == id)
	    bases.get(i).id = -1;
    }
    bases.get(ind).id = (short) id;

    return 0;
}

Point Check_pos_by_index(int ind)
{
    Point p = new Point();
    if (ind < 0 || ind >= checks.size()) {
	logger.warn("Bad checkpoint index ({})", ind);
	p.x = 0;
	p.y = 0;
   }else {
        p.x = checks.get(ind).pos / Setup.y;
        p.y = checks.get(ind).pos % Setup.y;
    }
    return p;
}

int Check_index_by_pos(int x, int y)
{
    int			i, pos;

    pos = x * Setup.y + y;
    for (i = 0; i < checks.size(); i++) {
	if (pos == checks.get(i).pos)
	    return i;
    }
    logger.warn("Can't find checkpoint ({},{})", x, y);
    return 0;
}

/*
 * Convert a 'space' map block into a dot.
 */
static void Map_make_dot( int[] data, int index)
{
    if (data[index] == SETUP_SPACE)
        data[index] = SETUP_SPACE_DOT;
    else if (data[index] == SETUP_DECOR_FILLED)
    data[index] = SETUP_DECOR_DOT_FILLED;
    else if (data[index] == SETUP_DECOR_RU)
    data[index] = SETUP_DECOR_DOT_RU;
    else if (data[index] == SETUP_DECOR_RD)
    data[index] = SETUP_DECOR_DOT_RD;
    else if (data[index] == SETUP_DECOR_LU)
    data[index] = SETUP_DECOR_DOT_LU;
    else if (data[index] == SETUP_DECOR_LD)
    data[index] = SETUP_DECOR_DOT_LD;
}

/*
 * Optimize the drawing of all blue space dots by converting
 * certain map objects into a specialised form of their type.
 */
void Map_dots()
{
    int			i,
			x,
			y,
			start;
    int	[]dot = new int[256];

    /*
     * Lookup table to recognize dots.
     */
    dot[SETUP_SPACE_DOT] = 1;
    dot[SETUP_DECOR_DOT_FILLED] = 1;
    dot[SETUP_DECOR_DOT_RU] = 1;
    dot[SETUP_DECOR_DOT_RD] = 1;
    dot[SETUP_DECOR_DOT_LU] = 1;
    dot[SETUP_DECOR_DOT_LD] = 1;

    /*
     * Restore the map to unoptimized form.
     */
    for (i = Setup.x * Setup.y; i-- > 0; ) {
	if (dot[Setup.map_data[i]] != 0) {
	    if (Setup.map_data[i] == SETUP_SPACE_DOT)
		Setup.map_data[i] = SETUP_SPACE;
	    else if (Setup.map_data[i] == SETUP_DECOR_DOT_FILLED)
		Setup.map_data[i] = SETUP_DECOR_FILLED;
	    else if (Setup.map_data[i] == SETUP_DECOR_DOT_RU)
		Setup.map_data[i] = SETUP_DECOR_RU;
	    else if (Setup.map_data[i] == SETUP_DECOR_DOT_RD)
		Setup.map_data[i] = SETUP_DECOR_RD;
	    else if (Setup.map_data[i] == SETUP_DECOR_DOT_LU)
		Setup.map_data[i] = SETUP_DECOR_LU;
	    else if (Setup.map_data[i] == SETUP_DECOR_DOT_LD)
		Setup.map_data[i] = SETUP_DECOR_LD;
	}
    }

    /*
     * Lookup table to test for map data which can be turned into a dot.
     */
    dot = new int[256];
    dot[SETUP_SPACE] = 1;
    if (!instruments.showDecor) {
	dot[SETUP_DECOR_FILLED] = 1;
	dot[SETUP_DECOR_RU] = 1;
	dot[SETUP_DECOR_RD] = 1;
	dot[SETUP_DECOR_LU] = 1;
	dot[SETUP_DECOR_LD] = 1;
    }

    /*
     * Optimize.
     */
    if (backgroundPointSize > 0) {
	if (Setup.mode.get( WRAP_PLAY)) {
	    for (x = 0; x < Setup.x; x++) {
		if (dot[Setup.map_data[x * Setup.y]]!=0)
		    Map_make_dot(Setup.map_data,(x * Setup.y));
	    }
	    for (y = 0; y < Setup.y; y++) {
		if (dot[Setup.map_data[y]]!= 0)
		    Map_make_dot(Setup.map_data,y);
	    }
	    start = backgroundPointDist;
	} else
	    start = 0;

	if (backgroundPointDist > 0) {
	    for (x = start; x < Setup.x; x += backgroundPointDist) {
		for (y = start; y < Setup.y; y += backgroundPointDist) {
		    if (dot[Setup.map_data[x * Setup.y + y]]!= 0)
			Map_make_dot(Setup.map_data,x * Setup.y + y);
		}
	    }
	}
	for (i = 0; i < cannons.size(); i++) {
	    CannonTime ct = cannons.get(i);
	    x = ct.pos / Setup.y;
	    y = ct.pos % Setup.y;
	    if ((x == 0 || y == 0) && Setup.mode.get( WRAP_PLAY))
		ct.dot = 1;
	    else if (backgroundPointDist > 0
		&& x % backgroundPointDist == 0
		&& y % backgroundPointDist == 0)
		ct.dot = 1;
	    else
		ct.dot = 0;
	}
    }
}

/*
 * Optimize the drawing of all blue map objects by converting
 * their map type to a bitmask with bits for each blue segment.
 */
void Map_restore(int startx, int starty, int width, int height)
{
    int			i, j,
			x, y,
			map_index,
			type;

    /*
     * Restore an optimized map to its original unoptimized state.
     */
    x = startx;
    for (i = 0; i < width; i++, x++) {
	if (x < 0)
	    x += Setup.x;
	else if (x >= Setup.x)
	    x -= Setup.x;

	y = starty;
	for (j = 0; j < height; j++, y++) {
	    if (y < 0)
		y += Setup.y;
	    else if (y >= Setup.y)
		y -= Setup.y;

	    map_index = x * Setup.y + y;

	    type = Setup.map_data[map_index];
	    if ((type & BLUE_BIT) == 0) {
		if (type == SETUP_FILLED_NO_DRAW)
		    Setup.map_data[map_index] = SETUP_FILLED;
	    }
	    else if ((type & BLUE_FUEL) == BLUE_FUEL)
		Setup.map_data[map_index] = SETUP_FUEL;

	    else if ((type & BLUE_OPEN) == BLUE_OPEN) {
		if ((type & BLUE_BELOW) == BLUE_BELOW)
		    Setup.map_data[map_index] = SETUP_REC_RD;
		else
		    Setup.map_data[map_index] = SETUP_REC_LU;
	    }
	    else if ((type & BLUE_CLOSED) == BLUE_CLOSED){
		if ((type & BLUE_BELOW) == BLUE_BELOW)
		    Setup.map_data[map_index] = SETUP_REC_LD;
		else
		    Setup.map_data[map_index] = SETUP_REC_RU;
	    } else
		Setup.map_data[map_index] = SETUP_FILLED;
	}
    }
}

void Map_blue(int startx, int starty, int width, int height)
{
    int			i, j,
			x, y,
			map_index,
			type,
			newtype;
     char	[]blue;
    boolean		outline = false;

    if (instruments.outlineWorld ||
	instruments.filledWorld ||
	instruments.texturedWalls)
	outline = true;
    /*
     * Optimize the map for blue.
     */
    blue = new char[256];
    blue[SETUP_FILLED] = BLUE_LEFT | BLUE_UP | BLUE_RIGHT | BLUE_DOWN;
    blue[SETUP_FILLED_NO_DRAW] = blue[SETUP_FILLED];
    blue[SETUP_FUEL] = blue[SETUP_FILLED];
    blue[SETUP_REC_RU] = BLUE_RIGHT | BLUE_UP;
    blue[SETUP_REC_RD] = BLUE_RIGHT | BLUE_DOWN;
    blue[SETUP_REC_LU] = BLUE_LEFT | BLUE_UP;
    blue[SETUP_REC_LD] = BLUE_LEFT | BLUE_DOWN;
    blue[BLUE_BIT|BLUE_OPEN] =
    blue[BLUE_BIT|BLUE_OPEN|BLUE_LEFT] =
    blue[BLUE_BIT|BLUE_OPEN|BLUE_UP] =
    blue[BLUE_BIT|BLUE_OPEN|BLUE_LEFT|BLUE_UP] =
	blue[SETUP_REC_LU];
    blue[BLUE_BIT|BLUE_OPEN|BLUE_BELOW] =
    blue[BLUE_BIT|BLUE_OPEN|BLUE_BELOW|BLUE_RIGHT] =
    blue[BLUE_BIT|BLUE_OPEN|BLUE_BELOW|BLUE_DOWN] =
    blue[BLUE_BIT|BLUE_OPEN|BLUE_BELOW|BLUE_RIGHT|BLUE_DOWN] =
	blue[SETUP_REC_RD];
    blue[BLUE_BIT|BLUE_CLOSED] =
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_RIGHT] =
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_UP] =
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_RIGHT|BLUE_UP] =
	blue[SETUP_REC_RU];
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_BELOW] =
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_BELOW|BLUE_LEFT] =
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_BELOW|BLUE_DOWN] =
    blue[BLUE_BIT|BLUE_CLOSED|BLUE_BELOW|BLUE_LEFT|BLUE_DOWN] =
	blue[SETUP_REC_LD];
    for (i = BLUE_BIT; i < blue.length; i++) {
	if ((i & BLUE_FUEL) == BLUE_FUEL
	    || (i & (BLUE_OPEN|BLUE_CLOSED)) == 0)
	    blue[i] = blue[SETUP_FILLED];
    }

    x = startx;
    for (i = 0; i < width; i++, x++) {
	if (x < 0)
	    x += Setup.x;
	else if (x >= Setup.x)
	    x -= Setup.x;

	y = starty;
	for (j = 0; j < height; j++, y++) {
	    if (y < 0)
		y += Setup.y;
	    else if (y >= Setup.y)
		y -= Setup.y;

	    map_index = x * Setup.y + y;

	    type = Setup.map_data[map_index];
	    newtype = 0;
	    switch (type) {
	    case SETUP_FILLED:
	    case SETUP_FILLED_NO_DRAW:
	    case SETUP_FUEL:
		newtype = BLUE_BIT;
		if (type == SETUP_FUEL) {
		    newtype |= BLUE_FUEL;
		}
		if ((x == 0)
		    ? (!Setup.mode.get( WRAP_PLAY) ||
			(blue[Setup.map_data[(Setup.x - 1) * Setup.y + y]]
			    & BLUE_RIGHT) == 0)
		    : (blue[Setup.map_data[(x - 1) * Setup.y + y]]
			& BLUE_RIGHT) == 0)
		    newtype |= BLUE_LEFT;
		if ((y == 0)
		    ? (!Setup.mode.get( WRAP_PLAY) ||
			(blue[Setup.map_data[x * Setup.y + Setup.y - 1]]
			    & BLUE_UP)==0)
		    : (blue[Setup.map_data[x * Setup.y + (y - 1)]]
			& BLUE_UP)==0)
		    newtype |= BLUE_DOWN;
		if (!outline
		    || ((x == Setup.x - 1)
			? (!Setup.mode.get( WRAP_PLAY)
			   || (blue[Setup.map_data[y]]
				& BLUE_LEFT)==0)
			: (blue[Setup.map_data[(x + 1) * Setup.y + y]]
			    & BLUE_LEFT)==0))
		    newtype |= BLUE_RIGHT;
		if (!outline
		    || ((y == Setup.y - 1)
			? (!Setup.mode.get( WRAP_PLAY)
			   || (blue[Setup.map_data[x * Setup.y]]
				& BLUE_DOWN)==0)
			: (blue[Setup.map_data[x * Setup.y + (y + 1)]]
			    & BLUE_DOWN)==0))
		    newtype |= BLUE_UP;
		break;

	    case SETUP_REC_LU:
		newtype = BLUE_BIT | BLUE_OPEN;
		if (x == 0
		    ? (!Setup.mode.get( WRAP_PLAY) ||
			(blue[Setup.map_data[(Setup.x - 1) * Setup.y + y]]
			    & BLUE_RIGHT) == 0)
		    : (blue[Setup.map_data[(x - 1) * Setup.y + y]]
			& BLUE_RIGHT)==0)
		    newtype |= BLUE_LEFT;
		if (!outline
		    || ((y == Setup.y - 1)
			? (!Setup.mode.get( WRAP_PLAY)
			   || (blue[Setup.map_data[x * Setup.y]]
				& BLUE_DOWN)==0)
			: (blue[Setup.map_data[x * Setup.y + (y + 1)]]
			    & BLUE_DOWN)==0))
		    newtype |= BLUE_UP;
		break;

	    case SETUP_REC_RU:
		newtype = BLUE_BIT | BLUE_CLOSED;
		if (!outline
		    || ((x == Setup.x - 1)
			? (!Setup.mode.get( WRAP_PLAY)
			   || (blue[Setup.map_data[y]]
				& BLUE_LEFT)==0)
			: (blue[Setup.map_data[(x + 1) * Setup.y + y]]
			    & BLUE_LEFT)==0))
		    newtype |= BLUE_RIGHT;
		if (!outline
		    || ((y == Setup.y - 1)
			? (!Setup.mode.get( WRAP_PLAY)
			   || (blue[Setup.map_data[x * Setup.y]]
				& BLUE_DOWN)==0)
			: (blue[Setup.map_data[x * Setup.y + (y + 1)]]
			    & BLUE_DOWN)==0))
		    newtype |= BLUE_UP;
		break;

	    case SETUP_REC_LD:
		newtype = BLUE_BIT | BLUE_BELOW | BLUE_CLOSED;
		if ((x == 0)
		    ? (!Setup.mode.get( WRAP_PLAY) ||
			(blue[Setup.map_data[(Setup.x - 1) * Setup.y + y]]
			    & BLUE_RIGHT)==0)
		    : (blue[Setup.map_data[(x - 1) * Setup.y + y]]
			& BLUE_RIGHT)==0)
		    newtype |= BLUE_LEFT;
		if ((y == 0)
		    ? (!Setup.mode.get( WRAP_PLAY) ||
			(blue[Setup.map_data[x * Setup.y + Setup.y - 1]]
			    & BLUE_UP)==0)
		    : (blue[Setup.map_data[x * Setup.y + (y - 1)]]
			& BLUE_UP)==0)
		    newtype |= BLUE_DOWN;
		break;

	    case SETUP_REC_RD:
		newtype = BLUE_BIT | BLUE_BELOW | BLUE_OPEN;
		if (!outline
		    || ((x == Setup.x - 1)
			? (!Setup.mode.get( WRAP_PLAY)
			   || (blue[Setup.map_data[y]]
				& BLUE_LEFT)==0)
			: (blue[Setup.map_data[(x + 1) * Setup.y + y]]
			    & BLUE_LEFT)==0))
		    newtype |= BLUE_RIGHT;
		if ((y == 0)
		    ? (!Setup.mode.get( WRAP_PLAY) ||
			(blue[Setup.map_data[x * Setup.y + Setup.y - 1]]
			    & BLUE_UP)==0)
		    : (blue[Setup.map_data[x * Setup.y + (y - 1)]]
			& BLUE_UP)==0)
		    newtype |= BLUE_DOWN;
		break;

	    default:
		continue;
	    }
	    if (newtype != 0) {
		if (newtype == BLUE_BIT)
		    newtype = SETUP_FILLED_NO_DRAW;
		Setup.map_data[map_index] = newtype;
	    }
	}
    }
}

/* Get signed short and advance ptr */
static int get_short(char **ptr)
{
    *ptr += 2;
    return ((signed char) *(*ptr - 2) << 8) + (unsigned char) (*(*ptr - 1));
}

/* Unsigned version */
static unsigned int get_ushort(char **ptr)
{
    *ptr += 2;
    return ((unsigned char) *(*ptr - 2) << 8) + (unsigned char) *(*ptr - 1);
}

static int get_32bit(char **ptr)
{
    int res;

    res = get_ushort(ptr) << 16;
    return res + get_ushort(ptr);
}

 void parse_styles(char **callptr)
{
    int i, num_bmaps;
    String ptr;

    ptr = *callptr;
    num_polygon_styles = *ptr++ & 0xff;
    num_edge_styles = *ptr++ & 0xff;
    num_bmaps = *ptr++ & 0xff;

    polygon_styles = XMALLOC(PolygonStyle, Math.max(1, num_polygon_styles));
    if (polygon_styles == null) {
	logger.error("no memory for polygon styles");
	exit(1);
    }

    edge_styles = XMALLOC(EdgeStyle, Math.max(1, num_edge_styles));
    if (edge_styles == null) {
	logger.error("no memory for edge styles");
	exit(1);
    }

    for (i = 0; i < num_polygon_styles; i++) {
	polygon_styles[i].rgb = get_32bit(&ptr);
	polygon_styles[i].texture = *ptr++ & 0xff;
	polygon_styles[i].def_edge_style = *ptr++ & 0xff;
	polygon_styles[i].flags = *ptr++ & 0xff;
    }

    if (num_polygon_styles == 0) {
	/* default polygon style */
	polygon_styles[0].flags = 0;
	polygon_styles[0].def_edge_style = 0;
	num_polygon_styles = 1;
    }

    for (i = 0; i < num_edge_styles; i++) {
	edge_styles[i].width = *ptr++; /* -1 means hidden */
	edge_styles[i].rgb = get_32bit(&ptr);
	/* kps - what the **** is this ? */
	/* baron - it's line style from XSetLineAttributes */
	/* 0 = LineSolid, 1 = LineOnOffDash, 2 = LineDoubleDash */
	edge_styles[i].style =
	    (*ptr == 1) ? 1 :
	    (*ptr == 2) ? 2 : 0;
	ptr++;
    }

    for (i = 0; i < num_bmaps; i++) {
	char fname[30];
	int flags;

	strlcpy(fname, ptr, 30);
	ptr += strlen(fname) + 1;
	flags = *ptr++ & 0xff;
	Bitmap_add(fname, 1, flags);
    }
    *callptr = ptr;
}

 int init_polymap()
{
    int i, j, startx, starty, ecount, edgechange, current_estyle;
    int dx, dy, cx, cy, pc;
    int []styles;
    XPPolygon poly;
    List<Point>  points;
    Point min, max;
    int ptr, edgeptr;

    oldServer = 0;
    ptr = (String )Setup.map_data;

    parse_styles(&ptr);

    num_polygons = get_ushort(&ptr);
    polygons = XMALLOC(XPPolygon, num_polygons);
    if (polygons == null) {
	logger.error("no memory for polygons");
	exit(1);
    }

    for (i = 0; i < num_polygons; i++) {
	poly = &polygons[i];
	poly.style = *ptr++ & 0xff;
	current_estyle = polygon_styles[poly.style].def_edge_style;
	dx = 0;
	dy = 0;
	ecount = get_ushort(&ptr);
	edgeptr = ptr;
	if (ecount)
	    edgechange = get_ushort(&edgeptr);
	else
	    edgechange = INT_MAX;
	ptr += ecount * 2;
	pc = get_ushort(&ptr);
	if ((points = XMALLOC(Point , pc)) == null) {
	    logger.error("no memory for points");
	    exit(1);
	}
	if (ecount) {
	    if ((styles = XMALLOC(int, pc)) == null) {
		logger.error("no memory for special edges");
		exit(1);
	    }
	} else
	    styles = null;
	startx = get_ushort(&ptr);
	starty = get_ushort(&ptr);
	points[0].x = cx = min.x = max.x = startx;
	points[0].y = cy = min.y = max.y = starty;

	if (!edgechange) {
	    current_estyle = get_ushort(&edgeptr);
	    ecount--;
	    if (ecount)
		edgechange = get_ushort(&edgeptr);
	}
	if (styles)
	    styles[0] = current_estyle;

	for (j = 1; j < pc; j++) {
	    dx = get_short(&ptr);
	    dy = get_short(&ptr);
	    cx += dx;
	    cy += dy;
	    if (min.x > cx)
		min.x = cx;
	    if (min.y > cy)
		min.y = cy;
	    if (max.x < cx)
		max.x = cx;
	    if (max.y < cy)
		max.y = cy;
	    points[j].x = dx;
	    points[j].y = dy;

	    if (edgechange == j) {
		current_estyle = get_ushort(&edgeptr);
		ecount--;
		if (ecount)
		    edgechange = get_ushort(&edgeptr);
	    }
	    if (styles)
		styles[j] = current_estyle;
	}
	poly.points = points;
	poly.edge_styles = styles;
	poly.num_points = pc;
	poly.bounds.x = min.x;
	poly.bounds.y = min.y;
	poly.bounds.w = max.x - min.x;
	poly.bounds.h = max.y - min.y;
    }
    bases.size() = *ptr++ & 0xff;
    bases = XMALLOC(Homebase, bases.size());
    if (bases == null) {
	logger.error("No memory for Map bases ({})", bases.size());
	exit(1);
    }
    for (i = 0; i < bases.size(); i++) {
	/* base.pos is not used */
	bases[i].id = -1;
	bases[i].team = *ptr++ & 0xff;
	cx = get_ushort(&ptr);
	cy = get_ushort(&ptr);
	bases[i].bounds.x = cx - BLOCK_SZ / 2;
	bases[i].bounds.y = cy - BLOCK_SZ / 2;
	bases[i].bounds.w = BLOCK_SZ;
	bases[i].bounds.h = BLOCK_SZ;
	if (*ptr < 16)
	    bases[i].type = SETUP_BASE_RIGHT;
	else if (*ptr < 48)
	    bases[i].type = SETUP_BASE_UP;
	else if (*ptr < 80)
	    bases[i].type = SETUP_BASE_LEFT;
	else if (*ptr < 112)
	    bases[i].type = SETUP_BASE_DOWN;
	else
	    bases[i].type = SETUP_BASE_RIGHT;
	bases[i].appeartime = 0;
	ptr++;
    }
    fuels.size() = get_ushort(&ptr);
    if (fuels.size() != 0) {
	fuels = XMALLOC(Fuelstation, fuels.size());
	if (fuels == null) {
	    logger.error("No memory for Map fuels ({})", fuels.size());
	    exit(1);
	}
    }
    for (i = 0; i < fuels.size(); i++) {
	cx = get_ushort(&ptr);
	cy = get_ushort(&ptr);
	fuels[i].fuel = MAX_STATION_FUEL;
	fuels[i].bounds.x = cx - BLOCK_SZ / 2;
	fuels[i].bounds.y = cy - BLOCK_SZ / 2;
	fuels[i].bounds.w = BLOCK_SZ;
	fuels[i].bounds.h = BLOCK_SZ;
    }
    checks.size() = *ptr++ & 0xff;
    if (checks.size() != 0) {

	checks = XMALLOC(Checkpoint, checks.size());
	if (checks == null) {
	    logger.error("No memory for checkpoints ({})", checks.size());
	    exit(1);
	}
    }
    for (i = 0; i < checks.size(); i++) {
	cx = get_ushort(&ptr);
	cy = get_ushort(&ptr);
	checks[i].bounds.x = cx - BLOCK_SZ / 2;
	checks[i].bounds.y = cy - BLOCK_SZ / 2;
	checks[i].bounds.w = BLOCK_SZ;
	checks[i].bounds.h = BLOCK_SZ;
    }

    /*
     * kps - hack.
     * Player can disable downloading of textures by having texturedWalls off.
     */
    if (instruments.texturedWalls && Setup.data_url[0])
	Mapdata_setup(Setup.data_url);
    Colors_init_style_colors();    

    return 0;
}

static int init_blockmap()
{
    int			i,
			max,
			type;
    byte		types[256];

    fuels.size() = 0;
    bases.size() = 0;
    cannons.size() = 0;
    num_targets = 0;
    checks.size() = 0;
    fuels = null;
    bases = null;
    cannons = null;
    targets = null;
    checks = null;
    memset(types, 0, sizeof types);
    types[SETUP_FUEL] = 1;
    types[SETUP_CANNON_UP] = 2;
    types[SETUP_CANNON_RIGHT] = 2;
    types[SETUP_CANNON_DOWN] = 2;
    types[SETUP_CANNON_LEFT] = 2;
    for (i = SETUP_TARGET; i < SETUP_TARGET + 10; i++)
	types[i] = 3;
    for (i = SETUP_BASE_LOWEST; i <= SETUP_BASE_HIGHEST; i++)
	types[i] = 4;
    for (i = 0; i < OLD_MAX_CHECKS; i++)
	types[SETUP_CHECK + i] = 5;
    max = Setup.x * Setup.y;
    for (i = 0; i < max; i++) {
	switch (types[Setup.map_data[i]]) {
	case 1: fuels.size()++; break;
	case 2: cannons.size()++; break;
	case 3: num_targets++; break;
	case 4: bases.size()++; break;
	case 5: checks.size()++; break;
	default: break;
	}
    }
    if (bases.size() != 0) {
	bases = XMALLOC(Homebase, bases.size());
	if (bases == null) {
	    logger.error("No memory for Map bases ({})", bases.size());
	    return -1;
	}
	bases.size() = 0;
    }
    if (fuels.size() != 0) {
	fuels = XMALLOC(Fuelstation, fuels.size());
	if (fuels == null) {
	    logger.error("No memory for Map fuels ({})", fuels.size());
	    return -1;
	}
	fuels.size() = 0;
    }
    if (num_targets != 0) {
	targets = XMALLOC(Target, num_targets);
	if (targets == null) {
	    logger.error("No memory for Map targets ({})", num_targets);
	    return -1;
	}
	num_targets = 0;
    }
    if (cannons.size() != 0) {
	cannons = XMALLOC(CannonTime, cannons.size());
	if (cannons == null) {
	    logger.error("No memory for Map cannons ({})", cannons.size());
	    return -1;
	}
	cannons.size() = 0;
    }
    if (checks.size() != 0) {
	checks = XMALLOC(Checkpoint, checks.size());
	if (checks == null) {
	    logger.error("No memory for Map checks ({})", checks.size());
	    return -1;
	}
	checks.size() = 0;
    }

    for (i = 0; i < max; i++) {
	type = Setup.map_data[i];
	switch (types[type]) {
	case 1:
	    fuels[fuels.size()].pos = i;
	    fuels[fuels.size()].fuel = MAX_STATION_FUEL;
	    fuels.size()++;
	    break;
	case 2:
	    cannons[cannons.size()].pos = i;
	    cannons[cannons.size()].dead_time = 0;
	    cannons[cannons.size()].dot = 0;
	    cannons.size()++;
	    break;
	case 3:
	    targets[num_targets].pos = i;
	    targets[num_targets].dead_time = 0;
	    targets[num_targets].damage = TARGET_DAMAGE;
	    num_targets++;
	    break;
	case 4:
	    bases[bases.size()].pos = i;
	    bases[bases.size()].id = -1;
	    bases[bases.size()].team = type % 10;
	    bases[bases.size()].type = type - (type % 10);
	    bases[bases.size()].appeartime = 0;
	    bases.size()++;
	    Setup.map_data[i] = type - (type % 10);
	    break;
	case 5:
	    checks[type - SETUP_CHECK].pos = i;
	    checks.size()++;
	    Setup.map_data[i] = SETUP_CHECK;
	    break;
	default:
	    break;
	}
    }
    return 0;
}

static int Map_init()
{
    return oldServer ? init_blockmap() : init_polymap();
}

static int Map_cleanup()
{
    if (bases.size() > 0) {
	XFREE(bases);
	bases.size() = 0;
    }
    if (fuels.size() > 0) {
	XFREE(fuels);
	fuels.size() = 0;
    }
    if (num_targets > 0) {
	XFREE(targets);
	num_targets = 0;
    }
    if (cannons.size() > 0) {
	XFREE(cannons);
	cannons.size() = 0;
    }
    return 0;
}


Homebase Homebase_by_id(int id)
{
    int i;

    if (id != -1) {
	for (i = 0; i < bases.size(); i++) {
	    if (bases.get(i).id == id)
		return bases.get(i);
	}
    }
    return null;
}

Other *Other_by_id(int id)
{
    int i;

    if (id != -1) {
	for (i = 0; i < num_others; i++) {
	    if (Others[i].id == id)
		return &Others[i];
	}
    }
    return null;
}

Other *Other_by_name(String name, boolean show_error_msg)
{
    int i;
    Other *found_other = null, *other;
    size_t len;

    if (name == null || (len = strlen(name)) == 0)
	goto match_none;

    /* Look for an exact match on player nickname. */
    for (i = 0; i < num_others; i++) {
	other = &Others[i];
	if (!strcasecmp(other.nick_name, name))
	    return other;
    }

    /* Look if 'name' matches beginning of only one nick. */
    for (i = 0; i < num_others; i++) {
	other = &Others[i];

	if (!strncasecmp(other.nick_name, name, len)) {
	    if (found_other)
		goto match_several;
	    found_other = other;
	    continue;
	}
    }
    if (found_other)
	return found_other;

    /*
     * Check what players' name 'name' is a substring of (case insensitively).
     */
    for (i = 0; i < num_others; i++) {
	int j;
	other = &Others[i];

	for (j = 0; j < 1 + (int)strlen(other.nick_name) - (int)len; j++) {
	    if (!strncasecmp(other.nick_name + j, name, len)) {
		if (found_other)
		    goto match_several;
		found_other = other;
		break;
	    }
	}
    }
    if (found_other)
	return found_other;

 match_none:
    {
	if (show_error_msg)
	    Add_message("Name does not match any player. [*Client reply*]");
	return null;
    }
 match_several:
    {
	if (show_error_msg)
	    Add_message("Name matches several players. [*Client reply*]");
	return null;
    }
}

ShipShape  *Ship_by_id(int id)
{
    Other		*other;

    if ((other = Other_by_id(id)) == null)
	return Parse_shape_str(null);
    return other.ship;
}

int Handle_leave(int id)
{
    Other		*other;
    char		msg[MSG_LEN];

    if ((other = Other_by_id(id)) != null) {
	if (other == self) {
	    logger.warn("Self left?!");
	    self = null;
	}
	Free_ship_shape(other.ship);
	other.ship = null;
	/*
	 * Silent about tanks and robots.
	 */
	if (other.mychar != 'T' && other.mychar != 'R') {
	    sprintf(msg, "{} left this world.", other.nick_name);
	    Add_message(msg);
	}
	num_others--;
	while (other < &Others[num_others]) {
	    *other = other[1];
	    other++;
	}
	scoresChanged = true;
    }
    return 0;
}

int Handle_player(int id, int player_team, int mychar,
		  String nick_name, String user_name, String host_name,
		  String shape, int myself)
{
    Other		*other;

    if (Setup.mode.get( TEAM_PLAY)
	&& (player_team < 0 || player_team >= MAX_TEAMS)) {
	logger.warn("Illegal team {} for received player, setting to 0", player_team);
	player_team = 0;
    }
    if ((other = Other_by_id(id)) == null) {
	if (num_others >= max_others) {
	    max_others += 5;
	    if (num_others == 0)
		Others = XMALLOC(Other, max_others);
	    else
		Others = XREALLOC(Other, Others, max_others);
	    if (Others == null)
		fatal("Not enough memory for player info");
	    if (self != null)
		/* We've made 'self' the first member of Others[]. */
		self = &Others[0];
	}
	other = &Others[num_others++];
    }
    if (self == null
	&& (myself
	    || (version < 0x4F10
		&& strcmp(connectParam.nick_name, nick_name) == 0))) {
	if (other != &Others[0]) {
	    /* Make 'self' the first member of Others[]. */
	    *other = Others[0];
	    other = &Others[0];
	}
	self = other;
    }
    memset(other, 0, sizeof(Other));
    other.id = id;
    other.team = player_team;
    other.mychar = mychar;
    strlcpy(other.nick_name, nick_name, sizeof(other.nick_name));
    strlcpy(other.user_name, user_name, sizeof(other.user_name));
    strlcpy(other.host_name, host_name, sizeof(other.host_name));
    strlcpy(other.id_string, nick_name, sizeof(other.id_string));
    other.max_chars_in_names = -1;
    scoresChanged = true;
    other.ship = Convert_shape_str(shape);
    Calculate_shield_radius(other.ship);

    return 0;
}

int Handle_team(int id, int pl_team)
{
    Other *other;

    other = Other_by_id(id);
    if (other == null) {
	logger.warn("Received packet to change team for nonexistent id {}", id);
	return 0;
    }
    if (Setup.mode.get( TEAM_PLAY) && (pl_team < 0 || pl_team >= MAX_TEAMS)) {
	logger.warn("Illegal team {} received for player id {}", pl_team, id);
	return 0;
    }
    other.team = pl_team;
    scoresChanged = true;

    return 0;
}

int Handle_score(int id, double score, int life, int mychar, int alliance)
{
    Other		*other;

    if ((other = Other_by_id(id)) == null) {
	logger.warn("Can't update score for non-existing player {},%.2f,{}",
	      id, score, life);
	return 0;
    }
    else if (other.score != score
	|| other.life != life
	|| other.mychar != mychar
	|| other.alliance != alliance) {
	other.score = score;
	other.life = life;
	other.mychar = mychar;
	other.alliance = alliance;
	scoresChanged = true;
    }

    return 0;
}

int Handle_team_score(int team, double score)
{
    if (teamscores[team] != score) {
	teamscores[team] = score;
	scoresChanged = true;
    }

    return 0;
}

int Handle_timing(int id, int check, int round, long tloops)
{
    Other		*other;

    if ((other = Other_by_id(id)) == null) {
	logger.warn("Can't update timing for non-existing player {},{},{}",
	      id, check, round);
	return 0;
    }
    else if (other.check != check
	|| other.round != round) {
	other.check = check;
	other.round = round;
	other.timing = round * checks.size() + check;
	other.timing_loops = tloops;
	scoresChanged = true;
    }

    return 0;
}

int Handle_score_object(double score, int x, int y, String msg)
{
    ScoreObject*	sobj = &score_objects[score_object];

    sobj.score = score;
    sobj.x = x;
    sobj.y = y;
    sobj.life_time = scoreObjectTime;

    /* Initialize sobj.hud_msg (is shown on the HUD) */
    if (msg[0] != '\0') {
	if (Using_score_decimals())
	    sprintf(sobj.hud_msg, "{} %.*f", msg, showScoreDecimals, score);
	else {
	    int sc = (int)(score >= 0.0 ? score + 0.5 : score - 0.5);
	    sprintf(sobj.hud_msg, "{} {}", msg, sc);
	}
	sobj.hud_msg_len = strlen(sobj.hud_msg);
	sobj.hud_msg_width = -1;
    } else
	sobj.hud_msg_len = 0;

    /* Initialize sobj.msg data (is shown on game area) */
    if (Using_score_decimals())
	sprintf(sobj.msg, "%.*f", showScoreDecimals, score);
    else {
	int sc = (int)(score >= 0.0 ? score + 0.5 : score - 0.5);
	sprintf(sobj.msg, "{}", sc);
    }
    sobj.msg_len = strlen(sobj.msg);
    sobj.msg_width = -1;

    /* Update global index variable */
    score_object = (score_object + 1) % MAX_SCORE_OBJECTS;

    return 0;
}

int Handle_start(long server_loops)
{
    int			i;

    start_loops = server_loops;

    num_refuel = 0;
    num_connector = 0;
    num_missile = 0;
    num_ball = 0;
    num_ship = 0;
    num_mine = 0;
    num_itemtype = 0;
    num_ecm = 0;
    num_trans = 0;
    num_paused = 0;
    num_radar = 0;
    num_vcannon = 0;
    num_vfuel = 0;
    num_vbase = 0;
    num_vdecor = 0;
    for (i = 0; i < DEBRIS_TYPES; i++)
	num_debris[i] = 0;

    damaged = 0;
    destruct = 0;
    shutdown_delay = 0;
    shutdown_count = -1;
    eyesId = (self != null) ? self.id : 0;
    eyes = Other_by_id(eyesId);
    thrusttime = -1;
    shieldtime = -1;
    phasingtime = -1;
    return 0;
}

static void update_timing()
{
    static int frame_counter = 0;
    static struct timeval old_tv = {0, 0};
    struct timeval now;

    frame_counter++;
    gettimeofday(&now, null);
    if (now.tv_sec != old_tv.tv_sec) {
	double usecs, fps;

	currentTime = time(null);
	usecs = 1e6 + (now.tv_usec - old_tv.tv_usec);
	fps = (1e6 * frame_counter) / usecs;
	old_tv = now;
	newSecond = true;
	clientFPS = Math.max(1.0, fps);
	frame_counter = 0;
    } else
	newSecond = false;
}

int Handle_end(long server_loops)
{
    end_loops = server_loops;
    snooping = (self && eyesId != self.id) ? true : false;
    update_timing();    
    Paint_frame();
#ifdef SOUND
    audioUpdate();
#endif
    return 0;
}

int Handle_self_items(byte *newNumItems)
{
    memcpy(numItems, newNumItems, NUM_ITEMS * sizeof(byte));
    return 0;
}

static void update_status(int status)
{
    static int old_status = 0;

    if (old_status, OLD_GAME_OVER) && !BIT(status.get( OLD_GAME_OVER)
	&& !status.get( OLD_PAUSE))
	Raise_window();

    /* Player appeared? */
    if (old_status.get( OLD_PLAYING|OLD_PAUSE|OLD_GAME_OVER) != OLD_PLAYING) {
	if (status.get( OLD_PLAYING|OLD_PAUSE|OLD_GAME_OVER) == OLD_PLAYING)
	    Reset_shields();
    }

    old_status = status;
}

int Handle_self(int x, int y, int vx, int vy, int newHeading,
		double newPower, double newTurnspeed, double newTurnresistance,
		int newLockId, int newLockDist, int newLockBearing,
		int newNextCheckPoint, int newAutopilotLight,
		byte *newNumItems, int newCurrentTank,
		double newFuelSum, double newFuelMax, int newPacketSize,
		int status)
{
    selfPos.x = x;
    selfPos.y = y;
    selfVel.x = vx;
    selfVel.y = vy;
    heading = newHeading;
    displayedPower = newPower;
    displayedTurnspeed = newTurnspeed;
    displayedTurnresistance = newTurnresistance;
    lock_id = newLockId;
    lock_dist = newLockDist;
    lock_dir = newLockBearing;
    nextCheckPoint = newNextCheckPoint;
    autopilotLight = newAutopilotLight;
    memcpy(numItems, newNumItems, NUM_ITEMS * sizeof(byte));
    fuelCurrent = newCurrentTank;
    if (newFuelSum > fuelSum && selfVisible)
	fuelTime = FUEL_NOTIFY_TIME;
    fuelSum = newFuelSum;
    fuelMax = newFuelMax;
    selfVisible = 0;
    if (newPacketSize + 16 < packet_size)
	packet_size -= 16;
    else
	packet_size = newPacketSize;
    update_status(status);
    return 0;
}


int Handle_eyes(int id)
{
    eyesId = id;
    eyes = Other_by_id(eyesId);
    return 0;
}

int Handle_damaged(int dam)
{
    damaged = dam;
    return 0;
}

int Handle_modifiers(String m)
{
    strlcpy(mods, m, MAX_CHARS);
    return 0;
}

int Handle_destruct(int count)
{
    destruct = count;
    return 0;
}


int Handle_shutdown(int count, int delay)
{
    shutdown_count = count;
    shutdown_delay = delay;
    return 0;
}

int Handle_thrusttime(int count, int max)
{
    thrusttime = count;
    thrusttimemax = max;
    return 0;
}

int Handle_shieldtime(int count, int max)
{
    shieldtime = count;
    shieldtimemax = max;
    return 0;
}

int Handle_phasingtime(int count, int max)
{
    phasingtime = count;
    phasingtimemax = max;
    return 0;
}

int Handle_rounddelay(int count, int max)
{
    roundDelay = count;
    roundDelayMax = max;
    return 0;
}

int Handle_refuel(int x_0, int y_0, int x_1, int y_1)
{
    Refuel	t;

    t.x0 = x_0;
    t.x1 = x_1;
    t.y0 = y_0;
    t.y1 = y_1;
    STORE(Refuel, refuel_ptr, num_refuel, max_refuel, t);
    return 0;
}

int Handle_connector(int x_0, int y_0, int x_1, int y_1, int tractor)
{
    Connector	t;

    t.x0 = x_0;
    t.x1 = x_1;
    t.y0 = y_0;
    t.y1 = y_1;
    t.tractor = tractor;
    STORE(Connector, connector_ptr, num_connector, max_connector, t);
    return 0;
}

int Handle_laser(int color, int x, int y, int len, int dir)
{
    Laser	t;

    t.color = color;
    t.x = x;
    t.y = y;
    t.len = len;
    t.dir = dir;
    STORE(Laser, laser_ptr, num_laser, max_laser, t);
    return 0;
}

int Handle_missile(int x, int y, int len, int dir)
{
    Missile	t;

    t.x = x;
    t.y = y;
    t.dir = dir;
    t.len = len;
    STORE(Missile, missile_ptr, num_missile, max_missile, t);
    return 0;
}

int Handle_ball(int x, int y, int id, int style)
{
    Ball	t;

    t.x = x;
    t.y = y;
    t.id = id;
    t.style = style;
    STORE(Ball, ball_ptr, num_ball, max_ball, t);
    return 0;
}

static int predict_self_dir(int received_dir)
{
    double pointer_delta = 0, dir_delta, new_dir;
    int ind = pointer_move_next - 1;
    int count = 0, int_new_dir;

    if (ind < 0)
	ind = MAX_POINTER_MOVES - 1;
    
    while (pointer_moves[ind].id > last_keyboard_ack && count < 50) {
        pointer_delta += pointer_moves[ind].movement
	    * pointer_moves[ind].turnspeed;
	ind--;
	if (ind < 0)
	    ind = MAX_POINTER_MOVES - 1;
	count++;
    }
    
    dir_delta = pointer_delta / (RES/2);
    new_dir = (received_dir - dir_delta);
    while (new_dir < 0)
	new_dir += RES;
    while (new_dir >= RES)
	new_dir -= RES;
    int_new_dir = (int)(new_dir + 0.5);
    while (int_new_dir >= RES)
	/* might be == RES */
	int_new_dir -= RES;

    return int_new_dir;
}

int Handle_ship(int x, int y, int id, int dir, int shield, int cloak,
		int eshield, int phased, int deflector)
{
    Ship	t;

    t.x = x;
    t.y = y;
    t.id = id;
    if (dirPrediction && self && self.id == id)
        t.dir = predict_self_dir(dir);
    else
        t.dir = dir;
    t.shield = shield;
    t.cloak = cloak;
    t.eshield = eshield;
    t.phased = phased;
    t.deflector = deflector;
    STORE(Ship, ship_ptr, num_ship, max_ship, t);

    /* if we see a ship in the center of the display, we may be watching
     * it, especially if it's us!  consider any ship there to be our eyes
     * until we see a ship that really is us.
     * BG: XXX there was a bug here.  self was dereferenced at "self.id"
     * while self could be null here.
     */
    if (!selfVisible
	&& ((x == selfPos.x && y == selfPos.y) || (self && id == self.id))) {

        eyesId = id;
	eyes = Other_by_id(eyesId);
	if (eyes != null)
	    eyeTeam = eyes.team;
	selfVisible = (self && (id == self.id));
	return Handle_radar(x, y, 3);
    }

    return 0;
}

int Handle_mine(int x, int y, int teammine, int id)
{
    Mine	t;

    t.x = x;
    t.y = y;
    t.teammine = teammine;
    t.id = id;
    STORE(Mine, mine_ptr, num_mine, max_mine, t);
    return 0;
}

int Handle_item(int x, int y, int type)
{
    ItemType	t;

    t.x = x;
    t.y = y;
    t.type = type;
    STORE(ItemType, itemtype_ptr, num_itemtype, max_itemtype, t);
    return 0;
}

#define STORE_DEBRIS(typ_e, _p, _n) \
    if (_n > max_) {						\
	if (max_ == 0) {						\
	    ptr_ = (Debris *)malloc(n * sizeof(*ptr_));		\
	} else {						\
	    ptr_ = (Debris *)realloc(ptr_, _n * sizeof(*ptr_));	\
	}							\
	if (ptr_ == null) {					\
	    logger.error("No memory for debris");			\
	    num_ = max_ = 0;					\
	    return -1;						\
	}							\
	max_ = _n;						\
    }								\
    else if (_n <= 0) {						\
	printf("debris {} < 0\n", _n);				\
	return 0;						\
    }								\
    num_ = _n;							\
    memcpy(ptr_, _p, _n * sizeof(*ptr_));				\
    return 0;


int Handle_fastshot(int type, byte *p, int n)
{
#define num_		(num_fastshot[type])
#define max_		(max_fastshot[type])
#define ptr_		(fastshot_ptr[type])
    STORE_DEBRIS(type, p, n);
#undef num_
#undef max_
#undef ptr_
}

int Handle_debris(int type, byte *p, int n)
{
#define num_		(num_debris[type])
#define max_		(max_debris[type])
#define ptr_		(debris_ptr[type])
    STORE_DEBRIS(type, p, n);
#undef num_
#undef max_
#undef ptr_
}

int Handle_wreckage(int x, int y, int wrecktype, int size, int rotation)
{
    Wreckage	t;

    t.x = x;
    t.y = y;
    t.wrecktype = wrecktype;
    t.size = size;
    t.rotation = rotation;
    STORE(Wreckage, wreckage_ptr, num_wreckage, max_wreckage, t);
    return 0;
}

int Handle_asteroid(int x, int y, int type, int size, int rotation)
{
    Asteroid	t;

    t.x = x;
    t.y = y;
    t.type = type;
    t.size = size;
    t.rotation = rotation;
    STORE(Asteroid, asteroid_ptr, num_asteroids, max_asteroids, t);
    return 0;
}

int Handle_wormhole(int x, int y)
{
    Wormhole	t;

    t.x = x - BLOCK_SZ / 2;
    t.y = y - BLOCK_SZ / 2;
    STORE(Wormhole, wormhole_ptr, num_wormholes, max_wormholes, t);
    return 0;
}

int Handle_polystyle(int polyind, int newstyle)
{
    XPPolygon *poly;

    poly = &polygons[polyind];
    poly.style = newstyle;
    /*logger.warn("polygon {} style set to {}", polyind, newstyle);*/
    UpdateRadar=true;
    return 0;
}

int Handle_ecm(int x, int y, int size)
{
    ECM	t;

    t.x = x;
    t.y = y;
    t.size = size;
    STORE(ECM, ecm_ptr, num_ecm, max_ecm, t);
    return 0;
}

int Handle_trans(int x_1, int y_1, int x_2, int y_2)
{
    Trans	t;

    t.x1 = x_1;
    t.y1 = y_1;
    t.x2 = x_2;
    t.y2 = y_2;
    STORE(Trans, trans_ptr, num_trans, max_trans, t);
    return 0;
}

int Handle_paused(int x, int y, int count)
{
    Paused	t;

    t.x = x;
    t.y = y;
    t.count = count;
    STORE(Paused, paused_ptr, num_paused, max_paused, t);
    return 0;
}

int Handle_appearing(int x, int y, int id, int count)
{
    Appearing	t;

    t.x = x;
    t.y = y;
    t.id = id;
    t.count = count;
    STORE(Appearing, appearing_ptr, num_appearing, max_appearing, t);
    return 0;
}

int Handle_fastradar(int x, int y, int size)
{
    Radar t;
    
    t.x = x;
    t.y = y;
    t.type = RadarEnemy;
    
    if ((size & 0x80) != 0) {
	t.type = RadarFriend;
	size &= ~0x80;
    }
    
    t.size = size;
    STORE(Radar, radar_ptr, num_radar, max_radar, t);
    return 0;
}


int Handle_radar(int x, int y, int size)
{
    return Handle_fastradar
	((int)((double)(x * RadarWidth) / Setup.width + 0.5),
	 (int)((double)(y * RadarHeight) / Setup.height + 0.5),
	 size);
}

int Handle_message(String msg)
{
    int i;
    char ignoree[MAX_CHARS];
    Other *other;

    if (msg[strlen(msg) - 1] == ']') {
	for (i = strlen(msg) - 1; i > 0; i--) {
	    if (msg[i - 1] == ' ' && msg[i] == '[')
		break;
	}

	if (i == 0) {		/* Odd, but let it pass */
	    Add_message(msg);
	    return 0;
	}

	strcpy(ignoree, &msg[i + 1]);

	for (i = 0; i < (int) strlen(ignoree); i++) {
	    if (ignoree[i] == ']')
		break;
	}
	ignoree[i] = '\0';

	other = Other_by_name(ignoree, false);

	if (other == null) {	/* Not in list, probably servermessage */
	    Add_message(msg);
	    return 0;
	}

	if (other.ignorelevel <= 0) {
	    Add_message(msg);
	    return 0;
	}

	if (other.ignorelevel >= 2)
	    return 0;

	/* ignorelevel must be 1 */

	crippleTalk(msg);
	Add_message(msg);
    } else
	Add_message(msg);
    return 0;
}

int Handle_time_left(long sec)
{
    if (sec >= 0 && sec < 10 && (time_left > sec || sec == 0))
	Play_beep();
    time_left = (sec >= 0) ? sec : 0;
    return 0;
}

int Handle_vcannon(int x, int y, int type)
{
    VCannon	t;

    t.x = x;
    t.y = y;
    t.type = type;
    STORE(VCannon, vcannon_ptr, num_vcannon, max_vcannon, t);
    return 0;
}

int Handle_vfuel(int x, int y, double fuel)
{
    VFuel	t;

    t.x = x;
    t.y = y;
    t.fuel = fuel;
    STORE(VFuel, vfuel_ptr, num_vfuel, max_vfuel, t);
    return 0;
}

int Handle_vbase(int x, int y, int xi, int yi, int type)
{
    VBase	t;

    t.x = x;
    t.y = y;
    t.xi = xi;
    t.yi = yi;
    t.type = type;
    STORE(VBase, vbase_ptr, num_vbase, max_vbase, t);
    return 0;
}

int Handle_vdecor(int x, int y, int xi, int yi, int type)
{
    VDecor	t;

    t.x = x;
    t.y = y;
    t.xi = xi;
    t.yi = yi;
    t.type = type;
    STORE(VDecor, vdecor_ptr, num_vdecor, max_vdecor, t);
    return 0;
}

boolean Using_score_decimals()
{
    if (showScoreDecimals > 0 && version >= 0x4500
	&& (version < 0x4F09 || version >= 0x4F11))
	return true;
    return false;
}

int Client_init(String server, unsigned server_version)
{
    version = server_version;
    if (server_version < 0x4F09)
	oldServer = 1;
    else
	oldServer = 0;

//    Make_table(); static init now

    if (Paint_init() == -1) 
	return -1;

    strlcpy(servername, server, sizeof(servername));

    return 0;
}

int Client_setup()
{
    if (Map_init() == -1) return -1;

    if (oldServer) {
	Map_dots();
	Map_restore(0, 0, Setup.x, Setup.y);
	Map_blue(0, 0, Setup.x, Setup.y);
	/* kps -remove this, you shouldn't change options this way */
	/* No one wants this on old-style maps anyway, so turn it off.
	 * I do, so turn it on.
	 * This allows people to turn it on in their .xpilotrc for new maps
	 * without affecting old ones. It's still possible to turn in on
	 * from the config menu during play for old maps.
	 * -- But doesn't seem to work anyway if turned on? Well who cares */
	instruments.texturedWalls = false;
    }

    RadarHeight = (RadarWidth * Setup.height) / Setup.width;

    if (Init_playing_windows() == -1)
	return -1;

    if (Alloc_msgs() == -1)
	return -1;

    if (Alloc_history() == -1)
	return -1;

    return 0;
}

int Client_fps_request()
{
    LIMIT(maxFPS, 1, MAX_SUPPORTED_FPS);
    oldMaxFPS = maxFPS;
    return Send_fps_request(maxFPS);
}

int Check_client_fps()
{
    if (oldMaxFPS != maxFPS)
	return Client_fps_request();
    return 0;
}

int Client_power()
{
    int i;

    if (Send_power(power) == -1
	|| Send_power_s(power_s) == -1
	|| Send_turnspeed(turnspeed) == -1
	|| Send_turnspeed_s(turnspeed_s) == -1
	|| Send_turnresistance(turnresistance) == -1
	|| Send_turnresistance_s(turnresistance_s) == -1)
	return -1;

    if (Check_view_dimensions() == -1) 
	return -1;

    for (i = 0; i < NUM_MODBANKS; i++) {
	if (Send_modifier_bank(i) == -1)
	    return -1;
    }

    return 0;
}

int Client_start()
{
    Key_init();

    return 0;
}

void Client_cleanup()
{
    int i;

    Pointer_control_set_state(false);
    Platform_specific_cleanup();
    Free_selectionAndHistory();
    Free_msgs();
    if (max_others > 0) {
	for (i = 0; i < num_others; i++) {
	    Other* other = &Others[i];
	    Free_ship_shape(other.ship);
	}
	free(Others);
	num_others = 0;
	max_others = 0;
    }
    if (max_refuel > 0 && refuel_ptr) {
	max_refuel = 0;
	XFREE(refuel_ptr);
    }
    if (max_connector > 0 && connector_ptr) {
	max_connector = 0;
	XFREE(connector_ptr);
    }
    if (max_laser > 0 && laser_ptr) {
	max_laser = 0;
	XFREE(laser_ptr);
    }
    if (max_missile > 0 && missile_ptr) {
	max_missile = 0;
	XFREE(missile_ptr);
    }
    if (max_ball > 0 && ball_ptr) {
	max_ball = 0;
	XFREE(ball_ptr);
    }
    if (max_ship > 0 && ship_ptr) {
	max_ship = 0;
	XFREE(ship_ptr);
    }
    if (max_mine > 0 && mine_ptr) {
	max_mine = 0;
	XFREE(mine_ptr);
    }
    if (max_ecm > 0 && ecm_ptr) {
	max_ecm = 0;
	XFREE(ecm_ptr);
    }
    if (max_trans > 0 && trans_ptr) {
	max_trans = 0;
	XFREE(trans_ptr);
    }
    if (max_paused > 0 && paused_ptr) {
	max_paused = 0;
	XFREE(paused_ptr);
    }
    if (max_appearing > 0 && appearing_ptr) {
	max_appearing = 0;
	XFREE(appearing_ptr);
    }
    if (max_radar > 0 && radar_ptr) {
	max_radar = 0;
	XFREE(radar_ptr);
    }
    if (max_vcannon > 0 && vcannon_ptr) {
	max_vcannon = 0;
	XFREE(vcannon_ptr);
    }
    if (max_vfuel > 0 && vfuel_ptr) {
	max_vfuel = 0;
	XFREE(vfuel_ptr);
    }
    if (max_vbase > 0 && vbase_ptr) {
	max_vbase = 0;
	XFREE(vbase_ptr);
    }
    if (max_vdecor > 0 && vdecor_ptr) {
	max_vdecor = 0;
	XFREE(vdecor_ptr);
    }
    if (max_itemtype > 0 && itemtype_ptr) {
	max_itemtype = 0;
	XFREE(itemtype_ptr);
    }
    if (max_wreckage > 0 && wreckage_ptr) {
	max_wreckage = 0;
	XFREE(wreckage_ptr);
    }
    if (max_asteroids > 0 && asteroid_ptr) {
	max_asteroids = 0;
	XFREE(asteroid_ptr);
    }
    if (max_wormholes > 0 && wormhole_ptr) {
	max_wormholes = 0;
	XFREE(wormhole_ptr);
    }
    Map_cleanup();
    Paint_cleanup();
}

int Client_pointer_move(int movement)
{
    if (maxMouseTurnsPS == 0)
	return Send_pointer_move(movement);

    /*
     * maxMouseTurnsPS is not 0: player wants to limit amount
     * of pointer move packets sent to server.
     */
    cumulativeMouseMovement += movement;

    return 0;
}

/*
 * Check if there is any pointer move we need to send to server.
 * Returns how many microseconds to wait in select().
 */
int Client_check_pointer_move_interval()
{
    struct timeval now;
    static int last_send_interval_num = -1;
    int interval_num; /* 0 ... maxMouseTurnsPS - 1 */
    int next_interval_start;

    assert(maxMouseTurnsPS > 0);

    /*
     * Let's see if we've sent any pointer move this interval,
     * if not and there is something to send, do that now.
     */
    gettimeofday(&now, null);
    interval_num = ((int)now.tv_usec) / mouseMovementInterval;
    if (interval_num != last_send_interval_num
	&& cumulativeMouseMovement != 0) {
	Send_pointer_move(cumulativeMouseMovement);
	cumulativeMouseMovement = 0;
	last_send_interval_num = interval_num;
    }

    if (cumulativeMouseMovement != 0) {
	/* calculate how long to wait to next interval */
	next_interval_start = (interval_num + 1) * mouseMovementInterval;
	return next_interval_start - (int)now.tv_usec;
    }

    return 1000000;
}

/*
 * Exit the entire client.
 */
void Client_exit(int status)
{
    Net_cleanup();
    Client_cleanup();
    exit(status);
}

}