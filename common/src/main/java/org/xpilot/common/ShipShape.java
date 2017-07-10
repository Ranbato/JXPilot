package org.xpilot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

import static org.xpilot.common.Const.MSG_LEN;
import static org.xpilot.common.Const.RES;
import static org.xpilot.common.Shape.*;
import static org.xpilot.common.XPMath.tcos;
import static org.xpilot.common.XPMath.tsin;

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
public class ShipShape {


 static final Logger logger = LoggerFactory.getLogger(ShipShape.class);

 private SShape[] shipShapes = new SShape[RES];

    ArrayList <Click> cashed_pts = new ArrayList(MAX_SHIP_PTS2);
    int		cashed_dir;
    String name;
    String author;

 static class SShape extends Shape {

	 Click engine = new Click();		/* Engine position */
	 Click m_gun = new Click();		/* Main gun position */

	 ArrayList<Click> l_gun = new ArrayList<>(),	/* Additional cannon positions, left*/
			 r_gun = new ArrayList<>(),	/* Additional cannon positions, right*/
			 l_rgun = new ArrayList<>(),	/* Additional rear cannon positions, left*/
			 r_rgun = new ArrayList<>();	/* Additional rear cannon positions, right*/

	 ArrayList<Click> l_light = new ArrayList<>(), /* Left and right light positions */
			 r_light = new ArrayList<>();
	 ArrayList<Click> m_rack = new ArrayList<>();
	 int shield_radius;		/* Radius of shield used by client. */



 }


static  Click  ipos2clpos(Point pos)
{
    Click  pt = new Click();

    pt.cx = pt.PIXEL_TO_CLICK(pos.x);
    pt.cy = pt.PIXEL_TO_CLICK(pos.y);

    return pt;
}

static Point2D clpos2position(Click  pt)
{
    Point2D pos = new Point2D.Float(pt.CLICK_TO_FLOAT(pt.cx),pt.CLICK_TO_FLOAT(pt.cy));
	
    return pos;
}



public   Click
Ship_get_point_clpos(int i, int dir)
{
    return shipShapes[dir].pts.get(i);
}
  Click
Ship_get_engine_clpos(int dir)
{
    return shipShapes[dir].engine;
}
  Click
Ship_get_m_gun_clpos(int dir)
{
    return shipShapes[dir].m_gun;
}
  Click
Ship_get_l_gun_clpos(int gun, int dir)
{
    return shipShapes[dir].l_gun.get(gun);
}
  Click
Ship_get_r_gun_clpos(int gun, int dir)
{
    return shipShapes[dir].r_gun.get(gun);
}
  Click
Ship_get_l_rgun_clpos( int gun, int dir)
{
    return shipShapes[dir].r_rgun.get(gun);
}
  Click
Ship_get_r_rgun_clpos( int gun, int dir)
{
    return shipShapes[dir].r_rgun.get(gun);
}
  Click
Ship_get_l_light_clpos( int l, int dir)
{
    return shipShapes[dir].l_light.get(l);
}
  Click
Ship_get_r_light_clpos( int l, int dir)
{
    return shipShapes[dir].r_light.get(l);
}
  Click
Ship_get_m_rack_clpos( int rack, int dir)
{
    return shipShapes[dir].m_rack.get(rack);
}


  Point2D
Ship_get_point_position( int i, int dir)
{
    return clpos2position(Ship_get_point_clpos( i, dir));
}
  Point2D
Ship_get_engine_position(  int dir)
{
    return clpos2position(Ship_get_engine_clpos( dir));
}
  Point2D
Ship_get_m_gun_position(int dir)
{
    return clpos2position(Ship_get_m_gun_clpos(dir));
}
  Point2D
Ship_get_l_gun_position( int gun, int dir)
{
    return clpos2position(Ship_get_l_gun_clpos( gun, dir));
}
  Point2D
Ship_get_r_gun_position( int gun, int dir)
{
    return clpos2position(Ship_get_r_gun_clpos( gun, dir));
}
  Point2D
Ship_get_l_rgun_position( int gun, int dir)
{
    return clpos2position(Ship_get_l_rgun_clpos( gun, dir));
}
  Point2D
Ship_get_r_rgun_position( int gun, int dir)
{
    return clpos2position(Ship_get_r_rgun_clpos( gun, dir));
}
  Point2D
Ship_get_l_light_position(  int l, int dir)
{
    return clpos2position(Ship_get_l_light_clpos( l, dir));
}
  Point2D
Ship_get_r_light_position(int l, int dir)
{
    return clpos2position(Ship_get_r_light_clpos(l, dir));
}
  Point2D
Ship_get_m_rack_position( int rack, int dir)
{
    return clpos2position(Ship_get_m_rack_clpos( rack, dir));
}

boolean 	debugShapeParsing = false;
boolean 	verboseShapeParsing = false;
boolean 	shapeLimits = true;
//extern boolean is_server;

 void Ship_set_point_ipos( int i, Point  pos)
{
    shipShapes[0].pts.set(i,ipos2clpos(pos));
}

 void Ship_set_engine_ipos( Point  pos)
{
    shipShapes[0].engine = ipos2clpos(pos);
}

 void Ship_set_m_gun_ipos( Point  pos)
{
    shipShapes[0].m_gun = ipos2clpos(pos);
}

 void Ship_set_l_gun_ipos( int i, Point  pos)
{
    shipShapes[0].l_gun.set(i, ipos2clpos(pos));
}

 void Ship_set_r_gun_ipos( int i, Point  pos)
{
    shipShapes[0].r_gun.set(i,ipos2clpos(pos));
}

 void Ship_set_l_rgun_ipos( int i, Point  pos)
{
    shipShapes[0].l_rgun.set(i,ipos2clpos(pos));
}

 void Ship_set_r_rgun_ipos( int i, Point  pos)
{
    shipShapes[0].r_rgun.set(i, ipos2clpos(pos));
}

 void Ship_set_l_light_ipos( int i, Point  pos)
{
    shipShapes[0].l_light.set(i, ipos2clpos(pos));
}

 void Ship_set_r_light_ipos( int i, Point  pos)
{
    shipShapes[0].r_light.set(i,ipos2clpos(pos));
}

 void Ship_set_m_rack_ipos( int i, Point  pos)
{
    shipShapes[0].m_rack.set(i, ipos2clpos(pos));
}


/* kps - tmp hack */
ArrayList<Click>  Shape_get_points( int dir)
{
 

    /* todo kps - optimize if cashed_dir == dir */
    cashed_pts = (ArrayList<Click>) shipShapes[dir].pts.clone();

    return cashed_pts;
}

Click Rotate_point(Click  pt,int  dir)
{
    double cx, cy;
    Click ret = new Click();

	cx = tcos(dir) * pt.cx - tsin(dir) * pt.cy;
	cy = tsin(dir) * pt.cx + tcos(dir) * pt.cy;
	ret.cx = (int) (cx >= 0.0 ? cx + 0.5 : cx - 0.5);
	ret.cy = (int) (cy >= 0.0 ? cy + 0.5 : cy - 0.5);
	
	return ret;

}

void Rotate_position(Point2D pt[])
{
    int i;

    for (i = 1; i < RES; i++) {
	pt[i].setLocation( tcos(i) * pt[0].getX() - tsin(i) * pt[0].getY(),
	tsin(i) * pt[0].getX() + tcos(i) * pt[0].getY());
    }
}

void Rotate_ship()
{
    int i;

    SShape original = shipShapes[0];
    for(int dir = 1;dir < RES;dir++) {
        for (i = 0; i < original.num_points; i++)
            shipShapes[dir].pts.set(i, Rotate_point(original.pts.get(i),dir));

        shipShapes[dir].engine = Rotate_point(original.engine,dir);
        shipShapes[dir].m_gun = Rotate_point(original.m_gun,dir);
        for (i = 0; i < original.l_gun.size(); i++)
            shipShapes[dir].l_gun.set(i,Rotate_point(original.l_gun.get(i),dir));
        for (i = 0; i < original.r_gun.size(); i++)
            shipShapes[dir].r_gun.set(i,Rotate_point(original.r_gun.get(i),dir));
        for (i = 0; i < original.l_rgun.size(); i++)
            shipShapes[dir].l_rgun.set(i,Rotate_point(original.l_rgun.get(i),dir));
        for (i = 0; i < original.r_rgun.size(); i++)
            shipShapes[dir].r_rgun.set(i,Rotate_point(original.r_rgun.get(i),dir));
        for (i = 0; i < original.l_light.size(); i++)
            shipShapes[dir].l_light.set(i,Rotate_point(original.l_light.get(i),dir));
        for (i = 0; i < original.r_light.size(); i++)
            shipShapes[dir].r_light.set(i,Rotate_point(original.r_light.get(i),dir));
        for (i = 0; i < original.m_rack.size(); i++)
            shipShapes[dir].m_rack.set(i,Rotate_point(original.m_rack.get(i),dir));
    }
}


/*
 * Default this ship
 */
void Default_ship()
{
    SShape sh = shipShapes[0];
     Click 	pt;

    if (sh.num_points == 0) {
	Point  pos = new Point();

	sh.num_points = 3;
    
	pos.x = 14;
	pos.y = 0;
	Ship_set_point_ipos( 0, pos);

	pos.x = -8;
	pos.y = 8;
	Ship_set_point_ipos( 1, pos);

	pos.x = -8;
	pos.y = -8;
	Ship_set_point_ipos( 2, pos);

	pos.x = -8;
	pos.y = 0;
	Ship_set_engine_ipos( pos);

	pos.x = 14;
	pos.y = 0;
	Ship_set_m_gun_ipos( pos);

	pos.x = -8;
	pos.y = 8;
	Ship_set_l_light_ipos( 0, pos);

	pos.x = -8;
	pos.y = -8;
	Ship_set_r_light_ipos( 0, pos);

	pos.x = 14;
	pos.y = 0;
	Ship_set_m_rack_ipos( 0, pos);

	sh.l_gun.clear();
	sh.r_gun.clear();
	sh.l_rgun.clear();
	sh.r_rgun.clear();

	Rotate_ship();
    }

}

static class Grid {
    int todo, done;
    byte pt[][] = new byte[32][32];
    Point chk[][] = new Point[32][32];


    /*
     * Functions to simplify limit-checking for ship points.
     */
    void Grid_reset() {
        todo = 0;
        done = 0;
        pt = new byte[32][32];
        chk = new Point[32][32];
    }

    void Grid_set_value(int x, int y, byte value) {
        assert (!(x < -15 || x > 15 || y < -15 || y > 15));
        pt[x + 15][y + 15] = value;
    }

    byte Grid_get_value(int x, int y) {
        if (x < -15 || x > 15 || y < -15 || y > 15)
            return 2;
        return pt[x + 15][y + 15];
    }

    void Grid_add(int x, int y) {
        Grid_set_value(x, y, (byte) 2);
        chk[todo].x = x + 15;
        chk[todo].y = y + 15;
        todo++;
    }

    Point Grid_get() {
        Point pos = new Point();

        pos.x = (int) chk[done].x - 15;
        pos.y = (int) chk[done].y - 15;
        done++;

        return pos;
    }

    boolean Grid_is_ready() {
        return (done >= todo) ? true : false;
    }

    boolean Grid_point_is_outside_ship(Point pt) {
        byte value = Grid_get_value(pt.x, pt.y);

        if (value == 2)
            return true;
        return false;
    }


    void Grid_print() {
        int x, y;

        logger.debug("============================================================");

        for (y = 0; y < 32; y++) {
            StringBuilder line = new StringBuilder(32);

            for (x = 0; x < 32; x++)
                line.append(pt[x][y]);
            logger.debug(line.toString());
        }

        logger.debug("------------------------------------------------------------");

        {
            Point pt = new Point();

            for (pt.y = -15; pt.y <= 15; pt.y++) {
                StringBuilder line = new StringBuilder(32);

                for (pt.x = -15; pt.x <= 15; pt.x++)
                    line.append(Grid_point_is_outside_ship(pt) ? '.' : '*');
                logger.debug(line.toString());
            }
        }


        logger.debug("\n");
    }
}

 int shape2wire(String ship_shape_str, ShipShape ship)
{
    Grid grid;
    int i, j, x, y, dx, dy, max, shape_version = 0;
    ArrayList<Point> pt = new ArrayList<>();
    Point in, old_in, engine, m_gun;
    ArrayList<Point>  l_light= new ArrayList<>();
    ArrayList<Point> r_light= new ArrayList<>();
    ArrayList<Point>  l_gun= new ArrayList<>();
    ArrayList<Point> r_gun= new ArrayList<>();
    ArrayList<Point>  l_rgun= new ArrayList<>();
    ArrayList<Point> r_rgun= new ArrayList<>();
    ArrayList<Point> m_rack= new ArrayList<>();
    boolean mainGunSet = false, engineSet = false;
     String teststr;
    String keyw;
    String buf;
    boolean remove_edge = false;



    if (debugShapeParsing)
	logger.warn("parsing shape: {}", ship_shape_str);

    String [] sections = ship_shape_str.split("[()]");
    for (String section:sections) {


        section = section.trim();

	if (shape_version == 0) {
	    if (Character.isDigit(section.charAt(0))) {
		if (verboseShapeParsing)
		   logger.warn("Ship shape is in obsolete 3.1 format.");
		return -1;
	    } else
		shape_version = 0x3200;
	}
    String [] types = section.split(":");

        keyw = types[0].trim();

        if(types.length != 2){
        if (verboseShapeParsing)
            logger.warn("Missing colon in ship shape: {}", keyw);
        continue;
    }

        String points[] = types[1].split("\\w+");

        switch (SSKeys.lookup(keyw)) {

        case SHAPE:
            for (String point:points ) {


            int [] coords = Arrays.stream(point.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
            if(coords.length != 2)
             {
		    if (verboseShapeParsing)
			logger.warn("Missing ship shape coordinate in: \"{}\"",
			     point);
		    break;
		}
		else{
                in = new Point(coords[0],coords[1]);
            }
		if (pt.size() >= MAX_SHIP_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many ship shape coordinates");
		} else {
		    if (pt.size() > 0
			&& old_in.x == in.x
			&& old_in.y == in.y) {
			remove_edge = true;
			if (verboseShapeParsing)
			   logger.warn("Duplicate ship point at {},{}, ignoring it.",
				 in.x, in.y);
		    }
		    else {
			pt.add(in);
			old_in = in;
			if (debugShapeParsing)
			   logger.warn("ship point at {},{}", in.x, in.y);
		    }
		}
		teststr = strchr(teststr, ' ');
	    }
	    if (ship.num_points > 0
		&& pt[ship.num_points - 1].x == pt[0].x
		&& pt[ship.num_points - 1].y == pt[0].y) {
		if (verboseShapeParsing)
		   logger.warn("Ship last point equals first point at %d,%d, "
			 "ignoring it.", pt[0].x, pt[0].y);
		remove_edge = true;
		ship.num_points--;
	    }

	    if (remove_edge && verboseShapeParsing)
		logger.warn("Removing ship edges with length 0.");
	    
	    break;

	case 1:		/* Keyword is 'mainGun' */
	    if (mainGunSet) {
		if (verboseShapeParsing)
		   logger.warn("Ship shape keyword \"%s\" multiple defined", keyw);
		break;
	    }
	    while (*teststr == ' ') teststr++;
	    if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		if (verboseShapeParsing)
		   logger.warn("Missing main gun coordinate in: \"%s\"", teststr);
	    } else {
		m_gun = in;
		mainGunSet = true;
		if (debugShapeParsing)
		   logger.warn("main gun at %d,%d", in.x, in.y);
	    }
	    break;

	case 2:		/* Keyword is 'leftGun' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing)
			logger.warn("Missing left gun coordinate in: \"%s\"",
			     teststr);
		    break;
		}
		if (ship.num_l_gun >= MAX_GUN_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many left gun coordinates");
		} else {
		    l_gun[ship.num_l_gun++] = in;
		    if (debugShapeParsing)
			logger.warn("left gun at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	case 3:		/* Keyword is 'rightGun' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing)
			logger.warn("Missing right gun coordinate in: \"%s\"",
			     teststr);
		    break;
		}
		if (ship.num_r_gun >= MAX_GUN_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many right gun coordinates");
		} else {
		    r_gun[ship.num_r_gun++] = in;
		    if (debugShapeParsing)
			logger.warn("right gun at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	case 4:		/* Keyword is 'leftLight' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing)
			logger.warn("Missing left light coordinate in: \"%s\"",
			     teststr);
		    break;
		}
		if (ship.num_l_light >= MAX_LIGHT_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many left light coordinates");
		} else {
		    l_light[ship.num_l_light++] = in;
		    if (debugShapeParsing)
			logger.warn("left light at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	case 5:		/* Keyword is 'rightLight' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing)
			logger.warn("Missing right light coordinate in: \"%s\"",
			       teststr);
		    break;
		}
		if (ship.num_r_light >= MAX_LIGHT_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many right light coordinates");
		} else {
		    r_light[ship.num_r_light++] = in;
		    if (debugShapeParsing)
			logger.warn("right light at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	case 6:		/* Keyword is 'engine' */
	    if (engineSet) {
		if (verboseShapeParsing)
		   logger.warn("Ship shape keyword \"%s\" multiple defined", keyw);
		break;
	    }
	    while (*teststr == ' ') teststr++;
	    if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		if (verboseShapeParsing)
		   logger.warn("Missing engine coordinate in: \"%s\"", teststr);
	    } else {
		engine = in;
		engineSet = true;
		if (debugShapeParsing)
		   logger.warn("engine at %d,%d", in.x, in.y);
	    }
	    break;

	case 7:		/* Keyword is 'missileRack' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing) {
			logger.warn("Missing missile rack coordinate in: \"%s\"",
			     teststr);
		    }
		    break;
		}
		if (ship.num_m_rack >= MAX_RACK_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many missile rack coordinates");
		} else {
		    m_rack[ship.num_m_rack++] = in;
		    if (debugShapeParsing)
			logger.warn("missile rack at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	case 8:		/* Keyword is 'name' */
	    ship.name = xp_strdup(teststr);
	    /* ship.name[strlen(ship.name)-1] = '\0'; */
	    break;

	case 9:		/* Keyword is 'author' */
	    ship.author = xp_strdup(teststr);
	    /* ship.author[strlen(ship.author)-1] = '\0'; */
	    break;

	case 10:		/* Keyword is 'leftRearGun' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing)
			logger.warn("Missing left rear gun coordinate in: \"%s\"",
			     teststr);
		    break;
		}
		if (ship.num_l_rgun >= MAX_GUN_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many left rear gun coordinates");
		} else {
		    l_rgun[ship.num_l_rgun++] = in;
		    if (debugShapeParsing)
			logger.warn("left rear gun at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	case 11:		/* Keyword is 'rightRearGun' */
	    while (teststr) {
		while (*teststr == ' ') teststr++;
		if (sscanf(teststr, "%d,%d", &in.x, &in.y) != 2) {
		    if (verboseShapeParsing)
			logger.warn("Missing right rear gun coordinate in: \"%s\"",
			     teststr);
		    break;
		}
		if (ship.num_r_rgun >= MAX_GUN_PTS) {
		    if (verboseShapeParsing)
			logger.warn("Too many right rear gun coordinates");
		} else {
		    r_rgun[ship.num_r_rgun++] = in;
		    if (debugShapeParsing)
			logger.warn("right rear gun at %d,%d", in.x, in.y);
		}
		teststr = strchr(teststr, ' ');
	    }
	    break;

	default:
	    if (verboseShapeParsing)
		logger.warn("Invalid ship shape keyword: \"%s\"", keyw);
	    /* the good thing about this format is that we can just ignore
	     * this.  it is likely to be a new extension we don't know
	     * about yet. */
	    break;
	}
    }

    /* Check for some things being set, and give them defaults if not */
    if (ship.num_points < 3) {
	if (verboseShapeParsing)
	   logger.warn("A shipshape must have at least 3 valid points.");
	return -1;
    }

    /* If no main gun set, put at foremost point */
    if (!mainGunSet) {
	max = 0;
	for (i = 1; i < ship.num_points; i++) {
	    if (pt[i].x > pt[max].x
		|| (pt[i].x == pt[max].x
		    && Math.abs(pt[i].y) < ABS(pt[max].y)))
		max = i;
	}
	m_gun = pt[max];
	mainGunSet = true;
    }

    /* If no left light set, put at leftmost point */
    if (!ship.num_l_light) {
	max = 0;
	for (i = 1; i < ship.num_points; i++) {
	    if (pt[i].y > pt[max].y
		|| (pt[i].y == pt[max].y
		    && pt[i].x <= pt[max].x))
		max = i;
	}
	l_light[0] = pt[max];
	ship.num_l_light = 1;
    }

    /* If no right light set, put at rightmost point */
    if (!ship.num_r_light) {
	max = 0;
	for (i = 1; i < ship.num_points; i++) {
	    if (pt[i].y < pt[max].y
		|| (pt[i].y == pt[max].y
		    && pt[i].x <= pt[max].x))
		max = i;
	}
	r_light[0] = pt[max];
	ship.num_r_light = 1;
    }

    /* If no engine position, put at rear of ship */
    if (!engineSet) {
	max = 0;
	for (i = 1; i < ship.num_points; i++) {
	    if (pt[i].x < pt[max].x)
		max = i;
	}
	/* this may lay outside of ship. */
	engine.x = pt[max].x;
	engine.y = 0;
	engineSet = true;
    }

    /* If no missile racks, put at main gun position*/
    if (!ship.num_m_rack) {
	m_rack[0] = m_gun;
	ship.num_m_rack = 1;
    }

    if (shapeLimits) {
	const int	isLow = -8, isHi = 8, isLeft = 8, isRight = -8,
			minLow = 1, minHi = 1, minLeft = 1, minRight = 1,
			horMax = 15, verMax = 15, horMin = -15, verMin = -15,
			minCount = 3, minSize = 22 + 16;
	int		low = 0, hi = 0, left = 0, right = 0,
			count = 0, change,
			lowest = 0, highest = 0,
			leftmost = 0, rightmost = 0;
	int		invalid = 0;
	const int	checkWidthAgainstLongestAxis = 1;

	max = 0;
	for (i = 0; i < ship.num_points; i++) {
	    x = pt[i].x;
	    y = pt[i].y;
	    change = 0;
	    if (y >= isLeft) {
		change++, left++;
		if (y > leftmost)
		    leftmost = y;
	    }
	    if (y <= isRight) {
		change++, right++;
		if (y < rightmost)
		    rightmost = y;
	    }
	    if (x <= isLow) {
		change++, low++;
		if (x < lowest)
		    lowest = x;
	    }
	    if (x >= isHi) {
		change++, hi++;
		if (x > highest)
		    highest = x;
	    }
	    if (change)
		count++;
	    if (y > horMax || y < horMin)
		max++;
	    if (x > verMax || x < verMin)
		max++;
	}
	if (low < minLow
	    || hi < minHi
	    || left < minLeft
	    || right < minRight
	    || count < minCount) {
	    if (verboseShapeParsing)
		logger.warn("Ship shape does not meet size requirements "
		     "(%d,%d,%d,%d,%d)", low, hi, left, right, count);
	    return -1;
	}
	if (max) {
	    if (verboseShapeParsing)
		logger.warn("Ship shape exceeds size maxima.");
	    return -1;
	}
	if (leftmost - rightmost + highest - lowest < minSize) {
	    if (verboseShapeParsing)
		logger.warn("Ship shape is not big enough.\n"
		     "The ship's width and height added together should\n"
		     "be at least %d.", minSize);
	    return -1;
	}

	if (checkWidthAgainstLongestAxis) {
	    /*
	     * For making sure the ship is the right width!
	     */
	    int pair[2];
	    int dist = 0, tmpDist = 0;
	    double vec[2], width, dTmp;
	    const int minWidth = 12;

	    /*
	     * Loop over all the points and find the two furthest apart
	     */
	    for (i = 0; i < ship.num_points; i++) {
		for (j = i + 1; j < ship.num_points; j++) {
		    /*
		     * Compare the points if they are not the same ones.
		     * Get this distance -- doesn't matter about sqrting
		     * it since only size is important.
		     */
		    if ((tmpDist = ((pt[i].x - pt[j].x) * (pt[i].x - pt[j].x) +
				    (pt[i].y - pt[j].y) * (pt[i].y - pt[j].y)))
			> dist) {
			/*
			 * Set new separation thingy.
			 */
			dist = tmpDist;
			pair[0] = i;
			pair[1] = j;
		    }
		}
	    }

	    /*
	     * Now we know the vector that is _|_ to the one above
	     * is simply found by (x,y) . (y,-x) => dot-prod = 0
	     */
	    vec[0] = (double)(pt[pair[1]].y - pt[pair[0]].y);
	    vec[1] = (double)(pt[pair[0]].x - pt[pair[1]].x);

	    /*
	     * Normalise
	     */
	    dTmp = LENGTH(vec[0], vec[1]);
	    vec[0] /= dTmp;
	    vec[1] /= dTmp;

	    /*
	     * Now check the width _|_ to the ship main line.
	     */
	    for (i = 0, width = dTmp = 0.0; i < ship.num_points; i++) {
		for (j = i + 1; j < ship.num_points; j++) {
		    /*
		     * Check the line if the points are not the same ones
		     */
		    width = fabs(vec[0] * (double)(pt[i].x - pt[j].x) +
				 vec[1] * (double)(pt[i].y - pt[j].y));
		    if (width > dTmp)
			dTmp = width;
		}
	    }

	    /*
	     * And make sure it is nice and far away
	     */
	    if (((int)dTmp) < minWidth) {
		if (verboseShapeParsing)
		   logger.warn("Ship shape is not big enough.\n"
			 "The ship's width should be at least %d.\n"
			 "Player's is %d", minWidth, (int)dTmp);
		return -1;
	    }
	}

	/*
	 * Check that none of the special points are outside the
	 * shape defined by the normal points.
	 * First the shape is drawn on a grid.  Then all grid points
	 * on the outside of the shape are marked.  Thusly for each
	 * special point can be determined if it is outside the shape.
	 */
	Grid_reset(&grid);

	/* Draw the ship outline first. */
	for (i = 0; i < ship.num_points; i++) {
	    j = i + 1;
	    if (j == ship.num_points)
		j = 0;

	    Grid_set_value(&grid, pt[i].x, pt[i].y, 1);

	    dx = pt[j].x - pt[i].x;
	    dy = pt[j].y - pt[i].y;
	    if (Math.abs(dx) >= ABS(dy)) {
		if (dx > 0) {
		    for (x = pt[i].x + 1; x < pt[j].x; x++) {
			y = pt[i].y + (dy * (x - pt[i].x)) / dx;
			Grid_set_value(&grid, x, y, 1);
		    }
		} else {
		    for (x = pt[j].x + 1; x < pt[i].x; x++) {
			y = pt[j].y + (dy * (x - pt[j].x)) / dx;
			Grid_set_value(&grid, x, y, 1);
		    }
		}
	    } else {
		if (dy > 0) {
		    for (y = pt[i].y + 1; y < pt[j].y; y++) {
			x = pt[i].x + (dx * (y - pt[i].y)) / dy;
			Grid_set_value(&grid, x, y, 1);
		    }
		} else {
		    for (y = pt[j].y + 1; y < pt[i].y; y++) {
			x = pt[j].x + (dx * (y - pt[j].y)) / dy;
			Grid_set_value(&grid, x, y, 1);
		    }
		}
	    }
	}

	/* Check the borders of the grid for blank points. */
	for (y = -15; y <= 15; y++) {
	    for (x = -15; x <= 15; x += (y == -15 || y == 15) ? 1 : 2*15) {
		if (Grid_get_value(&grid, x, y) == 0)
		    Grid_add(&grid, x, y);
	    }
	}

	/* Check from the borders of the grid to the centre. */
	while (!Grid_is_ready(&grid)) {
	    Point  pos = Grid_get(&grid);

	    x = pos.x;
	    y = pos.y;
	    if (x <  15 && Grid_get_value(&grid, x + 1, y) == 0)
		Grid_add(&grid, x + 1, y);
	    if (x > -15 && Grid_get_value(&grid, x - 1, y) == 0)
		Grid_add(&grid, x - 1, y);
	    if (y <  15 && Grid_get_value(&grid, x, y + 1) == 0)
		Grid_add(&grid, x, y + 1);
	    if (y > -15 && Grid_get_value(&grid, x, y - 1) == 0)
		Grid_add(&grid, x, y - 1);
	}

#ifdef GRID_PRINT
	Grid_print(&grid);
#endif

	/*
	 * Note that for the engine, old format shapes may well have the
	 * engine position outside the ship, so this check not used for those.
	 */

	if (Grid_point_is_outside_ship(&grid, m_gun)) {
	    if (verboseShapeParsing)
		logger.warn("Main gun (at (%d,%d)) is outside ship.",
		     m_gun.x, m_gun.y);
	    invalid++;
	}
	for (i = 0; i < ship.num_l_gun; i++) {
	    if (Grid_point_is_outside_ship(&grid, l_gun[i])) {
		if (verboseShapeParsing)
		   logger.warn("Left gun at (%d,%d) is outside ship.",
			 l_gun[i].x, l_gun[i].y);
		invalid++;
	    }
	}
	for (i = 0; i < ship.num_r_gun; i++) {
	    if (Grid_point_is_outside_ship(&grid, r_gun[i])) {
		if (verboseShapeParsing)
		   logger.warn("Right gun at (%d,%d) is outside ship.",
			 r_gun[i].x, r_gun[i].y);
		invalid++;
	    }
	}
	for (i = 0; i < ship.num_l_rgun; i++) {
	    if (Grid_point_is_outside_ship(&grid, l_rgun[i])) {
		if (verboseShapeParsing)
		   logger.warn("Left rear gun at (%d,%d) is outside ship.",
			 l_rgun[i].x, l_rgun[i].y);
		invalid++;
	    }
	}
	for (i = 0; i < ship.num_r_rgun; i++) {
	    if (Grid_point_is_outside_ship(&grid, r_rgun[i])) {
		if (verboseShapeParsing)
		   logger.warn("Right rear gun at (%d,%d) is outside ship.",
			 r_rgun[i].x, r_rgun[i].y);
		invalid++;
	    }
	}
	for (i = 0; i < ship.num_m_rack; i++) {
	    if (Grid_point_is_outside_ship(&grid, m_rack[i])) {
		if (verboseShapeParsing)
		   logger.warn("Missile rack at (%d,%d) is outside ship.",
			 m_rack[i].x, m_rack[i].y);
		invalid++;
	    }
	}
	for (i = 0; i < ship.num_l_light; i++) {
	    if (Grid_point_is_outside_ship(&grid, l_light[i])) {
		if (verboseShapeParsing)
		   logger.warn("Left light at (%d,%d) is outside ship.",
			 l_light[i].x, l_light[i].y);
		invalid++;
	    }
	}
	for (i = 0; i < ship.num_r_light; i++) {
	    if (Grid_point_is_outside_ship(&grid, r_light[i])) {
		if (verboseShapeParsing)
		   logger.warn("Right light at (%d,%d) is outside ship.",
			 r_light[i].x, r_light[i].y);
		invalid++;
	    }
	}
	if (Grid_point_is_outside_ship(&grid, engine)) {
	    if (verboseShapeParsing)
		logger.warn("Engine (at (%d,%d)) is outside ship.",
		     engine.x, engine.y);
	    invalid++;
	}

	if (debugShapeParsing) {
	    for (i = -15; i <= 15; i++) {
		for (j = -15; j <= 15; j++) {
		    switch (Grid_get_value(&grid, j, i)) {
		    case 0: putchar(' '); break;
		    case 1: putchar('*'); break;
		    case 2: putchar('.'); break;
		    default: putchar('?'); break;
		    }
		}
		putchar('\n');
	    }
	}

	if (invalid)
	    return -1;
    }

    ship.num_orig_points = ship.num_points;

    /*MARA evil hack*/
    /* always do SSHACK on server, it seems to work */
    if (is_server) {
	pt[ship.num_points] = pt[0];
	for (i = 1; i < ship.num_points; i++)
	    pt[i + ship.num_points] = pt[ship.num_points - i];
	ship.num_points = ship.num_points * 2;
    }
    /*MARA evil hack*/

    i = RES;
    if (!(ship.pts[0] = XMALLOC(Click , (size_t)ship.num_points * i))
	|| (ship.num_l_gun
	    && !(ship.l_gun[0]
		 = XMALLOC(Click , (size_t)ship.num_l_gun * i)))
	|| (ship.num_r_gun
	    && !(ship.r_gun[0]
		 = XMALLOC(Click , (size_t)ship.num_r_gun * i)))
	|| (ship.num_l_rgun
	    && !(ship.l_rgun[0]
		 = XMALLOC(Click , (size_t)ship.num_l_rgun * i)))
	|| (ship.num_r_rgun
	    && !(ship.r_rgun[0]
		 = XMALLOC(Click , (size_t)ship.num_r_rgun * i)))
	|| (ship.num_l_light
	    && !(ship.l_light[0]
		 = XMALLOC(Click , (size_t)ship.num_l_light * i)))
	|| (ship.num_r_light
	    && !(ship.r_light[0]
		 = XMALLOC(Click , (size_t)ship.num_r_light * i)))
	|| (ship.num_m_rack
	    && !(ship.m_rack[0]
		 = XMALLOC(Click , (size_t)ship.num_m_rack * i)))) {
	error("Not enough memory for ship shape");
	XFREE(ship.pts[0]);
	XFREE(ship.l_gun[0]);
	XFREE(ship.r_gun[0]);
	XFREE(ship.l_rgun[0]);
	XFREE(ship.r_rgun[0]);
	XFREE(ship.l_light[0]);
	XFREE(ship.r_light[0]);
	XFREE(ship.m_rack[0]);
	return -1;
    }

    for (i = 1; i < ship.num_points; i++)
	ship.pts[i] = ship.pts[i - 1][RES];

    for (i = 1; i < ship.num_l_gun; i++)
	ship.l_gun[i] = ship.l_gun[i - 1][RES];

    for (i = 1; i < ship.num_r_gun; i++)
	ship.r_gun[i] = ship.r_gun[i - 1][RES];

    for (i = 1; i < ship.num_l_rgun; i++)
	ship.l_rgun[i] = ship.l_rgun[i - 1][RES];

    for (i = 1; i < ship.num_r_rgun; i++)
	ship.r_rgun[i] = ship.r_rgun[i - 1][RES];

    for (i = 1; i < ship.num_l_light; i++)
	ship.l_light[i] = ship.l_light[i - 1][RES];

    for (i = 1; i < ship.num_r_light; i++)
	ship.r_light[i] = ship.r_light[i - 1][RES];

    for (i = 1; i < ship.num_m_rack; i++)
	ship.m_rack[i] = ship.m_rack[i - 1][RES];


    for (i = 0; i < ship.num_points; i++)
	Ship_set_point_ipos(ship, i, pt[i]);

    if (engineSet)
	Ship_set_engine_ipos(ship, engine);

    if (mainGunSet)
	Ship_set_m_gun_ipos(ship, m_gun);

    for (i = 0; i < ship.num_l_gun; i++)
	Ship_set_l_gun_ipos(ship, i, l_gun[i]);

    for (i = 0; i < ship.num_r_gun; i++)
	Ship_set_r_gun_ipos(ship, i, r_gun[i]);

    for (i = 0; i < ship.num_l_rgun; i++)
	Ship_set_l_rgun_ipos(ship, i, l_rgun[i]);

    for (i = 0; i < ship.num_r_rgun; i++)
	Ship_set_r_rgun_ipos(ship, i, r_rgun[i]);

    for (i = 0; i < ship.num_l_light; i++)
	Ship_set_l_light_ipos(ship, i, l_light[i]);

    for (i = 0; i < ship.num_r_light; i++)
	Ship_set_r_light_ipos(ship, i, r_light[i]);

    for (i = 0; i < ship.num_m_rack; i++)
	Ship_set_m_rack_ipos(ship, i, m_rack[i]);

    Rotate_ship(ship);

    return 0;
}

static ShipShape do_parse_shape(String str)
{
    ShipShape ship;

    if (!str || !*str) {
	if (debugShapeParsing)
	   logger.warn("shape str not set");
	return Default_ship();
    }
    if (!(ship = XMALLOC(ShipShape , 1))) {
	error("No mem for ship shape");
	return Default_ship();
    }
    if (shape2wire(str, ship) != 0) {
	free(ship);
	if (debugShapeParsing)
	   logger.warn("shape2wire failed");
	return Default_ship();
    }
    if (debugShapeParsing)
	logger.warn("shape2wire succeeded");

    return(ship);
}

void Free_ship_shape(ShipShape ship)
{
    if (ship != null && ship != Default_ship()) {
	if (ship.num_points > 0)  XFREE(ship.pts[0]);
	if (ship.num_l_gun > 0)   XFREE(ship.l_gun[0]);
	if (ship.num_r_gun > 0)   XFREE(ship.r_gun[0]);
	if (ship.num_l_rgun > 0)  XFREE(ship.l_rgun[0]);
	if (ship.num_r_rgun > 0)  XFREE(ship.r_rgun[0]);
	if (ship.num_l_light > 0) XFREE(ship.l_light[0]);
	if (ship.num_r_light > 0) XFREE(ship.r_light[0]);
	if (ship.num_m_rack > 0)  XFREE(ship.m_rack[0]);

	if (ship.name) free(ship.name);
	if (ship.author) free(ship.author);
	free(ship);
    }
}

ShipShape Parse_shape_str(String str)
{
    if (is_server)
	verboseShapeParsing = debugShapeParsing;
    else
	verboseShapeParsing = true;
    shapeLimits = true;
    return do_parse_shape(str);
}

ShipShape Convert_shape_str(String str)
{
    verboseShapeParsing = debugShapeParsing;
    shapeLimits = debugShapeParsing;
    return do_parse_shape(str);
}

/*
 * Returns 0 if ships is not valid, 1 if valid.
 */
int Validate_shape_str(String str)
{
    ShipShape ship;

    verboseShapeParsing = true;
    shapeLimits = true;
    ship = do_parse_shape(str);
    Free_ship_shape(ship);
    return (ship && ship != Default_ship());
}

void Convert_ship_2_string(ShipShape ship, String buf, String ext,
			   unsigned shape_version)
{
    char tmp[MSG_LEN];
    int i, buflen = 0, extlen, tmplen;

    ext[extlen = 0] = '\0';

    if (shape_version >= 0x3200) {
	Point2D engine, m_gun;

	strcpy(buf, "(SH:");
	buflen = strlen(&buf[0]);
	for (i = 0; i < ship.num_orig_points && i < MAX_SHIP_PTS; i++) {
	    Point2D pt = Ship_get_point_position(ship, i, 0);

	    sprintf(&buf[buflen], " %d,%d", (int)pt.x, (int)pt.y);
	    buflen += strlen(&buf[buflen]);
	}
	engine = Ship_get_engine_position(ship, 0);
	m_gun = Ship_get_m_gun_position(ship, 0);
	sprintf(&buf[buflen], ")(EN: %d,%d)(MG: %d,%d)",
		(int)engine.x, (int)engine.y,
		(int)m_gun.x, (int)m_gun.y);
	buflen += strlen(&buf[buflen]);

	/*
	 * If the calculations are correct then only from here on
	 * there is danger for overflowing the MSG_LEN size
	 * of the buffer.  Therefore first copy a new pair of
	 * parentheses into a temporary buffer and when the closing
	 * parenthesis is reached then determine if there is enough
	 * room in the main buffer or else copy it into the extended
	 * buffer.  This scheme allows cooperation with versions which
	 * didn't had the extended buffer yet for which the extended
	 * buffer will simply be discarded.
	 */
	if (ship.num_l_gun > 0) {
	    strcpy(&tmp[0], "(LG:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_l_gun && i < MAX_GUN_PTS; i++) {
		Point2D l_gun = Ship_get_l_gun_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)l_gun.x, (int)l_gun.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
	if (ship.num_r_gun > 0) {
	    strcpy(&tmp[0], "(RG:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_r_gun && i < MAX_GUN_PTS; i++) {
		Point2D r_gun = Ship_get_r_gun_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)r_gun.x, (int)r_gun.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
	if (ship.num_l_rgun > 0) {
	    strcpy(&tmp[0], "(LR:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_l_rgun && i < MAX_GUN_PTS; i++) {
		Point2D l_rgun = Ship_get_l_rgun_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)l_rgun.x, (int)l_rgun.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
	if (ship.num_r_rgun > 0) {
	    strcpy(&tmp[0], "(RR:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_r_rgun && i < MAX_GUN_PTS; i++) {
		Point2D r_rgun = Ship_get_r_rgun_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)r_rgun.x, (int)r_rgun.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
	if (ship.num_l_light > 0) {
	    strcpy(&tmp[0], "(LL:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_l_light && i < MAX_LIGHT_PTS; i++) {
		Point2D l_light = Ship_get_l_light_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)l_light.x, (int)l_light.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
	if (ship.num_r_light > 0) {
	    strcpy(&tmp[0], "(RL:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_r_light && i < MAX_LIGHT_PTS; i++) {
		Point2D r_light = Ship_get_r_light_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)r_light.x, (int)r_light.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
	if (ship.num_m_rack > 0) {
	    strcpy(&tmp[0], "(MR:");
	    tmplen = strlen(&tmp[0]);
	    for (i = 0; i < ship.num_m_rack && i < MAX_RACK_PTS; i++) {
		Point2D m_rack = Ship_get_m_rack_position(ship, i, 0);

		sprintf(&tmp[tmplen], " %d,%d",
			(int)m_rack.x, (int)m_rack.y);
		tmplen += strlen(&tmp[tmplen]);
	    }
	    strcpy(&tmp[tmplen], ")");
	    tmplen++;
	    if (buflen + tmplen < MSG_LEN) {
		strcpy(&buf[buflen], tmp);
		buflen += tmplen;
	    } else if (extlen + tmplen < MSG_LEN) {
		strcpy(&ext[extlen], tmp);
		extlen += tmplen;
	    }
	}
    } else
	buf[0] = '\0';

    if (buflen >= MSG_LEN || extlen >= MSG_LEN)
	logger.warn("BUG: convert ship: buffer overflow (%d,%d)", buflen, extlen);

    if (debugShapeParsing)
	logger.warn("ship 2 str: %s %s", buf, ext);
}
    public enum SSKeys{
           SHAPE("SH","shape"),
           MAINGUN("MG","mainGun"),
           LEFTGUN("LG","leftGun"),
           RIGHTGUN("RG","rightGun"),
           LEFTLIGHT("LL","leftLight"),
           RIGHTLIGHT("RL","rightLight"),
           ENGINE("EN","engine"),
           MISSILERACK("MR","missileRack"),
           NAME("NM","name"),
           AUTHOR("AU","author"),
           LEFTREARGUN("LR","leftRearGun"),
           RIGHTREARGUN("RR","rightRearGun");
        String longName;

        public String getLongName() {
            return longName;
        }

        public String getShortName() {
            return shortName;
        }

        String shortName;

        SSKeys(String shortName, String longName) {
            this.longName = longName;
            this.shortName = shortName;
        }

        private static final Map<String,SSKeys> lookup
                = new HashMap<>();

        static {
            for(SSKeys s : EnumSet.allOf(SSKeys.class)) {
                // Assert for duplicates.  Should never happen.
                assert (lookup.put(s.getShortName().toUpperCase(), s) != null);
                assert (lookup.put(s.getLongName().toUpperCase(),s) != null);
            }
        }


        /**
         * Case insensitive lookup by shortname or longname
         * @param name
         * @return
         */
        public static SSKeys lookup(String name) {
            return lookup.get(name.toUpperCase());
        }
    }


void Calculate_shield_radius(ShipShape ship)
{
    int			i;
    int			radius2, max_radius = 0;

    for (i = 0; i < ship.num_points; i++) {
	Point2D pti = Ship_get_point_position(ship, i, 0);
	radius2 = (int)(Math.pow(pti.x) + sqr(pti.y),2.0);
	if (radius2 > max_radius)
	    max_radius = radius2;
    }
    max_radius = (int)(2.0 * sqrt((double)max_radius));
    ship.shield_radius = (max_radius + 2 <= 34)
			? 34
			: (max_radius + 2 - (max_radius & 1));
}

}
