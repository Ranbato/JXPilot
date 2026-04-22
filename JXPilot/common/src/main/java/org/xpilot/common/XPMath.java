package org.xpilot.common;/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 2003-2004 by
 *
 *      Uoti Urpala          <uau@users.sourceforge.net>
 *      Kristian Söderblom   <kps@users.sourceforge.net>
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

import org.xpilot.common.Const;

import java.awt.*;
import java.util.List;

import static java.lang.Math.hypot;

public class XPMath{

static final double		tbl_sin[] = new double[Const.TABLE_SIZE];
static final double		tbl_cos[] = new double [Const.TABLE_SIZE];


  /* New table lookup with optional range checking and no extra calculations. */
public static double tsin(int x)	{return tbl_sin[x];}
    public static double tcos(int x)	{return tbl_cos[x];}

public int NELEM(List a)	{return a.size();}

    public static double DELTA(double a,double b)		{return((a) >= (b)) ? ((a) - (b)) : ((b) - (a));}
    public static double LENGTH(double x,double y)		{return  hypot( (double) (x), (double) (y) ) ;}
    public static double VECTOR_LENGTH(Point v)	{return  hypot( (double) (v).x, (double) (v).y );}
public static double QUICK_LENGTH(double x,double y)	{ return Math.abs(x)+Math.abs(y); } /*-BA Only approx, but v. quick */
    public static double LIMIT(double val,double lo,double hi)	{ return (val)>(hi)?(hi):((val)<(lo)?(lo):(val)); }

static final public int MOD2(int x,int m){		return( (x) & ((m) - 1) );}

boolean ON(String optval)
{
    return ("true".equalsIgnoreCase(optval)
	    || "on".equalsIgnoreCase(optval)
	    || "yes".equalsIgnoreCase(optval));
}


boolean OFF(String optval)
{
    return ("false".equalsIgnoreCase(optval)
	    || "off".equalsIgnoreCase(optval)
	    || "no".equalsIgnoreCase(optval));
}


int mod(int x, int y)
{
    if (x >= y || x < 0)
	x = x - y*(x/y);

    if (x < 0)
	x += y;

    return x;
}

double findDir(double x, double y)
{
    double angle;

    if (x != 0.0 || y != 0.0)
	angle = Math.atan2(y, x) / (2 * Math.PI);
    else
	angle = 0.0;

    if (angle < 0)
	angle++;
    return angle * Const.RES;
}


static
{

    for (int i = 0; i < Const.TABLE_SIZE; i++) {
	tbl_sin[i] = Math.sin(i * (2.0 * Math.PI / Const.TABLE_SIZE));
	tbl_cos[i] = Math.cos(i * (2.0 * Math.PI / Const.TABLE_SIZE));
    }
}
}
