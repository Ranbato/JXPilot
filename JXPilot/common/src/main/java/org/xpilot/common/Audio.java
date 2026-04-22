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


enum Audio {
    START_SOUND					("start"                  ),
    FIRE_SHOT_SOUND				("fire_shot"              ),
    FIRE_TORPEDO_SOUND			("fire_torpedo"           ),
    FIRE_HEAT_SHOT_SOUND		("fire_heat_shot"         ),
    FIRE_SMART_SHOT_SOUND		("fire_smart_shot"        ),
    PLAYER_EXPLOSION_SOUND		("player_explosion"       ),
    PLAYER_HIT_PLAYER_SOUND		("player_hit_player"      ),
    PLAYER_HIT_CANNON_SOUND		("player_hit_cannon"      ),
    PLAYER_HIT_MINE_SOUND		("player_hit_mine"        ),
    PLAYER_EAT_TORPEDO_SHOT_SOUND("player_eat_torpedo_shot"),
    PLAYER_EAT_HEAT_SHOT_SOUND	("player_eat_heat_shot"   ),
    PLAYER_EAT_SMART_SHOT_SOUND	("player_eat_smart_shot"  ),
    DROP_MINE_SOUND				("drop_mine"              ),
    PLAYER_HIT_WALL_SOUND		("player_hit_wall"        ),
    WORM_HOLE_SOUND				("worm_hole"              ),
    WIDEANGLE_SHOT_PICKUP_SOUND	("wideangle_shot_pickup"  ),
    SENSOR_PACK_PICKUP_SOUND	("sensor_pack_pickup"     ),
    BACK_SHOT_PICKUP_SOUND		("back_shot_pickup"       ),
    ROCKET_PACK_PICKUP_SOUND	("rocket_pack_pickup"     ),
    CLOAKING_DEVICE_PICKUP_SOUND("cloaking_device_pickup" ),
    ENERGY_PACK_PICKUP_SOUND	("energy_pack_pickup"     ),
    MINE_PACK_PICKUP_SOUND		("mine_pack_pickup"       ),
    REFUEL_SOUND				("refuel"                 ),
    THRUST_SOUND				("thrust"                 ),
    CLOAK_SOUND					("cloak"                  ),
    CHANGE_HOME_SOUND			("change_home"            ),
    ECM_PICKUP_SOUND			("ecm_pickup"             ),
    AFTERBURNER_PICKUP_SOUND	("afterburner_pickup"     ),
    TANK_PICKUP_SOUND			("tank_pickup"            ),
    DROP_MOVING_MINE_SOUND		("drop_moving_mine"       ),
    MINE_EXPLOSION_SOUND		("mine_explosion"         ),
    ECM_SOUND					("ecm"                    ),
    TANK_DETACH_SOUND			("tank_detach"            ),
    CANNON_FIRE_SOUND			("cannon_fire"            ),
    PLAYER_SHOT_THEMSELF_SOUND	("player_shot_themself"   ),
    DECLARE_WAR_SOUND			("declare_war"            ),
    PLAYER_HIT_CANNONFIRE_SOUND	("player_hit_cannonfire"  ),
    OBJECT_EXPLOSION_SOUND		("object_explosion"       ),
    PLAYER_EAT_SHOT_SOUND		("player_eat_shot"        ),
    TRANSPORTER_PICKUP_SOUND	("transporter_pickup"     ),
    TRANSPORTER_SUCCESS_SOUND	("transporter_success"    ),
    TRANSPORTER_FAIL_SOUND		("transporter_fail"       ),
    DEFLECTOR_PICKUP_SOUND		("deflector_pickup"       ),
    DEFLECTOR_SOUND				("deflector"              ),
    HYPERJUMP_PICKUP_SOUND		("hyperjump_pickup"       ),
    HYPERJUMP_SOUND				("hyperjump"              ),
    PHASING_DEVICE_PICKUP_SOUND	("phasing_device_pickup"  ),
    PHASING_ON_SOUND			("phasing_on"             ),
    PHASING_OFF_SOUND			("phasing_off"            ),
    MIRROR_PICKUP_SOUND			("mirror_pickup"          ),
    ARMOR_PICKUP_SOUND			("armor_pickup"           ),
    NUKE_LAUNCH_SOUND			("nuke_launch"            ),
    NUKE_EXPLOSION_SOUND		("nuke_explosion"         ),
    PLAYER_RAN_OVER_PLAYER_SOUND("player_ran_over_player" ),
    LASER_PICKUP_SOUND			("laser_pickup"           ),
    EMERGENCY_THRUST_PICKUP_SOUND("emergency_thrust_pickup"),
    AUTOPILOT_PICKUP_SOUND		("autopilot_pickup"       ),
    TRACTOR_BEAM_PICKUP_SOUND	("tractor_beam_pickup"    ),
    PLAYER_BOUNCED_SOUND		("player_bounced"         ),
    FIRE_LASER_SOUND			("fire_laser"             ),
    AUTOPILOT_ON_SOUND			("autopilot_on"           ),
    AUTOPILOT_OFF_SOUND			("autopilot_off"          ),
    EMERGENCY_THRUST_ON_SOUND	("emergency_thrust_on"    ),
    EMERGENCY_THRUST_OFF_SOUND	("emergency_thrust_off"   ),
    TRACTOR_BEAM_SOUND			("tractor_beam"           ),
    PRESSOR_BEAM_SOUND			("pressor_beam"           ),
    CONNECT_BALL_SOUND			("connect_ball"           ),
    DROP_BALL_SOUND				("drop_ball"              ),
    EXPLODE_BALL_SOUND			("explode_ball"           ),
    DESTROY_BALL_SOUND			("destroy_ball"           ),
    DESTROY_TARGET_SOUND		("destroy_target"         ),
    TEAM_WIN_SOUND				("team_win"               ),
    TEAM_DRAW_SOUND				("team_draw"              ),
    PLAYER_WIN_SOUND			("player_win"             ),
    PLAYER_DRAW_SOUND			("player_draw"            ),
    PLAYER_ROASTED_SOUND		("player_roasted"         ),
    PLAYER_EAT_LASER_SOUND		("player_eat_laser"       ),
    EMERGENCY_SHIELD_PICKUP_SOUND("emergency_shield_pickup"),
    EMERGENCY_SHIELD_ON_SOUND	("emergency_shield_on"    ),
    EMERGENCY_SHIELD_OFF_SOUND	("emergency_shield_off"   ),
    CANNON_EXPLOSION_SOUND		("cannon_explosion"       ),
    ASTEROID_HIT_SOUND			("asteroid_hit"           ),
    ASTEROID_BREAK_SOUND		("asteroid_break"         ),
    MAX_SOUNDS("MAX_SOUNDS");

    public String getSoundName()
    {
        return soundName;
    }

    String  soundName = "";

    Audio(String soundName)
    {
        this.soundName = soundName;
    }
}
