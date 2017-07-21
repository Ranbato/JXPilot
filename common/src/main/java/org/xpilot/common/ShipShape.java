package org.xpilot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.*;

import static java.lang.Math.sqrt;
import static org.xpilot.common.Const.MSG_LEN;
import static org.xpilot.common.Const.RES;
import static org.xpilot.common.Shape.*;
import static org.xpilot.common.XPMath.LENGTH;
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

    ArrayList<Click> cashed_pts = new ArrayList<>(MAX_SHIP_PTS);
    int cashed_dir;

    public void drawShip(JPanel panel, int dir) {

        Path2D shape = new Path2D.Float();

        for (int idx = 0;idx < shipShapes[dir].pts.size();idx++
             ) {
            Click p = shipShapes[dir].pts.get(idx);
            if(idx == 0){
                shape.moveTo(p.cx,p.cy);
            } else {
                shape.lineTo(p.cx,p.cy);
            }

        }
        Click p = shipShapes[dir].pts.get(0);
        shape.lineTo(p.cx,p.cy);
        Graphics2D graphics2D = (Graphics2D)panel.getGraphics();
        // todo Temporary to see on screen
        graphics2D.translate(500,500);
        AffineTransform at = new AffineTransform();
        at.scale(0.25,0.25);
        shape.transform(at);
        graphics2D.setColor(Color.LIGHT_GRAY);
        Rectangle rectangle = shape.getBounds();
        graphics2D.fill(rectangle);
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(shape);
    }

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
        String name;
        String author;


    }


    static Click ipos2clpos(Point pos) {
        Click pt = new Click();

        pt.cx = pt.PIXEL_TO_CLICK(pos.x);
        pt.cy = pt.PIXEL_TO_CLICK(pos.y);

        return pt;
    }

    static Point2D clpos2position(Click pt) {
        Point2D pos = new Point2D.Float(pt.CLICK_TO_FLOAT(pt.cx), pt.CLICK_TO_FLOAT(pt.cy));

        return pos;
    }


    public Click
    Ship_get_point_clpos(int i, int dir) {
        return shipShapes[dir].pts.get(i);
    }

    Click
    Ship_get_engine_clpos(int dir) {
        return shipShapes[dir].engine;
    }

    Click
    Ship_get_m_gun_clpos(int dir) {
        return shipShapes[dir].m_gun;
    }

    Click
    Ship_get_l_gun_clpos(int gun, int dir) {
        return shipShapes[dir].l_gun.get(gun);
    }

    Click
    Ship_get_r_gun_clpos(int gun, int dir) {
        return shipShapes[dir].r_gun.get(gun);
    }

    Click
    Ship_get_l_rgun_clpos(int gun, int dir) {
        return shipShapes[dir].r_rgun.get(gun);
    }

    Click
    Ship_get_r_rgun_clpos(int gun, int dir) {
        return shipShapes[dir].r_rgun.get(gun);
    }

    Click
    Ship_get_l_light_clpos(int l, int dir) {
        return shipShapes[dir].l_light.get(l);
    }

    Click
    Ship_get_r_light_clpos(int l, int dir) {
        return shipShapes[dir].r_light.get(l);
    }

    Click
    Ship_get_m_rack_clpos(int rack, int dir) {
        return shipShapes[dir].m_rack.get(rack);
    }


    Point2D
    Ship_get_point_position(int i, int dir) {
        return clpos2position(Ship_get_point_clpos(i, dir));
    }

    Point2D
    Ship_get_engine_position(int dir) {
        return clpos2position(Ship_get_engine_clpos(dir));
    }

    Point2D
    Ship_get_m_gun_position(int dir) {
        return clpos2position(Ship_get_m_gun_clpos(dir));
    }

    Point2D
    Ship_get_l_gun_position(int gun, int dir) {
        return clpos2position(Ship_get_l_gun_clpos(gun, dir));
    }

    Point2D
    Ship_get_r_gun_position(int gun, int dir) {
        return clpos2position(Ship_get_r_gun_clpos(gun, dir));
    }

    Point2D
    Ship_get_l_rgun_position(int gun, int dir) {
        return clpos2position(Ship_get_l_rgun_clpos(gun, dir));
    }

    Point2D
    Ship_get_r_rgun_position(int gun, int dir) {
        return clpos2position(Ship_get_r_rgun_clpos(gun, dir));
    }

    Point2D
    Ship_get_l_light_position(int l, int dir) {
        return clpos2position(Ship_get_l_light_clpos(l, dir));
    }

    Point2D
    Ship_get_r_light_position(int l, int dir) {
        return clpos2position(Ship_get_r_light_clpos(l, dir));
    }

    Point2D
    Ship_get_m_rack_position(int rack, int dir) {
        return clpos2position(Ship_get_m_rack_clpos(rack, dir));
    }

    boolean debugShapeParsing = false;
    boolean verboseShapeParsing = false;
    boolean shapeLimits = true;
//extern boolean is_server;

    void Ship_set_point_ipos(int i, Point pos) {
        shipShapes[0].pts.add(i, ipos2clpos(pos));
    }

    void Ship_set_engine_ipos(Point pos) {
        shipShapes[0].engine = ipos2clpos(pos);
    }

    void Ship_set_m_gun_ipos(Point pos) {
        shipShapes[0].m_gun = ipos2clpos(pos);
    }

    void Ship_set_l_gun_ipos(int i, Point pos) {
        shipShapes[0].l_gun.add(i, ipos2clpos(pos));
    }

    void Ship_set_r_gun_ipos(int i, Point pos) {
        shipShapes[0].r_gun.add(i, ipos2clpos(pos));
    }

    void Ship_set_l_rgun_ipos(int i, Point pos) {
        shipShapes[0].l_rgun.add(i, ipos2clpos(pos));
    }

    void Ship_set_r_rgun_ipos(int i, Point pos) {
        shipShapes[0].r_rgun.add(i, ipos2clpos(pos));
    }

    void Ship_set_l_light_ipos(int i, Point pos) {
        shipShapes[0].l_light.add(i, ipos2clpos(pos));
    }

    void Ship_set_r_light_ipos(int i, Point pos) {
        shipShapes[0].r_light.add(i, ipos2clpos(pos));
    }

    void Ship_set_m_rack_ipos(int i, Point pos) {
        shipShapes[0].m_rack.add(i, ipos2clpos(pos));
    }


    /* kps - tmp hack */
    ArrayList<Click> Shape_get_points(int dir) {
 

    /* todo kps - optimize if cashed_dir == dir */
        cashed_pts = (ArrayList<Click>) shipShapes[dir].pts.clone();

        return cashed_pts;
    }

    Click Rotate_point(Click pt, int dir) {
        double cx, cy;
        Click ret = new Click();

        cx = tcos(dir) * pt.cx - tsin(dir) * pt.cy;
        cy = tsin(dir) * pt.cx + tcos(dir) * pt.cy;
        ret.cx = (int) (cx >= 0.0 ? cx + 0.5 : cx - 0.5);
        ret.cy = (int) (cy >= 0.0 ? cy + 0.5 : cy - 0.5);

        return ret;

    }

    void Rotate_position(Point2D pt[]) {
        int i;

        for (i = 1; i < RES; i++) {
            pt[i].setLocation(tcos(i) * pt[0].getX() - tsin(i) * pt[0].getY(),
                    tsin(i) * pt[0].getX() + tcos(i) * pt[0].getY());
        }
    }

    void Rotate_ship() {
        int i;

        SShape original = shipShapes[0];
        for (int dir = 1; dir < RES; dir++) {
            shipShapes[dir]= new SShape();
            for (i = 0; i < original.pts.size(); i++) {
                shipShapes[dir].pts.add(i, Rotate_point(original.pts.get(i), dir));
            }

            shipShapes[dir].engine = Rotate_point(original.engine, dir);
            shipShapes[dir].m_gun = Rotate_point(original.m_gun, dir);
            for (i = 0; i < original.l_gun.size(); i++) {
                shipShapes[dir].l_gun.add(i, Rotate_point(original.l_gun.get(i), dir));
            }
            for (i = 0; i < original.r_gun.size(); i++) {
                shipShapes[dir].r_gun.add(i, Rotate_point(original.r_gun.get(i), dir));
            }
            for (i = 0; i < original.l_rgun.size(); i++) {
                shipShapes[dir].l_rgun.add(i, Rotate_point(original.l_rgun.get(i), dir));
            }
            for (i = 0; i < original.r_rgun.size(); i++) {
                shipShapes[dir].r_rgun.add(i, Rotate_point(original.r_rgun.get(i), dir));
            }
            for (i = 0; i < original.l_light.size(); i++) {
                shipShapes[dir].l_light.add(i, Rotate_point(original.l_light.get(i), dir));
            }
            for (i = 0; i < original.r_light.size(); i++) {
                shipShapes[dir].r_light.add(i, Rotate_point(original.r_light.get(i), dir));
            }
            for (i = 0; i < original.m_rack.size(); i++) {
                shipShapes[dir].m_rack.add(i, Rotate_point(original.m_rack.get(i), dir));
            }
        }
    }

    /*
     * Default this ship
     */
    void Default_ship() {
        SShape sh = new SShape();
        shipShapes[0] = sh;

            Point pos = new Point();

            pos.x = 14;
            pos.y = 0;
            Ship_set_point_ipos(0, pos);

            pos.x = -8;
            pos.y = 8;
            Ship_set_point_ipos(1, pos);

            pos.x = -8;
            pos.y = -8;
            Ship_set_point_ipos(2, pos);

            pos.x = -8;
            pos.y = 0;
            Ship_set_engine_ipos(pos);

            pos.x = 14;
            pos.y = 0;
            Ship_set_m_gun_ipos(pos);

            pos.x = -8;
            pos.y = 8;
            Ship_set_l_light_ipos(0, pos);

            pos.x = -8;
            pos.y = -8;
            Ship_set_r_light_ipos(0, pos);

            pos.x = 14;
            pos.y = 0;
            Ship_set_m_rack_ipos(0, pos);

            sh.l_gun.clear();
            sh.r_gun.clear();
            sh.l_rgun.clear();
            sh.r_rgun.clear();

            sh.name = "Default";
            sh.author = "XPilot";

            Rotate_ship();

    }

    static class Grid {
        int todo, done;
        byte pt[][] = new byte[32][32];
        Point chk[] = new Point[32 * 32];


        /*
         * Functions to simplify limit-checking for ship points.
         */
        void Grid_reset() {
            todo = 0;
            done = 0;
            pt = new byte[32][32];
            chk = new Point[32 * 32];
            for (int i = 0;i<chk.length;i++
                 )
            {
                chk[i] = new Point();
            }
        }

        void Grid_set_value(int x, int y, int value) {
            assert (!(x < -15 || x > 15 || y < -15 || y > 15));
            pt[x + 15][y + 15] = (byte) value;
        }

        byte Grid_get_value(int x, int y) {
            if (x < -15 || x > 15 || y < -15 || y > 15) {
                return 2;
            }
            return pt[x + 15][y + 15];
        }

        void Grid_add(int x, int y) {
            Grid_set_value(x, y, 2);
            if(chk[todo] == null){
                chk[todo] = new Point();
            }
            chk[todo].x = x + 15;
            chk[todo].y = y + 15;
            todo++;
        }

        Point Grid_get() {
            Point pos = new Point();

            pos.x = chk[done].x - 15;
            pos.y = chk[done].y - 15;
            done++;

            return pos;
        }

        boolean Grid_is_ready() {
            return (done >= todo) ? true : false;
        }

        boolean Grid_point_is_outside_ship(Point pt) {
            byte value = Grid_get_value(pt.x, pt.y);

            if (value == 2) {
                return true;
            }
            return false;
        }


        void Grid_print() {
            int x, y;

            logger.debug("============================================================");

            for (y = 0; y < 32; y++) {
                StringBuilder line = new StringBuilder(32);

                for (x = 0; x < 32; x++) {
                    line.append(pt[x][y]);
                }
                logger.debug(line.toString());
            }

            logger.debug("------------------------------------------------------------");

            {
                Point pt = new Point();

                for (pt.y = -15; pt.y <= 15; pt.y++) {
                    StringBuilder line = new StringBuilder(32);

                    for (pt.x = -15; pt.x <= 15; pt.x++) {
                        line.append(Grid_point_is_outside_ship(pt) ? '.' : '*');
                    }
                    logger.debug(line.toString());
                }
            }


            logger.debug("\n");
        }
    }

    int shape2wire(String ship_shape_str) {
        Grid grid = new Grid();
        int i, j, x, y, dx, dy, shape_version = 0;
        ArrayList<Point> pt = new ArrayList<>();
        Point in = new Point();
        Point old_in = new Point();
        Point engine = new Point();
        Point m_gun = new Point();
        ArrayList<Point> l_light = new ArrayList<>();
        ArrayList<Point> r_light = new ArrayList<>();
        ArrayList<Point> l_gun = new ArrayList<>();
        ArrayList<Point> r_gun = new ArrayList<>();
        ArrayList<Point> l_rgun = new ArrayList<>();
        ArrayList<Point> r_rgun = new ArrayList<>();
        ArrayList<Point> m_rack = new ArrayList<>();
        boolean mainGunSet = false, engineSet = false;
        String keyw;
        boolean remove_edge = false;
        int[] coords;
        Point max;
        Point temp;
        String name = "", author = "";


        if (debugShapeParsing) {
            logger.warn("parsing shape: {}", ship_shape_str);
        }

        String[] sections = ship_shape_str.split("[()]");
        for (String section : sections) {


            section = section.trim();

            if(section.isEmpty()){
                continue;
            }
            if (shape_version == 0) {
                if (Character.isDigit(section.charAt(0))) {
                    if (verboseShapeParsing) {
                        logger.warn("Ship shape is in obsolete 3.1 format.");
                    }
                    return -1;
                } else {
                    shape_version = 0x3200;
                }
            }
            String[] types = section.split(":");

            keyw = types[0].trim();

            if (types.length != 2) {
                if (verboseShapeParsing) {
                    logger.warn("Missing colon in ship shape: {}", keyw);
                }
                continue;
            }

            String points[] = types[1].split("\\s+");

            switch (SSKeys.lookup(keyw)) {

                case SHAPE: /* Keyword is 'shape' */
                    for (String point : points) {


                        if(point.isEmpty()){
                            continue;
                        }
                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing ship shape coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        } else {
                            in = new Point(coords[0], coords[1]);
                        }
                        if (pt.size() >= MAX_SHIP_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many ship shape coordinates");
                            }
                        } else {
                            if (pt.size() > 0
                                    && old_in.x == in.x
                                    && old_in.y == in.y) {
                                remove_edge = true;
                                if (verboseShapeParsing) {
                                    logger.warn("Duplicate ship point at {},{}, ignoring it.",
                                            in.x, in.y);
                                }
                            } else {
                                pt.add(in);
                                old_in = in;
                                if (debugShapeParsing) {
                                    logger.warn("ship point at {},{}", in.x, in.y);
                                }
                            }
                        }

                    }
                    if (pt.size() > 0) {
                        Point first = pt.get(0);
                        Point latest = pt.get(pt.size() - 1);
                        if (first.x == latest.x
                                && first.y == latest.y) {
                            if (verboseShapeParsing) {
                                logger.warn("Ship last point equals first point at {},{}, ignoring it.", first.x, first.y);
                            }
                            remove_edge = true;
                            pt.remove(pt.size() - 1);
                        }
                    }

                    if (remove_edge && verboseShapeParsing) {
                        logger.warn("Removing ship edges with length 0.");
                    }

                    break;

                case MAINGUN:		/* Keyword is 'mainGun' */
                    if (mainGunSet) {
                        if (verboseShapeParsing) {
                            logger.warn("Ship shape keyword \"{}\" multiple defined", keyw);
                        }
                        break;
                    }

                    coords = Arrays.stream(points[1].split(",")).mapToInt(Integer::parseInt).toArray();
                    if (coords.length != 2) {
                        if (verboseShapeParsing) {
                            logger.warn("Missing main gun coordinate in: \"{}\"", coords);
                        }
                    } else {
                        m_gun = new Point(coords[0], coords[1]);
                        mainGunSet = true;
                        if (debugShapeParsing) {
                            logger.warn("main gun at {},{}", in.x, in.y);
                        }
                    }
                    break;

                case LEFTGUN:		/* Keyword is 'leftGun' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }
                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing left gun coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        }

                        in = new Point(coords[0], coords[1]);
                        if (l_gun.size() >= MAX_GUN_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many left gun coordinates");
                            }
                        } else {
                            l_gun.add(in);
                            if (debugShapeParsing) {
                                logger.warn("left gun at {},{}", in.x, in.y);
                            }
                        }
                    }

                    break;

                case RIGHTGUN:		/* Keyword is 'rightGun' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }
                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing right gun coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        }
                        in = new Point(coords[0], coords[1]);
                        if (r_gun.size() >= MAX_GUN_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many right gun coordinates");
                            }
                        } else {
                            r_gun.add(in);
                            if (debugShapeParsing) {
                                logger.warn("right gun at {},{}", in.x, in.y);
                            }
                        }
                    }
                    break;

                case LEFTLIGHT:		/* Keyword is 'leftLight' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }
                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing left light coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        }
                        in = new Point(coords[0], coords[1]);
                        if (l_light.size() >= MAX_LIGHT_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many left light coordinates");
                            }
                        } else {
                            l_light.add(in);
                            if (debugShapeParsing) {
                                logger.warn("left light at {},{}", in.x, in.y);
                            }
                        }
                    }
                    break;

                case RIGHTLIGHT:		/* Keyword is 'rightLight' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }

                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing left light coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        }
                        in = new Point(coords[0], coords[1]);
                        if (r_light.size() >= MAX_LIGHT_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many right light coordinates");
                            }
                        } else {
                            r_light.add(in);
                            if (debugShapeParsing) {
                                logger.warn("right light at {},{}", in.x, in.y);
                            }
                        }
                    }
                    break;

                case ENGINE:		/* Keyword is 'engine' */
                    if (engineSet) {
                        if (verboseShapeParsing) {
                            logger.warn("Ship shape keyword \"%s\" multiple defined", keyw);
                        }
                        break;
                    }
                    coords = Arrays.stream(points[1].split(",")).mapToInt(Integer::parseInt).toArray();
                    if (coords.length != 2) {
                        if (verboseShapeParsing) {
                            logger.warn("Missing engine coordinate in: \"{}\"", coords);
                        }
                    } else {
                        engine = new Point(coords[0], coords[1]);

                        engineSet = true;
                        if (debugShapeParsing) {
                            logger.warn("engine at {},{}", in.x, in.y);
                        }
                    }
                    break;

                case MISSILERACK:		/* Keyword is 'missileRack' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }

                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing missile rack coordinate in: \"{}\"",
                                        coords);
                            }
                            break;
                        }
                        in = new Point(coords[0], coords[1]);

                        if (m_rack.size() >= MAX_RACK_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many missile rack coordinates");
                            }
                        } else {
                            m_rack.add(in);
                            if (debugShapeParsing) {
                                logger.warn("missile rack at {},{}", in.x, in.y);
                            }
                        }
                    }
                    break;

                case NAME:		/* Keyword is 'name' */
                    name = types[1];
        /* ship.name[strlen(ship.name)-1] = '\0'; */
                    break;

                case AUTHOR:		/* Keyword is 'author' */
                    author = types[1];
        /* ship.author[strlen(ship.author)-1] = '\0'; */
                    break;

                case LEFTREARGUN:		/* Keyword is 'leftRearGun' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }

                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing left rear gun coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        }
                        in = new Point(coords[0], coords[1]);
                        if (l_rgun.size() >= MAX_GUN_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many left rear gun coordinates");
                            }
                        } else {
                            l_rgun.add(in);
                            if (debugShapeParsing) {
                                logger.warn("left rear gun at {},{}", in.x, in.y);
                            }
                        }
                    }
                    break;

                case RIGHTREARGUN:		/* Keyword is 'rightRearGun' */
                    for (String point : points) {

                        if(point.isEmpty()){
                            continue;
                        }

                        coords = Arrays.stream(point.split(",")).mapToInt(Integer::parseInt).toArray();
                        if (coords.length != 2) {
                            if (verboseShapeParsing) {
                                logger.warn("Missing right rear gun coordinate in: \"{}\"",
                                        point);
                            }
                            break;
                        }
                        in = new Point(coords[0], coords[1]);
                        if (r_rgun.size() >= MAX_GUN_PTS) {
                            if (verboseShapeParsing) {
                                logger.warn("Too many right rear gun coordinates");
                            }
                        } else {
                            r_rgun.add(in);
                            if (debugShapeParsing) {
                                logger.warn("right rear gun at {},{}", in.x, in.y);
                            }
                        }
                    }
                    break;

                default:
                    if (verboseShapeParsing) {
                        logger.warn("Invalid ship shape keyword: \"{}\"", keyw);
                    }
	    /* the good thing about this format is that we can just ignore
	     * this.  it is likely to be a new extension we don't know
	     * about yet. */
                    break;
            }
        }

    /* Check for some things being set, and give them defaults if not */
        if (pt.size() < 3) {
            if (verboseShapeParsing) {
                logger.warn("A shipshape must have at least 3 valid points.");
            }
            return -1;
        }

    /* If no main gun set, put at foremost point */
        if (!mainGunSet) {
            max = new Point();
            temp = new Point();
            for (i = 1; i < pt.size(); i++) {
                temp = pt.get(i);
                if (temp.x > max.x
                        || (temp.x == max.x
                        && Math.abs(temp.y) < Math.abs(max.y))) {
                    max = temp;
                }
            }
            m_gun = temp;
            mainGunSet = true;
        }

    /* If no left light set, put at leftmost point */
        if (l_light.isEmpty()) {
            max = new Point();
            for (i = 1; i < pt.size(); i++) {
                temp = pt.get(i);
                if (temp.y > max.y
                        || (temp.y == max.y
                        && temp.x <= max.x)) {
                    max = temp;
                }
            }
            l_light.add(max);
        }

    /* If no right light set, put at rightmost point */
        if (r_light.isEmpty()) {
            max = new Point();
            for (i = 1; i < pt.size(); i++) {
                temp = pt.get(i);
                if (temp.y < max.y
                        || (temp.y == max.y
                        && temp.x <= max.x)) {
                    max = temp;
                }
            }
            r_light.add(max);

        }

    /* If no engine position, put at rear of ship */
        if (!engineSet) {
            max = new Point();
            temp = new Point();
            for (i = 1; i < pt.size(); i++) {
                temp = pt.get(i);
                if (temp.x < max.x) {
                    max = temp;
                }
            }
	/* this may lay outside of ship. */
            engine.x = max.x;
            engine.y = 0;
            engineSet = true;
        }

    /* If no missile racks, put at main gun position*/
        if (m_rack.isEmpty()) {
            m_rack.add(m_gun);
        }

        if (shapeLimits) {
            final int isLow = -8, isHi = 8, isLeft = 8, isRight = -8,
                    minLow = 1, minHi = 1, minLeft = 1, minRight = 1,
                    horMax = 15, verMax = 15, horMin = -15, verMin = -15,
                    minCount = 3, minSize = 22 + 16;
            int low = 0, hi = 0, left = 0, right = 0,
                    count = 0,
                    lowest = 0, highest = 0,
                    leftmost = 0, rightmost = 0;
            boolean invalid = false;
            boolean checkWidthAgainstLongestAxis = true;
            boolean change = false;

            boolean maximum = false;
            for (i = 0; i < pt.size(); i++) {
                temp = pt.get(i);
                x = temp.x;
                y = temp.y;
                change = false;
                if (y >= isLeft) {
                    change = true;
                    left++;
                    if (y > leftmost) {
                        leftmost = y;
                    }
                }
                if (y <= isRight) {
                    change = true;
                    right++;
                    if (y < rightmost) {
                        rightmost = y;
                    }
                }
                if (x <= isLow) {
                    change = true;
                    low++;
                    if (x < lowest) {
                        lowest = x;
                    }
                }
                if (x >= isHi) {
                    change = true;
                    hi++;
                    if (x > highest) {
                        highest = x;
                    }
                }
                if (change) {
                    count++;
                }
                if (y > horMax || y < horMin) {
                    maximum = true;
                }
                if (x > verMax || x < verMin) {
                    maximum = true;
                }
            }
            if (low < minLow
                    || hi < minHi
                    || left < minLeft
                    || right < minRight
                    || count < minCount) {
                if (verboseShapeParsing) {
                    logger.warn("Ship shape does not meet size requirements ({},{},{},{},{})", low, hi, left, right, count);
                }
                return -1;
            }
            if (maximum) {
                if (verboseShapeParsing) {
                    logger.warn("Ship shape exceeds size maxima.");
                }
                return -1;
            }
            if (leftmost - rightmost + highest - lowest < minSize) {
                if (verboseShapeParsing) {
                    logger.warn("Ship shape is not big enough.\n" +
                            "The ship's width and height added together should\n" +
                            "be at least {}.", minSize);
                }
                return -1;
            }

            if (checkWidthAgainstLongestAxis) {
	    /*
	     * For making sure the ship is the right width!
	     */
                int pair[] = new int[2];
                int dist = 0, tmpDist = 0;
                double vec[] = new double[2];
                double width, dTmp;
                final int minWidth = 12;

	    /*
	     * Loop over all the points and find the two furthest apart
	     */
                for (i = 0; i < pt.size(); i++) {
                    for (j = i + 1; j < pt.size(); j++) {
		    /*
		     * Compare the points if they are not the same ones.
		     * Get this distance -- doesn't matter about sqrting
		     * it since only size is important.
		     */
                        tmpDist = ((pt.get(i).x - pt.get(j).x) * (pt.get(i).x - pt.get(j).x) +
                                (pt.get(i).y - pt.get(j).y) * (pt.get(i).y - pt.get(j).y));
                        if (tmpDist > dist) {
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
                vec[0] = (double) (pt.get(pair[1]).y - pt.get(pair[0]).y);
                vec[1] = (double) (pt.get(pair[0]).x - pt.get(pair[1]).x);

	    /*
	     * Normalise
	     */
                dTmp = LENGTH(vec[0], vec[1]);
                vec[0] /= dTmp;
                vec[1] /= dTmp;

	    /*
	     * Now check the width _|_ to the ship main line.
	     */
                for (i = 0, width = dTmp = 0.0; i < pt.size(); i++) {
                    for (j = i + 1; j < pt.size(); j++) {
		    /*
		     * Check the line if the points are not the same ones
		     */
                        width = Math.abs(vec[0] * (double) (pt.get(i).x - pt.get(j).x) +
                                vec[1] * (double) (pt.get(i).y - pt.get(j).y));
                        if (width > dTmp) {
                            dTmp = width;
                        }
                    }
                }

	    /*
	     * And make sure it is nice and far away
	     */
                if (((int) dTmp) < minWidth) {
                    if (verboseShapeParsing) {
                        logger.warn("Ship shape is not big enough.\n" +
                                "The ship's width should be at least {}.\n" +
                                "Player's is {}", minWidth, (int) dTmp);
                    }
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
            grid.Grid_reset();

	/* Draw the ship outline first. */
            for (i = 0; i < pt.size(); i++) {
                j = i + 1;
                if (j == pt.size()) {
                    j = 0;
                }

                grid.Grid_set_value(pt.get(i).x, pt.get(i).y, 1);

                dx = pt.get(j).x - pt.get(i).x;
                dy = pt.get(j).y - pt.get(i).y;
                if (Math.abs(dx) >= Math.abs(dy)) {
                    if (dx > 0) {
                        for (x = pt.get(i).x + 1; x < pt.get(j).x; x++) {
                            y = pt.get(i).y + (dy * (x - pt.get(i).x)) / dx;
                            grid.Grid_set_value(x, y, 1);
                        }
                    } else {
                        for (x = pt.get(j).x + 1; x < pt.get(i).x; x++) {
                            y = pt.get(j).y + (dy * (x - pt.get(j).x)) / dx;
                            grid.Grid_set_value(x, y, 1);
                        }
                    }
                } else {
                    if (dy > 0) {
                        for (y = pt.get(i).y + 1; y < pt.get(j).y; y++) {
                            x = pt.get(i).x + (dx * (y - pt.get(i).y)) / dy;
                            grid.Grid_set_value(x, y, 1);
                        }
                    } else {
                        for (y = pt.get(j).y + 1; y < pt.get(i).y; y++) {
                            x = pt.get(j).x + (dx * (y - pt.get(j).y)) / dy;
                            grid.Grid_set_value(x, y, 1);
                        }
                    }
                }
            }

	/* Check the borders of the grid for blank points. */
            for (y = -15; y <= 15; y++) {
                for (x = -15; x <= 15; x += (y == -15 || y == 15) ? 1 : 2 * 15) {
                    if (grid.Grid_get_value(x, y) == 0) {
                        grid.Grid_add(x, y);
                    }
                }
            }

	/* Check from the borders of the grid to the centre. */
            while (!grid.Grid_is_ready()) {
                Point pos = grid.Grid_get();

                x = pos.x;
                y = pos.y;
                if (x < 15 && grid.Grid_get_value(x + 1, y) == 0) {
                    grid.Grid_add(x + 1, y);
                }
                if (x > -15 && grid.Grid_get_value(x - 1, y) == 0) {
                    grid.Grid_add(x - 1, y);
                }
                if (y < 15 && grid.Grid_get_value(x, y + 1) == 0) {
                    grid.Grid_add(x, y + 1);
                }
                if (y > -15 && grid.Grid_get_value(x, y - 1) == 0) {
                    grid.Grid_add(x, y - 1);
                }
            }

            grid.Grid_print();

	/*
	 * Note that for the engine, old format shapes may well have the
	 * engine position outside the ship, so this check not used for those.
	 */

            if (grid.Grid_point_is_outside_ship(m_gun)) {
                if (verboseShapeParsing) {
                    logger.warn("Main gun (at ({},{})) is outside ship.",
                            m_gun.x, m_gun.y);
                }
                invalid = true;
            }
            for (i = 0; i < l_gun.size(); i++) {
                if (grid.Grid_point_is_outside_ship(l_gun.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Left gun at ({},{}) is outside ship.",
                                l_gun.get(i).x, l_gun.get(i).y);
                    }
                    invalid = true;
                }
            }
            for (i = 0; i < r_gun.size(); i++) {
                if (grid.Grid_point_is_outside_ship(r_gun.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Right gun at ({},{}) is outside ship.",
                                r_gun.get(i).x, r_gun.get(i).y);
                    }
                    invalid = true;
                }
            }
            for (i = 0; i < l_rgun.size(); i++) {
                if (grid.Grid_point_is_outside_ship(l_rgun.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Left rear gun at ({},{}) is outside ship.",
                                l_rgun.get(i).x, l_rgun.get(i).y);
                    }
                    invalid = true;
                }
            }
            for (i = 0; i < r_rgun.size(); i++) {
                if (grid.Grid_point_is_outside_ship(r_rgun.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Right rear gun at ({},{}) is outside ship.",
                                r_rgun.get(i).x, r_rgun.get(i).y);
                    }
                    invalid = true;
                }
            }
            for (i = 0; i < m_rack.size(); i++) {
                if (grid.Grid_point_is_outside_ship(m_rack.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Missile rack at ({},{}) is outside ship.",
                                m_rack.get(i).x, m_rack.get(i).y);
                    }
                    invalid = true;
                }
            }
            for (i = 0; i < l_light.size(); i++) {
                if (grid.Grid_point_is_outside_ship(l_light.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Left light at ({},{}) is outside ship.",
                                l_light.get(i).x, l_light.get(i).y);
                    }
                    invalid = true;
                }
            }
            for (i = 0; i < r_light.size(); i++) {
                if (grid.Grid_point_is_outside_ship(r_light.get(i))) {
                    if (verboseShapeParsing) {
                        logger.warn("Right light at ({},{}) is outside ship.",
                                r_light.get(i).x, r_light.get(i).y);
                    }
                    invalid = true;
                }
            }
            if (grid.Grid_point_is_outside_ship(engine)) {
                if (verboseShapeParsing) {
                    logger.warn("Engine (at ({},{})) is outside ship.",
                            engine.x, engine.y);
                }
                invalid = true;
            }

            if (debugShapeParsing) {
                StringBuilder logTemp = new StringBuilder(32 * 33);
                for (i = -15; i <= 15; i++) {
                    for (j = -15; j <= 15; j++) {
                        switch (grid.Grid_get_value(j, i)) {
                            case 0:
                                logTemp.append(' ');
                                break;
                            case 1:
                                logTemp.append('*');
                                break;
                            case 2:
                                logTemp.append('.');
                                break;
                            default:
                                logTemp.append('?');
                                break;
                        }
                    }
                    logTemp.append('\n');
                }
                logger.warn(logTemp.toString());
            }

            if (invalid) {
                return -1;
            }
        }


        //        shipShapes[0].num_orig_points = pt.size();

        // todo evaluate this hack
//    /*MARA evil hack*/
//    /* always do SSHACK on server, it seems to work */
//    if (is_server) {
//	pt[ship.num_points] = pt[0];
//	for (i = 1; i < ship.num_points; i++)
//	    pt[i + ship.num_points] = pt[ship.num_points - i];
//	ship.num_points = ship.num_points * 2;
//    }
//    /*MARA evil hack*/

//
//    for (i = 1; i < ship.num_points; i++)
//	ship.pts[i] = ship.pts[i - 1][RES];
//
//    for (i = 1; i < l_gun.size(); i++)
//	ship.l_gun[i] = ship.l_gun[i - 1][RES];
//
//    for (i = 1; i < r_gun.size(); i++)
//	ship.r_gun[i] = ship.r_gun[i - 1][RES];
//
//    for (i = 1; i < l_rgun.size(); i++)
//	ship.l_rgun[i] = ship.l_rgun[i - 1][RES];
//
//    for (i = 1; i < r_rgun.size(); i++)
//	ship.r_rgun[i] = ship.r_rgun[i - 1][RES];
//
//    for (i = 1; i < l_light.size(); i++)
//	ship.l_light[i] = ship.l_light[i - 1][RES];
//
//    for (i = 1; i < r_light.size(); i++)
//	ship.r_light[i] = ship.r_light[i - 1][RES];
//
//    for (i = 1; i < m_rack.size(); i++)
//	ship.m_rack[i] = ship.m_rack[i - 1][RES];

        // Clear existing data
        shipShapes[0] = new SShape();


        for (i = 0; i < pt.size(); i++) {
            Ship_set_point_ipos(i, pt.get(i));
        }

        if (engineSet) {
            Ship_set_engine_ipos(engine);
        }

        if (mainGunSet) {
            Ship_set_m_gun_ipos(m_gun);
        }

        for (i = 0; i < l_gun.size(); i++) {
            Ship_set_l_gun_ipos(i, l_gun.get(i));
        }

        for (i = 0; i < r_gun.size(); i++) {
            Ship_set_r_gun_ipos(i, r_gun.get(i));
        }

        for (i = 0; i < l_rgun.size(); i++) {
            Ship_set_l_rgun_ipos(i, l_rgun.get(i));
        }

        for (i = 0; i < r_rgun.size(); i++) {
            Ship_set_r_rgun_ipos(i, r_rgun.get(i));
        }

        for (i = 0; i < l_light.size(); i++) {
            Ship_set_l_light_ipos(i, l_light.get(i));
        }

        for (i = 0; i < r_light.size(); i++) {
            Ship_set_r_light_ipos(i, r_light.get(i));
        }

        for (i = 0; i < m_rack.size(); i++) {
            Ship_set_m_rack_ipos(i, m_rack.get(i));
        }

        shipShapes[0].name = name;
        shipShapes[0].author = author;
        Rotate_ship();

        return 0;
    }

 public    boolean do_parse_shape(String str) {

        if (str == null || str.isEmpty()) {
            if (debugShapeParsing) {
                logger.warn("shape str not set");
            }
            Default_ship();
            return false;
        }

        if (shape2wire(str) != 0) {
            if (debugShapeParsing) {
                logger.warn("shape2wire failed");
            }
            Default_ship();
            return false;
        }
        if (debugShapeParsing) {
            logger.warn("shape2wire succeeded");
        }

        return true;
    }

    void Free_ship_shape() {
        Default_ship();

    }

    void Parse_shape_str(String str) {
        // todo is_server
//    if (is_server)
//    {
//        verboseShapeParsing = debugShapeParsing;
//    }
//    else
//    {
//        verboseShapeParsing = true;
//    }
        verboseShapeParsing = true;
        shapeLimits = true;
        do_parse_shape(str);
    }

    void Convert_shape_str(String str) {
        verboseShapeParsing = debugShapeParsing;
        shapeLimits = debugShapeParsing;
        do_parse_shape(str);
    }

    /*
     * Returns 0 if ships is not valid, 1 if valid.
     */
    boolean Validate_shape_str(String str) {
        ShipShape ship = new ShipShape();

        verboseShapeParsing = true;
        shapeLimits = true;
        return ship.do_parse_shape(str);
    }

    /**
     * Return critical ship data as a string
     * todo what about author & name?
     */
    public String[] Convert_ship_2_string(int shape_version) {
        StringBuilder tmp;
        StringBuilder buf = new StringBuilder(300);
        StringBuilder ext= new StringBuilder();
        int i;


        if (shape_version >= 0x3200) {
            Point2D engine, m_gun;

            buf.append("(SH:");
            for (i = 0; i < shipShapes[0].pts.size() && i < MAX_SHIP_PTS; i++) {
                Point2D pt = Ship_get_point_position(i, 0);

                buf.append(String.format(" %d,%d",Math.round(pt.getX()), Math.round(pt.getY())));
            }
            engine = Ship_get_engine_position(0);
            m_gun = Ship_get_m_gun_position(0);
            buf.append(String.format(")(EN: %d,%d)(MG: %d,%d)", Math.round(engine.getX()), Math.round(engine.getY()),
                    Math.round(m_gun.getX()), Math.round(m_gun.getY())));

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
            if (shipShapes[0].l_gun.size() > 0) {
                tmp = new StringBuilder("(LG:");
                for (i = 0; i < shipShapes[0].l_gun.size() && i < MAX_GUN_PTS; i++) {
                    Point2D l_gun = Ship_get_l_gun_position(i, 0);

                    tmp.append(String.format(" %d,%d",Math.round(l_gun.getX()), Math.round(l_gun.getY())));
                }
                tmp.append(')');
                if (buf.length() + tmp.length() < MSG_LEN) {
                    buf.append(tmp);
                } else if (ext.length() + tmp.length() < MSG_LEN) {
                    ext.append(tmp);
                }
            }
            if (shipShapes[0].r_gun.size() > 0) {
                tmp = new StringBuilder("(RG:");
                for (i = 0; i < shipShapes[0].r_gun.size() && i < MAX_GUN_PTS; i++) {
                    Point2D r_gun = Ship_get_r_gun_position(i, 0);

                    tmp.append(String.format(" %d,%d",Math.round(r_gun.getX()), Math.round(r_gun.getY())));
                }
                tmp.append(')');
                if (buf.length() + tmp.length() < MSG_LEN) {
                    buf.append(tmp);
                } else if (ext.length() + tmp.length() < MSG_LEN) {
                    ext.append(tmp);
                }
            }
            if (shipShapes[0].l_rgun.size() > 0) {
                tmp = new StringBuilder("(LR:");
                for (i = 0; i < shipShapes[0].l_rgun.size() && i < MAX_GUN_PTS; i++) {
                    Point2D l_rgun = Ship_get_l_rgun_position(i, 0);

                    tmp.append(String.format(" %d,%d",Math.round(l_rgun.getX()), Math.round(l_rgun.getY())));
                }
                tmp.append(')');
                if (buf.length() + tmp.length() < MSG_LEN) {
                    buf.append(tmp);
                } else if (ext.length() + tmp.length() < MSG_LEN) {
                    ext.append(tmp);
                }
            }
            if (shipShapes[0].r_rgun.size() > 0) {
                tmp = new StringBuilder("(RR:");
                for (i = 0; i < shipShapes[0].r_rgun.size() && i < MAX_GUN_PTS; i++) {
                    Point2D r_rgun = Ship_get_r_rgun_position(i, 0);

                    tmp.append(String.format(" %d,%d",Math.round(r_rgun.getX()), Math.round(r_rgun.getY())));
                }
                tmp.append(')');
                if (buf.length() + tmp.length() < MSG_LEN) {
                    buf.append(tmp);
                } else if (ext.length() + tmp.length() < MSG_LEN) {
                    ext.append(tmp);
                }
            }
            if (shipShapes[0].l_light.size() > 0) {
                tmp = new StringBuilder("(LL:");
                for (i = 0; i < shipShapes[0].l_light.size() && i < MAX_LIGHT_PTS; i++) {
                    Point2D l_light = Ship_get_l_light_position(i, 0);

                    tmp.append(String.format(" %d,%d",Math.round(l_light.getX()), Math.round(l_light.getY())));

                }
                tmp.append(')');
                if (buf.length() + tmp.length() < MSG_LEN) {
                    buf.append(tmp);
                } else if (ext.length() + tmp.length() < MSG_LEN) {
                    ext.append(tmp);
                }
            }
        }
        if (shipShapes[0].r_light.size() > 0) {
            tmp = new StringBuilder("(RL:");
            for (i = 0; i < shipShapes[0].r_light.size() && i < MAX_LIGHT_PTS; i++) {
                Point2D r_light = Ship_get_r_light_position(i, 0);

                tmp.append(String.format(" %d,%d",Math.round(r_light.getX()), Math.round(r_light.getY())));

            }
            tmp.append(')');
            if (buf.length() + tmp.length() < MSG_LEN) {
                buf.append(tmp);
            } else if (ext.length() + tmp.length() < MSG_LEN) {
                ext.append(tmp);
            }
        }

        if (shipShapes[0].m_rack.size() > 0) {
            tmp = new StringBuilder("(MR:");
            for (i = 0; i < shipShapes[0].m_rack.size() && i < MAX_RACK_PTS; i++) {
                Point2D m_rack = Ship_get_m_rack_position(i, 0);

                tmp.append(String.format(" %d,%d",Math.round(m_rack.getX()), Math.round(m_rack.getY())));
            }
            tmp.append(')');
            if (buf.length() + tmp.length() < MSG_LEN) {
                buf.append(tmp);
            } else if (ext.length() + tmp.length() < MSG_LEN) {
                ext.append(tmp);
            }


        } else {
            buf = new StringBuilder();
        }

        if (buf.length() >= MSG_LEN || ext.length() >= MSG_LEN) {
            logger.warn("BUG: convert ship: buffer overflow ({},{})", buf.length(), ext.length());
        }

        if (debugShapeParsing) {
            logger.warn("ship 2 str: {} {}", buf, ext);
        }

        return new String [] {buf.toString(),ext.toString()};
    }

    public enum SSKeys {
        SHAPE("SH", "shape"),
        MAINGUN("MG", "mainGun"),
        LEFTGUN("LG", "leftGun"),
        RIGHTGUN("RG", "rightGun"),
        LEFTLIGHT("LL", "leftLight"),
        RIGHTLIGHT("RL", "rightLight"),
        ENGINE("EN", "engine"),
        MISSILERACK("MR", "missileRack"),
        NAME("NM", "name"),
        AUTHOR("AU", "author"),
        LEFTREARGUN("LR", "leftRearGun"),
        RIGHTREARGUN("RR", "rightRearGun");
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

        private static final Map<String, SSKeys> lookup
                = new HashMap<>();

        static {
            for (SSKeys s : EnumSet.allOf(SSKeys.class)) {
                // Assert for duplicates.  Should never happen.
                if (lookup.put(s.getShortName().toUpperCase(), s) != null){
                    logger.warn("Duplicate enum {} with shortname '{}'",s,s.getShortName());
                }
                if (lookup.put(s.getLongName().toUpperCase(), s) != null){
                    logger.warn("Duplicate enum {} with longname '{}'",s,s.getLongName());
                }
            }
        }


        /**
         * Case insensitive lookup by shortname or longname
         *
         * @param name
         * @return
         */
        public static SSKeys lookup(String name) {
            return lookup.get(name.toUpperCase());
        }
    }


    void Calculate_shield_radius(ShipShape ship) {
        int i;
        int radius2, max_radius = 0;

        for (i = 0; i < shipShapes[0].pts.size(); i++) {
            Point2D pti = Ship_get_point_position(i, 0);
            radius2 = (int) (Math.pow(pti.getX() + (pti.getY() * pti.getY()), 2.0));
            if (radius2 > max_radius) {
                max_radius = radius2;
            }
        }
        max_radius = (int) (2.0 * sqrt((double) max_radius));
        shipShapes[0].shield_radius = (max_radius + 2 <= 34)
                ? 34
                : (max_radius + 2 - (max_radius & 1));
    }

}
