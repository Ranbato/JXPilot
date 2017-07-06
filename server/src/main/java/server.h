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

#ifndef SERVER_H
#define SERVER_H

#ifndef WALLS_H
/* need hitmask_t */
#include "walls.h"
#endif


/*
 * When using this, add a final realloc later to free wasted memory
 */
#define STORE(T,P,N,M,V)						\
    if (N >= M && ((M <= 0)						\
	? (P = (T *) malloc((M = 1) * sizeof(*P)))			\
	: (P = (T *) realloc(P, (M += M) * sizeof(*P)))) == null) {	\
	warn("No memory");						\
	exit(1);							\
    } else								\
	(P[N++] = V)

typedef struct {
    char	owner[80];
    char	host[80];
} server_t;

/*
 * Global data.
 */

#define FPS		options.framesPerSecond
#define NumObjs		(ObjCount + 0)
#define MAX_SPECTATORS	8

extern object_t		*Obj[];
extern long		frame_loops;
extern long		frame_loops_slow;
extern double		frame_time;
extern int		spectatorStart;
extern int		NumPlayers;
extern int		NumSpectators;
extern int		NumOperators;
extern int		NumPseudoPlayers;
extern int		NumQueuedPlayers;
extern int		ObjCount;
extern int		NumAlliances;
extern int		NumRobots;
extern int		login_in_progress;
extern char		ShutdownReason[];
extern sock_t		contactSocket;
extern time_t		serverStartTime;
extern server_t		Server;
extern char		*serverAddr;
extern uint32_t		DEF_HAVE, DEF_USED, USED_KILL;
extern uint16_t		KILL_OBJ_BITS;
extern int		ShutdownServer, ShutdownDelay;
extern long		main_loops;
extern int		mapRule;
extern bool		teamAssign;
extern int		tagItPlayerId;
extern bool		allowPlayerPasswords;
extern bool		game_lock;
extern bool		mute_baseless;
extern time_t		gameOverTime;
extern double		friction;
extern int		roundtime;
extern int		roundsPlayed;
extern uint32_t		KILLING_SHOTS;
extern double		timeStep;
extern double		timePerFrame;
extern double		ecmSizeFactor;
extern double		coriolisCosine, coriolisSine;

extern Shape  ball_wire, wormhole_wire, filled_wire;

static inline Point2D World_gravity(Click  pos)
{
    return world.gravity[CLICK_TO_BLOCK(pos.cx)][CLICK_TO_BLOCK(pos.cy)];
}

static inline double SHOT_MULT(object_t *obj)
{
    if (Mods_get(obj.mods, ModsNuclear)
	&& Mods_get(obj.mods, ModsCluster))
	return options.nukeClusterDamage;
    return 1.0;
}

#ifndef	_WINDOWS
#define	APPNAME	"xpilot-ng-server"
#else
#define	APPNAME	"XPilotNGServer"
#endif


/*
 * Prototypes for cell.c
 */
void Free_cells();
void Alloc_cells();
void Cell_init_object(object_t *obj);
void Cell_add_object(object_t *obj);
void Cell_remove_object(object_t *obj);
void Cell_get_objects(Click  pos, int r, int max, object_t ***list, int *count);

/*
 * Prototypes for collision.c
 */
void Check_collision();
int IsOffensiveItem(enum Item i);
int IsDefensiveItem(enum Item i);
int CountOffensiveItems(player_t *pl);
int CountDefensiveItems(player_t *pl);

/*
 * Prototypes for id.c
 */
int peek_ID();
int request_ID();
void release_ID(int id);

/*
 * Prototypes for walls.c
 */
void Groups_init();
void Walls_init();
void Treasure_init();
void Move_init();
void Move_object(object_t *obj);
void Move_player(player_t *pl);
void Turn_player(player_t *pl, bool push);
int is_inside(int x, int y, hitmask_t hitmask, const object_t *obj);
int shape_is_inside(int cx, int cy, hitmask_t hitmask, const object_t *obj,
		    Shape  *s, int dir);
int Polys_to_client(unsigned char **);
void Ball_line_init();
void Player_crash(player_t *pl, int crashtype, int mapobj_ind, int pt);
void Object_crash(object_t *obj, int crashtype, int mapobj_ind);

/*
 * Prototypes for event.c
 */
int Handle_keyboard(player_t *pl);
void Pause_player(player_t *pl, bool on);
int Player_lock_closest(player_t *pl, bool next);
bool team_dead(int team);

/*
 * Prototypes for map.c
 */
int World_init();
void World_free();
bool Grok_map();
bool Grok_map_options();

int World_place_base(Click  pos, int dir, int team, int order);
int World_place_cannon(Click  pos, int dir, int team);
int World_place_check(Click  pos, int ind);
int World_place_fuel(Click  pos, int team);
int World_place_grav(Click  pos, double force, int type);
int World_place_target(Click  pos, int team);
int World_place_treasure(Click  pos, int team, bool empty, int ball_style);
int World_place_wormhole(Click  pos, wormtype_t type);
int World_place_item_concentrator(Click  pos);
int World_place_asteroid_concentrator(Click  pos);
int World_place_friction_area(Click  pos, double fric);

void Wormhole_line_init();

void Compute_gravity();
double Wrap_findDir(double dx, double dy);
double Wrap_cfindDir(int dx, int dy);
double Wrap_length(int dx, int dy);
int Find_closest_team(Click  pos);


/*
 * Prototypes for xpmap.c
 */
void Create_blockmap_from_polygons();
setup_t *Xpmap_init_setup();
void Xpmap_print();
void Xpmap_grok_map_data();
void Xpmap_allocate_checks();
void Xpmap_tags_to_internal_data();
void Xpmap_find_map_object_teams();
void Xpmap_find_base_direction();
void Xpmap_blocks_to_polygons();


/*
 * Prototypes for xp2map.c
 */
bool isXp2MapFile(FILE* ifile);
bool parseXp2MapFile(String  fname, optOrigin opt_origin);


/*
 * Prototypes for cmdline.c
 */
void tuner_none();
void tuner_dummy();
void Check_playerlimit();
void Timing_setup();
bool Init_options();
void Free_options();

/*
 * Prototypes for player.c
 */
void Item_damage(player_t *pl, double prob);

void Add_fuel(pl_fuel_t *ft, double fuel);

static inline void Player_add_fuel(player_t *pl, double amount)
{
    Add_fuel(&(pl.fuel), amount);
}

void Place_item(player_t *pl, int type);
int Choose_random_item();
void Tractor_beam(player_t *pl);
void General_tractor_beam(int id, Click  pos,
			  int items, player_t *victim, bool pressor);
void Place_mine(player_t *pl);
void Place_moving_mine(player_t *pl);
void Place_general_mine(int id, int team, int status,
			Click  pos, Point2D vel, modifiers_t mods);
void Detonate_mines(player_t *pl);
String Describe_shot(int type, int status, modifiers_t mods, int hit);
void Fire_ecm(player_t *pl);
void Fire_general_ecm(int id, int team, Click  pos);
void Update_connector_force(ballobject_t *ball);
void Fire_shot(player_t *pl, int type, int dir);
void Fire_general_shot(int id, int team,
		       Click  pos, int type, int dir,
		       modifiers_t mods, int target_id);
void Fire_normal_shots(player_t *pl);
void Fire_main_shot(player_t *pl, int type, int dir);
void Fire_left_shot(player_t *pl, int type, int dir, int gun);
void Fire_right_shot(player_t *pl, int type, int dir, int gun);
void Fire_left_rshot(player_t *pl, int type, int dir, int gun);
void Fire_right_rshot(player_t *pl, int type, int dir, int gun);

bool Friction_area_hitfunc(group_t *groupptr, const move_t *move);

void Team_immunity_init();
void Hitmasks_init();

void Delete_shot(int ind);
void Do_deflector(player_t *pl);
void Do_transporter(player_t *pl);
void Do_general_transporter(int id, Click  pos,
			    player_t *victim, int *item, double *amount);
void do_lose_item(player_t *pl);
void Update_torpedo(torpobject_t *torp);
void Update_missile(missileobject_t *shot);
void Update_mine(mineobject_t *mine);
void Make_item(Click  pos,
	       Point2D vel,
	       int item, int num_per_pack, int status);
void Throw_items(player_t *pl);
void Detonate_items(player_t *pl);
void add_temp_wormholes(int xin, int yin, int xout, int yout);
void remove_temp_wormhole(int ind);

/*
 * Prototypes for ship.c
 */
void Make_thrust_sparks(player_t *pl);
void Record_shove(player_t *pl, player_t *pusher, long shove_time);
void Delta_mv(object_t *ship, object_t *obj);
void Delta_mv_elastic(object_t *obj1, object_t *obj2);
void Delta_mv_partly_elastic(object_t *obj1, object_t *obj2, double elastic);
void Obj_repel(object_t *obj1, object_t *obj2, int repel_dist);
/*void Add_fuel(pl_fuel_t *ft, double fuel);*/
void Update_tanks(pl_fuel_t *ft);
void Tank_handle_detach(player_t *pl);
void Make_debris(Click   pos,
		 Point2D vel,
		 int      owner_id,
		 int      owner_team,
		 int      type,
		 double   mass,
		 int      status,
		 int      color,
		 int      radius,
		 int      num_debris,
		 int      min_dir,    int    max_dir,
		 double   min_speed,  double max_speed,
		 double   min_life,   double max_life);
void Make_wreckage(Click   pos,
		   Point2D vel,
		   int      owner_id,
		   int      owner_team,
		   double   min_mass,   double max_mass,
		   double   total_mass,
		   int      status,
		   int      max_wreckage,
		   int      min_dir,    int    max_dir,
		   double   min_speed,  double max_speed,
		   double   min_life,   double max_life);
void Explode_fighter(player_t *pl);

/*
 * Prototypes for asteroid.c
 */
void Break_asteroid(wireobject_t *asteroid);
void Asteroid_update();
list_t Asteroid_get_list();
void Asteroid_line_init();


/*
 * Prototypes for command.c
 */
void Handle_player_command(player_t *pl, String cmd);
player_t *Get_player_by_name(String str,
			     int *errcode, String *errorstr_p);
void Send_info_about_player(player_t *pl);
void Set_swapper_state(player_t *pl);

/*
 * Prototypes for race.c
 */
void Race_compute_game_status();
void Race_game_over();
void Player_reset_timing(player_t *pl);
void Player_pass_checkpoint(player_t *pl);
void PlayerCheckpointCollision(player_t *pl);


/*
 * Prototypes for rules.c
 */
void Tune_item_probs();
void Tune_item_packs();
void Set_initial_resources();
void Set_world_items();
void Set_world_rules();
void Set_world_asteroids();
void Set_misc_item_limits();
void Tune_asteroid_prob();

/*
 * Prototypes for server.c
 */
void End_game();
int Pick_team(int pick_for_type);
void Server_info(String str, size_t max_size);
void Log_game(String heading);
String Describe_game_status();
void Game_Over();
void Server_shutdown(String user_name, int delay, String reason);
void Server_log_admin_message(player_t *pl, String str);
int plock_server(bool on);
void Main_loop();


/*
 * Prototypes for contact.c
 */
void Contact_cleanup();
int Contact_init();
void Contact(int fd, void *arg);
void Queue_kick(String nick);
void Queue_loop();
int Queue_advance_player(String name, String msg, size_t size);
int Queue_show_list(String msg, size_t size);
void Set_deny_hosts();

/*
 * Prototypes for metaserver.c
 */
void Meta_send(String mesg, size_t len);
int Meta_from(String addr, int port);
void Meta_gone();
void Meta_init();
void Meta_update(bool change);
void Meta_update_max_size_tuner();

/*
 * Prototypes for frame.c
 */
void Frame_update();
void Set_message(String message);
void Set_player_message(player_t *pl, String message);
void Set_message_f(String format, ...);
void Set_player_message_f(player_t *pl, String format, ...);

/*
 * Prototypes for update.c
 */
void Update_objects();
void Autopilot(player_t *pl, bool on);
void Cloak(player_t *pl, bool on);
void Deflector(player_t *pl, bool on);
void Emergency_thrust(player_t *pl, bool on);
void Emergency_shield(player_t *pl, bool on);
void Phasing(player_t *pl, bool on);
void Thrust(player_t *pl, bool on);

/*
 * Prototypes for parser.c
 */
int Parser_list_option(int *ind, String buf);
bool Parser(int argc, char **argv);
int Tune_option(String name, String val);
int Get_option_value(String name, String value, size_t size);

/*
 * Prototypes for fileparser.c
 */
bool parseDefaultsFile(String filename);
bool parsePasswordFile(String filename);
bool parseMapFile(String filename);
void expandKeyword(String keyword);

/*
 * Prototypes for laser.c
 */
void Fire_laser(player_t *pl);
void Fire_general_laser(int id, int team, Click  pos,
			int dir, modifiers_t mods);
void Laser_pulse_hits_player(player_t *pl, pulseobject_t *pulse);

/*
 * Prototypes for alliance.c
 */
int Invite_player(player_t *pl, player_t *ally);
int Cancel_invitation(player_t *pl);
int Refuse_alliance(player_t *pl, player_t *ally);
int Refuse_all_alliances(player_t *pl);
int Accept_alliance(player_t *pl, player_t *ally);
int Accept_all_alliances(player_t *pl);
int Get_alliance_member_count(int id);
void Player_join_alliance(player_t *pl, player_t *ally);
void Dissolve_all_alliances();
int Leave_alliance(player_t *pl);
void Alliance_player_list(player_t *pl);

/*
 * Prototypes for object.c
 */
object_t *Object_allocate();
void Object_free_ind(int ind);
void Object_free_ptr(object_t *obj);
void Alloc_shots(int number);
void Free_shots();
String Object_typename(object_t *obj);

/*
 * Prototypes for polygon.c
 */
void P_edgestyle(String id, int width, int color, int style);
void P_polystyle(String id, int color, int texture_id, int defedge_id,
		 int flags);
void P_bmpstyle(String id, String filename, int flags);
void P_start_polygon(Click  pos, int style);
void P_offset(Click  offset, int edgestyle);
void P_vertex(Click  pos, int edgestyle);
void P_style(String state, int style);
void P_end_polygon();
int P_start_ballarea();
void P_end_ballarea();
int P_start_balltarget(int team, int treasure_ind);
void P_end_balltarget();
int P_start_target(int target_ind);
void P_end_target();
int P_start_cannon(int cannon_ind);
void P_end_cannon();
int P_start_wormhole(int wormhole_ind);
void P_end_wormhole();
void P_start_decor();
void P_end_decor();
int P_start_friction_area(int fa_ind);
void P_end_friction_area();
int P_get_bmp_id(String s);
int P_get_edge_id(String s);
int P_get_poly_id(String s);
/*void P_grouphack(int type, void (*f)(int group, void *mapobj));*/
void P_set_hitmask(int group, hitmask_t hitmask);

/*
 * Prototypes for showtime.c
 */
String showtime();

/*
 * Prototypes for srecord.c
 */
void Init_recording();
void Handle_recording_buffers();
void Get_recording_data();

/*
 * Prototypes for tag.c
 */
void Transfer_tag(player_t *oldtag_pl, player_t *newtag_pl);
void Check_tag();

/*
 * Prototypes for target.c
 */
void Target_update();
void Object_hits_target(object_t *obj, target_t *targ, double player_cost);
hitmask_t Target_hitmask(target_t *targ);
void Target_set_hitmask(int group, target_t *targ);
void Target_init();
void World_restore_target(target_t *targ);
void World_remove_target(target_t *targ);

/*
 * Prototypes for treasure.c
 */
void Make_treasure_ball(treasure_t *t);
void Ball_hits_goal(ballobject_t *ball, group_t *groupptr);
void Ball_is_replaced(ballobject_t *ball);
void Ball_is_destroyed(ballobject_t *ball);
bool Balltarget_hitfunc(group_t *groupptr, const move_t *move);

/*
 * Prototypes for wormhole.c
 */
bool Initiate_hyperjump(player_t *pl);
void Player_warp(player_t *pl);
void Player_finish_warp(player_t *pl);
void Object_warp(object_t *obj);
void Object_finish_warp(object_t *obj);
void Object_hits_wormhole(object_t *obj, int ind);
hitmask_t Wormhole_hitmask(wormhole_t *wormhole);
bool Wormhole_hitfunc(group_t *groupptr, const move_t *move);
bool Verify_wormhole_consistency();

#endif
