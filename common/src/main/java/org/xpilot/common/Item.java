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


public enum Item {
    NO_ITEM(-1),
    ITEM_FUEL(0),
    ITEM_WIDEANGLE(1),
    ITEM_REARSHOT(2),
    ITEM_AFTERBURNER(3),
    ITEM_CLOAK(4),
    ITEM_SENSOR(5),
    ITEM_TRANSPORTER(6),
    ITEM_TANK(7),
    ITEM_MINE(8),
    ITEM_MISSILE(9),
    ITEM_ECM(10),
    ITEM_LASER(11),
    ITEM_EMERGENCY_THRUST(12),
    ITEM_TRACTOR_BEAM(13),
    ITEM_AUTOPILOT(14),
    ITEM_EMERGENCY_SHIELD(15),
    ITEM_DEFLECTOR(16),
    ITEM_HYPERJUMP(17),
    ITEM_PHASING(18),
    ITEM_MIRROR(19),
    ITEM_ARMOR(20),
    NUM_ITEMS(21);
    
    int ord;
    
     Item(int ord){
        this.ord = ord;
    }

public static final long ITEM_BIT_FUEL = (1L << ITEM_FUEL.ord);
public static final long ITEM_BIT_WIDEANGLE = (1L << ITEM_WIDEANGLE.ord);
public static final long ITEM_BIT_REARSHOT = (1L << ITEM_REARSHOT.ord);
public static final long ITEM_BIT_AFTERBURNER = (1L << ITEM_AFTERBURNER.ord);
public static final long ITEM_BIT_CLOAK = (1L << ITEM_CLOAK.ord);
public static final long ITEM_BIT_SENSOR = (1L << ITEM_SENSOR.ord);
public static final long ITEM_BIT_TRANSPORTER = (1L << ITEM_TRANSPORTER.ord);
public static final long ITEM_BIT_TANK = (1L << ITEM_TANK.ord);
public static final long ITEM_BIT_MINE = (1L << ITEM_MINE.ord);
public static final long ITEM_BIT_MISSILE = (1L << ITEM_MISSILE.ord);
public static final long ITEM_BIT_ECM = (1L << ITEM_ECM.ord);
public static final long ITEM_BIT_LASER = (1L << ITEM_LASER.ord);
public static final long ITEM_BIT_EMERGENCY_THRUST = (1L << ITEM_EMERGENCY_THRUST.ord);
public static final long ITEM_BIT_TRACTOR_BEAM = (1L << ITEM_TRACTOR_BEAM.ord);
public static final long ITEM_BIT_AUTOPILOT = (1L << ITEM_AUTOPILOT.ord);
public static final long ITEM_BIT_EMERGENCY_SHIELD = (1L << ITEM_EMERGENCY_SHIELD.ord);
public static final long ITEM_BIT_DEFLECTOR = (1L << ITEM_DEFLECTOR.ord);
public static final long ITEM_BIT_HYPERJUMP = (1L << ITEM_HYPERJUMP.ord);
public static final long ITEM_BIT_PHASING = (1L << ITEM_PHASING.ord);
public static final long ITEM_BIT_MIRROR = (1L << ITEM_MIRROR.ord);
public static final long ITEM_BIT_ARMOR = (1L << ITEM_ARMOR.ord);

/* Each item is ITEM_SIZE x ITEM_SIZE */
public static final long ITEM_SIZE = 16;

public static final long ITEM_TRIANGLE_SIZE = (5*ITEM_SIZE/7 + 1);

}
