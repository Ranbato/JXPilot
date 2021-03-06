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

#ifndef	TUNER_H
#define	TUNER_H

#ifndef MAP_H
# include "map.h"
#endif

void tuner_plock();
void tuner_shipmass();
void tuner_ballmass();
void tuner_maxrobots();
void tuner_minrobots();
void tuner_allowshields();
void tuner_playerstartsshielded();
void tuner_worldlives();
void tuner_cannonsmartness();
void tuner_teamcannons();
void tuner_mincannonshotlife();
void tuner_maxcannonshotlife();
void tuner_wormhole_stable_ticks();
void tuner_modifiers();
void tuner_gameduration();
void tuner_racelaps();
void tuner_allowalliances();
void tuner_announcealliances();
void tuner_playerwallbouncetype();

#endif
