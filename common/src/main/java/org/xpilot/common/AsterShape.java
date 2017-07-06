package org.xpilot.common;


import java.awt.*;

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
public class AsterShape {

public static final int NUM_ASTEROID_SHAPES = 2;
public static final int NUM_ASTEROID_POINTS = 12;

public static final Point[] ASTEROID_SHAPE_0 = new Point[] {
      new Point(-10,0), new Point(-7,6), new Point(-2,8), new Point(0,10), new Point(5,8), new Point(9,4), 
      new Point(10,0), new Point(7,-5), new Point(6,-9), new Point(0,-10), new Point(-5,-7), new Point(-7,-5)};


public static final Point [] ASTEROID_SHAPE_1 = new Point[] {
      new Point(-10,0), new Point(-8,7), new Point(-4,9), new Point(0,10), new Point(5,7), new Point(6,3), 
      new Point(10,0), new Point(9,-4), new Point(7,-7), new Point(0,-10), new Point(-6,-9), new Point(-9,-7)};

public static final int NUM_WRECKAGE_SHAPES = 3;
public static final int NUM_WRECKAGE_POINTS = 12;

public static final Point[] WRECKAGE_SHAPE_0 = new Point[] {
      new Point( -9, 6), new Point( -2, 8), new Point( 5, 2), new Point( 9, 3), new Point( 10, 0), new Point( 5, -1),
      new Point( 3, 0), new Point( -2, -9), new Point( -5, -6), new Point( -3, -2), new Point( -7, -1), new Point( -5, 2)};

public static final Point[] WRECKAGE_SHAPE_1 = new Point[] {
      new Point( -8, -9), new Point( -9, -3), new Point( -7, 3), new Point( -1, 7), new Point( 8, 9), new Point( 9, 6),
      new Point( 2, 5), new Point( -2, 2), new Point( 4, -1), new Point( 2, -5), new Point( 0, -2), new Point( -5, -2)};

public static final Point[] WRECKAGE_SHAPE_2 = new Point[] {
      new Point( -9, -2), new Point( -7, 2), new Point( -2, -3), new Point( 2, -3), new Point( 0, 1), new Point( 1, 10),
      new Point( 4, 9), new Point( 4, 2), new Point( 7, -2), new Point( 7, -5), new Point( 2, -8), new Point( -4, -7)};

static final public  Point wreckageShapes[][] = new Point[NUM_WRECKAGE_SHAPES][NUM_WRECKAGE_POINTS];

static final public  Point asteroidShapes[][] = new Point[NUM_ASTEROID_SHAPES][NUM_ASTEROID_POINTS];

}
