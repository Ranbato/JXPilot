package org.xpilot.common;/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 2000-2004 by
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

import java.awt.*;

import static org.xpilot.common.Const.BLOCK_SZ;

public class Click/* extends Point*/{

/*
 * The wall collision detection routines depend on repeatability
 * (getting the same result even after some "neutral" calculations)
 * and an exact determination whether a point is in space,
 * inside the wall (crash!) or on the edge.
 * This will be hard to achieve if only floating point would be used.
 * However, a resolution of a pixel is a bit rough and ugly.
 * Therefore a fixed point sub-pixel resolution is used called clicks.
 */

int cx;
int cy;


static final int CLICK_SHIFT	=	6;
static final int CLICK		=	(1 << CLICK_SHIFT);
    static final int PIXEL_CLICKS	=	CLICK;
    static final int BLOCK_CLICKS	=	(BLOCK_SZ << CLICK_SHIFT);

    public Click(Point pos) {
        this.cx = pos.x;
        this.cy = pos.y;
    }

    public Click() {
    }

    public int CLICK_TO_PIXEL(int C)	{return (int)((C) >> CLICK_SHIFT);}
static int CLICK_TO_BLOCK(int C)	{return (int)((C) / (BLOCK_SZ << CLICK_SHIFT));}
    public float CLICK_TO_FLOAT(int C)	{return (float)(C) * (1.0f / CLICK);}
    public int PIXEL_TO_CLICK(int I)	{return (I) << CLICK_SHIFT;}
    public int FLOAT_TO_CLICK(float F)	{return (int)((F) * CLICK);}

/*
 * Return the block position this click position is in.Math.
 */
static Dimension Clpos_to_blkpos(Click  pos)
{
    Dimension bpos = new Dimension(CLICK_TO_BLOCK(pos.cx),CLICK_TO_BLOCK(pos.cy));

    return bpos;
}

static int BLOCK_CENTER(int B) {return (int)((B) * BLOCK_CLICKS) + BLOCK_CLICKS / 2;}

/* calculate the clpos of the center of a block */
static  Click  Block_get_center_clpos(Dimension bpos)
{
    Click  pos = new Click();

    pos.cx = (int) ((bpos.getWidth() * BLOCK_CLICKS) + BLOCK_CLICKS / 2);
    pos.cy = (int) ((bpos.getHeight() * BLOCK_CLICKS) + BLOCK_CLICKS / 2);

    return pos;
}

}
