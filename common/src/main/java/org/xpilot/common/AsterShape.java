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

public static final int WRECKAGE_SHAPE_0 = 
      {-9, 6}, {-2, 8}, { 5, 2}, { 9, 3}, {10, 0}, { 5,-1}, \
      { 3, 0}, {-2,-9}, {-5,-6}, {-3,-2}, {-7,-1}, {-5, 2}

public static final int WRECKAGE_SHAPE_1 = 
      {-8,-9}, {-9,-3}, {-7, 3}, {-1, 7}, { 8, 9}, { 9, 6}, \
      { 2, 5}, {-2, 2}, { 4,-1}, { 2,-5}, { 0,-2}, {-5,-2}

public static final int WRECKAGE_SHAPE_2 = 
      {-9,-2}, {-7, 2}, {-2,-3}, { 2,-3}, { 0, 1}, { 1,10}, \
      { 4, 9}, { 4, 2}, { 7,-2}, { 7,-5}, { 2,-8}, {-4,-7}

static final public  Point wreckageShapes = new Point[NUM_WRECKAGE_SHAPES][NUM_WRECKAGE_POINTS];

static final public  Point asteroidShapes = new Point[NUM_ASTEROID_SHAPES][NUM_ASTEROID_POINTS];

}
