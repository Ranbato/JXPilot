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

#ifndef	NETCLIENT_H
#define	NETCLIENT_H

#ifndef TYPES_H
/* need u_byte */
#include "types.h"
#endif

#define MIN_RECEIVE_WINDOW_SIZE		1
#define MAX_RECEIVE_WINDOW_SIZE		4

#define MAX_SUPPORTED_FPS		255

typedef struct {
    int view_width;
    int view_height;
    int spark_rand;
    int num_spark_colors;
} display_t;

extern int	 receive_window_size;
extern long	 last_loops;
extern bool      packetMeasurement;
extern display_t server_display; /* the servers idea about our display */

typedef struct {
    int movement;
    double turnspeed;
    int id;
} pointer_move_t;

#define MAX_POINTER_MOVES 128

extern pointer_move_t pointer_moves[MAX_POINTER_MOVES];
extern int pointer_move_next;
extern long last_keyboard_ack;
extern bool dirPrediction;

int Net_setup();
int Net_verify(String real, String nick, String dpy);
int Net_init(String server, int port);
void Net_cleanup();
void Net_key_change();
int Net_flush();
int Net_fd();
int Net_start();
void Net_init_measurement();
void Net_init_lag_measurement();
int Net_input();
/* void Net_measurement(long loop, int status);*/
int Receive_start();
int Receive_end();
int Receive_message();
int Receive_self();
int Receive_self_items();
int Receive_modifiers();
int Receive_refuel();
int Receive_connector();
int Receive_laser();
int Receive_missile();
int Receive_ball();
int Receive_ship();
int Receive_mine();
int Receive_item();
int Receive_destruct();
int Receive_shutdown();
int Receive_thrusttime();
int Receive_shieldtime();
int Receive_phasingtime();
int Receive_rounddelay();
int Receive_debris();
int Receive_wreckage();
int Receive_asteroid();
int Receive_wormhole();
int Receive_polystyle();
int Receive_fastshot();
int Receive_ecm();
int Receive_trans();
int Receive_paused();
int Receive_appearing();
int Receive_radar();
int Receive_fastradar();
int Receive_damaged();
int Receive_leave();
int Receive_war();
int Receive_seek();
int Receive_player();
int Receive_team();
int Receive_score();
int Receive_score_object();
int Receive_team_score();
int Receive_timing();
int Receive_fuel();
int Receive_cannon();
int Receive_target();
int Receive_base();
int Receive_reliable();
int Receive_quit();
int Receive_string();
int Receive_reply(int *replyto, int *result);
int Send_ack(long rel_loops);
int Send_keyboard(u_byte *);
int Send_shape(String );
int Send_power(double pwr);
int Send_power_s(double pwr_s);
int Send_turnspeed(double turnspd);
int Send_turnspeed_s(double turnspd_s);
int Send_turnresistance(double turnres);
int Send_turnresistance_s(double turnres_s);
int Send_pointer_move(int movement);
int Receive_audio();
int Receive_talk_ack();
int Send_talk();
int Send_display(int width, int height, int sparks, int spark_colors);
int Send_modifier_bank(int);
int Net_talk(String str);
int Net_ask_for_motd(long offset, long maxlen);
int Receive_time_left();
int Receive_eyes();
int Receive_motd();
int Receive_magic();
int Send_audio_request(int on);
int Send_fps_request(int fps);
int Receive_loseitem();

#endif
