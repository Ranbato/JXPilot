/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 1991-2004 by
 *
 *      Bjørn Stabell        <bjoern@xpilot.org>
 *      Ken Ronny Schouten   <ken@xpilot.org>
 *      Bert Gijsbers        <bert@xpilot.org>
 *      Dick Balaska         <dick@xpilot.org>
 *      Erik Andersson       <maximan@users.sourceforge.net>
 *      Kristian Söderblom   <kps@users.sourceforge.net>
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

#include "xpclient.h"

Message	*TalkMsg[MAX_MSGS], *GameMsg[MAX_MSGS];
Message	*TalkMsg_pending[MAX_MSGS], *GameMsg_pending[MAX_MSGS];
char		*HistoryMsg[MAX_HIST_MSGS];

/* provide cut&paste and message history */
static	char		*HistoryBlock = null;
int			maxLinesInHistory;
int	maxMessages;		/* Max. number of messages to display */
int	messagesToStdout;	/* Send messages to standard output */

static bool ball_shout = false;
static bool need_cover = false;

bool roundend = false;
static int killratio_kills = 0;
static int killratio_deaths = 0;
static int killratio_totalkills = 0;
static int killratio_totaldeaths = 0;
static int ballstats_cashes = 0;
static int ballstats_replaces = 0;
static int ballstats_teamcashes = 0;
static int ballstats_lostballs = 0;
bool played_this_round = false;
static int rounds_played = 0;

static Message	*MsgBlock = null;
static Message	*MsgBlock_pending = null;

static void Delete_pending_messages();

/*
 * Little less ugly message scan hack by Samaseon (kps)
 *
 * used for:
 * - Kill/Death ratio counter, based on the original
 *   "e94_msu eKthHacks (killratio)"
 * - Mara's client ranking and base warning hacks
 */

/*
 * jpv fixed the BMS ugliness and made it work
 * the way it should.
 */

/* if you want debug messages, use the upper one */
/*#define DP(x) x*/
#define DP(x)

static String shottypes[] = {
    "a shot", null
};
static const char head_first[] = " head first";
static String crashes[] = {
    "crashed", "smashed", "smacked", "was trashed", null
};
static String obstacles[] = {
    "wall", "target", "treasure", "cannon", null
};
static String teamnames[] = {
    "2", "4", "0", "1", "3", "5", "6", "7", "8", "9", null
};

/* increase if you want to look for messages with more player names. */
public static final int MSG_MAX_NAMES = 3;
/* structure to store names found in a message */
typedef struct {
    int index;
    char nick_name[MSG_MAX_NAMES][MAX_CHARS];
} msgnames_t;

/* recursive descent parser for messages */
static bool Msg_match_fmt(String msg, String fmt, msgnames_t *mn)
{
    String fp;
    int i;
    size_t len;

    DP(printf("Msg_match_fmt - fmt = '{}'\n", fmt));
    DP(printf("Msg_match_fmt - msg = '{}'\n", msg));

    /* check that msg and fmt match to % */
    fp = strstr(fmt, "%");
    if (fp == null) {
	/* NOTE: if msg contains stuff beyond fmt, don't care */
	if (strncmp(msg, fmt, strlen(fmt)) == 0)
	    return true;
	else
	    return false;
    }
    len = (size_t) (fp - fmt);
    if (strncmp(msg, fmt, len) != 0)
	return false;
    fmt = fp + 2;
    msg += len;

    switch (*(fp + 1)) {
	String nick;
    case 'n':			/* nick */
	for (i = 0; i < num_others; i++) {
	    nick = Others[i].nick_name;
	    len = strlen(nick);
	    if ((strncmp(msg, nick, len) == 0)
		&& Msg_match_fmt(msg + len, fmt, mn)) {
		strncpy(mn.nick_name[mn.index++], nick, len + 1);
		return true;
	    }
	}
	break;
    case 's':			/* shot type */
	for (i = 0; shottypes[i] != null; i++) {
	    if (strncmp(msg, shottypes[i], strlen(shottypes[i])) == 0) {
		msg += strlen(shottypes[i]);
		return Msg_match_fmt(msg, fmt, mn);
	    }
	}
	break;
    case 'h':			/* head first or nothing */
	if (strncmp(msg, head_first, strlen(head_first)) == 0)
	    msg += strlen(head_first);
	return Msg_match_fmt(msg, fmt, mn);
    case 'o':			/* obstacle */
	for (i = 0; obstacles[i] != null; i++) {
	    if (strncmp(msg, obstacles[i], strlen(obstacles[i])) == 0) {
		msg += strlen(obstacles[i]);
		return Msg_match_fmt(msg, fmt, mn);
	    }
	}
	break;
    case 'c':			/* some sort of crash */
	for (i = 0; crashes[i] != null; i++) {
	    if (strncmp(msg, crashes[i], strlen(crashes[i])) == 0) {
		msg += strlen(crashes[i]);
		return Msg_match_fmt(msg, fmt, mn);
	    }
	}
	break;
    case 't':			/* "nick" of a team */
	for (i = 0; teamnames[i] != null; i++) {
	    if (strncmp(msg, teamnames[i], strlen(teamnames[i])) == 0) {
		strncpy(mn.nick_name[mn.index++], teamnames[i], 2);
		msg += strlen(teamnames[i]);
		return Msg_match_fmt(msg, fmt, mn);
	    }
	}
	break;
    default:
	break;
    }

    return false;
}

static bool Want_scan()
{
    int i;
    Other *other;
    int num_playing = 0;

    /* if only player on server, let's not bother */
    if (num_others < 2)
	return false;

    /* if not playing, don't bother */
    if (!self || strchr("PW", self.mychar))
	return false;

    for (i = 0; i < num_others; i++) {
	other = &Others[i];
	/* alive and dead ships and robots are considered playing */
	if (strchr(" DR", other.mychar))
	    num_playing++;
    }

    if (num_playing > 1)
	return true;
    return false;
}

/*
 * A total reset is most often done when a new match is starting.
 * If we see a total reset message we clear the statistics.
 */
static bool Msg_scan_for_total_reset(String message)
{
    static char total_reset[] = "Total reset";

    if (strstr(message, total_reset)) {
	killratio_kills = 0;
	killratio_deaths = 0;
	killratio_totalkills = 0;
	killratio_totaldeaths = 0;
	ballstats_cashes = 0;
	ballstats_replaces = 0;
	ballstats_teamcashes = 0;
	ballstats_lostballs = 0;
	played_this_round = false;
	rounds_played = 0;
	return true;
    }

    return false;
}

static bool Msg_scan_for_replace_treasure(String message)
{
    msgnames_t mn;

    if (!self || !Want_scan())
	return false;

    memset(&mn, 0, sizeof(mn));
    if (Msg_match_fmt(message,
		      " < %n (team %t) has replaced the treasure >",
		      &mn)) {
	int replacer_team = atoi(mn.nick_name[0]);
	String replacer = mn.nick_name[1];

	if (replacer_team == self.team) {
	    Bms_set_state(BmsSafe);
	    if (!strcmp(replacer, self.nick_name))
		ballstats_replaces++;
	    return true;
	}
	/*
	 * Ok, at this point we know that it was not someone in our team
	 * that replaced the treasure.
	 *
	 * If there are only 2 teams playing, and our team did not replace,
	 * it was the other team.
	 * In this case, we can clear the cover flag.
	 */
	if (num_playing_teams == 2)
	    Bms_set_state(BmsPop);
	return true;
    }

    return false;
}

static bool Msg_scan_for_ball_destruction(String message)
{
    msgnames_t mn;

    if (!self || !Want_scan())
	return false;

    memset(&mn, 0, sizeof(mn));
    if (Msg_match_fmt(message,
		      " < %n's (%t) team has destroyed team %t treasure >",
		      &mn)) {
	int destroyer_team = atoi(mn.nick_name[0]);
	int destroyed_team = atoi(mn.nick_name[1]);
	String destroyer = mn.nick_name[2];

	if (destroyer_team == self.team) {
	    ballstats_teamcashes++;
	    if (!strcmp(destroyer, self.nick_name))
		ballstats_cashes++;
	}
	if (destroyed_team == self.team)
	    ballstats_lostballs++;
	return true;
    }
    return false;
}

/* Needed by base warning hack */
static void Msg_scan_death(int id)
{
    int i;
    Other *other;

    if (version >= 0x4F12)
	return;

    other = Other_by_id(id);
    if (!other)
	return;

    /* don't do base warning for players who lost their last life */
    if (Setup.mode.get( LIMITED_LIVES)	&& other.life == 0)
	return;

    for (i = 0; i < bases.size(); i++) {
	if (bases[i].id == id) {
	    bases[i].appeartime = (long) (end_loops + 3 * clientFPS);
	    break;
	}
    }
}

static bool Msg_is_game_msg(String message)
{
    if (message[strlen(message) - 1] == ']' || strncmp(message, " <", 2) == 0)
	return false;
    return true;
}

static void Msg_scan_game_msg(String message)
{
    msgnames_t mn;
    String killer = null, *victim = null, *victim2 = null;
    bool i_am_killer = false;
    bool i_am_victim = false;
    bool i_am_victim2 = false;
    Other *other = null;

    DP(printf("MESSAGE: \"{}\"\n", message));

    /*
     * First check if it is a message indicating end of round.
     */
    if (strstr(message, "There is no Deadly Player") ||
	strstr(message, "is the Deadliest Player with") ||
	strstr(message, "are the Deadly Players with")) {

	/* Mara & jpv bmsg scan - clear flags at end of round. */
	Bms_set_state(BmsNone);

	if (played_this_round) {
	    played_this_round = false;
	    rounds_played++;
	}

	roundend = true;
	killratio_totalkills += killratio_kills;
	killratio_totaldeaths += killratio_deaths;
	return;
    }

    if (!self) {
	logger.warn("Variable 'self' is null!");
	return;
    }

    /*
     * Now let's check if someone got killed.
     */
    memset(&mn, 0, sizeof(mn));
    /*
     * note: matched names will be in reverse order in the message names
     * struct, because the deepest recursion level knows first if the
     * parsing succeeded.
     */

    if (Msg_match_fmt(message, "%n was killed by {} from %n.", &mn)) {
	DP(printf("shot:\n"));
	killer = mn.nick_name[0];
	victim = mn.nick_name[1];

    } else if (Msg_match_fmt(message, "%n %c%h against a %o.", &mn)) {
	DP(printf("crashed into obstacle:\n"));
	victim = mn.nick_name[0];

    } else if (Msg_match_fmt(message, "%n and %n crashed.", &mn)) {
	DP(printf("crash:\n"));
	victim = mn.nick_name[1];
	victim2 = mn.nick_name[0];

    } else if (Msg_match_fmt(message, "%n ran over %n.", &mn)) {
	DP(printf("overrun:\n"));
	killer = mn.nick_name[1];
	victim = mn.nick_name[0];

    } else if (Msg_match_fmt
	       (message, "%n %c%h against a %o with help from %n", &mn)) {
	DP(printf("crashed into obstacle:\n"));
	/*
	 * please fix this if you like, all helpers should get a kill
	 * (look at server/walls.c)
	 */
	killer = mn.nick_name[0];
	victim = mn.nick_name[1];

    } else if (Msg_match_fmt(message, "%n has committed suicide.", &mn)) {
	DP(printf("suicide:\n"));
	victim = mn.nick_name[0];

    } else if (Msg_match_fmt(message, "%n was killed by a ball.", &mn)) {
	DP(printf("killed by ball:\n"));
	victim = mn.nick_name[0];

    } else if (Msg_match_fmt
	       (message, "%n was killed by a ball owned by %n.", &mn)) {
	DP(printf("killed by ball:\n"));
	killer = mn.nick_name[0];
	victim = mn.nick_name[1];

    } else
	if (Msg_match_fmt(message, "%n succumbed to an explosion.", &mn)) {
	DP(printf("killed by explosion:\n"));
	victim = mn.nick_name[0];

    } else
	if (Msg_match_fmt
	    (message, "%n succumbed to an explosion from %n.", &mn)) {
	DP(printf("killed by explosion:\n"));
	killer = mn.nick_name[0];
	victim = mn.nick_name[1];

    } else if (Msg_match_fmt
	       (message, "%n got roasted alive by %n's laser.", &mn)) {
	DP(printf("roasted alive:\n"));
	killer = mn.nick_name[0];
	victim = mn.nick_name[1];

    } else if (Msg_match_fmt
	       (message, "%n was hit by cannonfire.", &mn)) {
	DP(printf("hit by cannonfire:\n"));
	victim = mn.nick_name[0];

    } else
	/* none of the above, nothing to do */
	return;


    if (killer != null) {
	DP(printf("Killer is {}.\n", killer));
	if (strcmp(killer, self.nick_name) == 0)
	    i_am_killer = true;
    }

    if (victim != null) {
	DP(printf("Victim is {}.\n", victim));
	if (strcmp(victim, self.nick_name) == 0)
	    i_am_victim = true;
    }

    if (victim2 != null) {
	DP(printf("Second victim is {}.\n", victim2));
	if (strcmp(victim2, self.nick_name) == 0)
	    i_am_victim2 = true;
    }

    /* handle death array */
    if (victim != null) {
	other = Other_by_name(victim, false);

	/*for safety... could possibly happen with
	  loss or parser bugs =) */
	if (other != null)
	    Msg_scan_death(other.id);
    } else {
	DP(printf("*** [{}] was not found in the players array! ***\n",
		  victim));
    }

    if (victim2 != null) {
	other = Other_by_name(victim, false);
	if (other != null)
	    Msg_scan_death(other.id);
    } else {
	DP(printf("*** [{}] was not found in the players array! ***\n",
		  victim));
    }

    /* handle killratio */
    if (i_am_killer && !i_am_victim) {
	killratio_kills++;
	if ((killratio_kills % 10) == 0) {
	    static char		mybuf[MSG_LEN];

	    if (killratio_deaths > 0)
		sprintf(mybuf, "Current kill ratio: {}/{} (%.03f).",
			killratio_kills, killratio_deaths, 
			(double)killratio_kills / killratio_deaths);

	    Add_message(mybuf);
	}
    }


    if (i_am_victim || i_am_victim2)
	killratio_deaths++;

    if (instruments.clientRanker) {
	/*static char tauntstr[MAX_CHARS];
	  int kills, deaths; */

	/* handle case where there is a victim and a killer */
	if (killer != null && victim != null) {
	    if (i_am_killer && !i_am_victim) {
		Add_rank_Death(victim);
		/*if (instruments.get( TAUNT)) {
		  kills = Get_kills(victim);
		  deaths = Get_deaths(victim);
		  if (deaths > kills) {
		  sprintf(tauntstr, "{}: %i-%i HEHEHEHE\0",
		  victim, deaths, kills);
		  Net_talk(tauntstr);
		  }
		  } */
	    }
	    if (!i_am_killer && i_am_victim)
		Add_rank_Kill(killer);
	}
    }
}


/*
 * Checks if the message is in angle brackets, that is,
 * starts with " < " and ends with ">"
 */
static bool Msg_is_in_angle_brackets(String message)
{
    if (strncmp(message, " < ", 3))
	return false;
    if (message[strlen(message) - 1] != '>')
	return false;
    return true;
}

static void Msg_scan_angle_bracketed_msg(String message)
{
    /* let's scan for total reset even if not playing */
    if (Msg_scan_for_total_reset(message))
	return;
    if (Msg_scan_for_ball_destruction(message))
	return;
    if (Msg_scan_for_replace_treasure(message))
	return;
}

/* Mara's ball message scan */
static MsgBms Msg_do_bms(String message)
{
    static char ball_text1[] = "BALL";
    static char ball_text2[] = "Ball";
    static char ball_text3[] = "VAKK";
    static char ball_text4[] = "B A L L";
    static char ball_text5[] = "ball";
    static char safe_text1[] = "SAFE";
    static char safe_text2[] = "Safe";
    static char safe_text3[] = "safe";
    static char safe_text4[] = "S A F E";
    static char cover_text1[] = "COVER";
    static char cover_text2[] = "Cover";
    static char cover_text3[] = "cover";
    static char cover_text4[] = "INCOMING";
    static char cover_text5[] = "Incoming";
    static char cover_text6[] = "incoming";
    static char pop_text1[] = "POP";
    static char pop_text2[] = "Pop";
    static char pop_text3[] = "pop";

    /*check safe b4 ball */
    if (strstr(message, safe_text1) ||
	strstr(message, safe_text2) ||
	strstr(message, safe_text3) ||
	strstr(message, safe_text4)) {
	return BmsSafe;
    }

    if (strstr(message, cover_text1) ||
	strstr(message, cover_text2) ||
	strstr(message, cover_text3) ||
	strstr(message, cover_text4) ||
	strstr(message, cover_text5) ||
	strstr(message, cover_text6)) {
	return BmsCover;
    }

    if (strstr(message, pop_text1) ||
	strstr(message, pop_text2) ||
	strstr(message, pop_text3)) {
	return BmsPop;
    }

    if (strstr(message, ball_text1) ||
	strstr(message, ball_text2) ||
	strstr(message, ball_text3) ||
	strstr(message, ball_text4) ||
	strstr(message, ball_text5)) {
	return BmsBall;
    }

    return BmsNone;
}


/*
 * Checks if the message is sent by someone in your team.
 * In 'bracket' we will store info about where the
 * player name starts so the bms does can ignore that.
 */
static bool Msg_is_from_our_team(String message, String *msg2)
{
    Other *other;
    static char buf[MAX_CHARS + 8];
    size_t bufstrlen, len;
    int i;

    if (self == null)
	return false;

    len = strlen(message);
    for (i = 0; i < num_others; i++) {
	other = &Others[i];
	if (other.team != self.team)
	    continue;

	/* first check if someone in your team sent the message for all */
	sprintf(buf, "[{}]", other.nick_name);
	bufstrlen = strlen(buf);
	if (len < bufstrlen)
	    continue;
	if (!strcmp(&message[len - bufstrlen], buf)) {
	    *msg2 = buf;
	    strlcpy(buf, message, len - bufstrlen);
	    return true;
	}

	/* if not, check if it was sent to your team only */
	sprintf(buf, "[{}]:[{}]", other.nick_name, other.team);
	bufstrlen = strlen(buf);
	if (len < bufstrlen)
	    continue;
	if (!strcmp(&message[len - bufstrlen], buf)) {
	    *msg2 = buf;
	    strlcpy(buf, message, len - bufstrlen);
	    return true;
	}
    }
    return false;
}

int Alloc_msgs()
{
    Message *x, *x2 = null;
    int i;

    x = XMALLOC(Message, 2 * MAX_MSGS);
    if (x == null) {
	error("No memory for messages");
	return -1;
    }

    x2 = XMALLOC(Message, 2 * MAX_MSGS);
    if (x2 == null) {
	error("No memory for history messages");
	free(x);
	return -1;
    }

    MsgBlock_pending = x2;
    MsgBlock = x;

    for (i = 0; i < 2 * MAX_MSGS; i++) {
	if (i < MAX_MSGS) {
	    TalkMsg[i] = x;
	    TalkMsg_pending[i] = x2;
	} else {
	    GameMsg[i - MAX_MSGS] = x;
	    GameMsg_pending[i - MAX_MSGS] = x2;
	}
	x.txt[0] = '\0';
	x.len = 0;
	x.lifeTime = 0.0;
	x++;

	x2.txt[0] = '\0';
	x2.len = 0;
	x2.lifeTime = 0.0;
	x2++;
    }
    return 0;
}

void Free_msgs()
{
    XFREE(MsgBlock);
    XFREE(MsgBlock_pending);
}

int Alloc_history()
{
    char	*hist_ptr;
    int		i;

    /* maxLinesInHistory is a runtime constant */
    hist_ptr = XMALLOC(char, (size_t)maxLinesInHistory * MAX_CHARS);
    if (hist_ptr == null) {
	error("No memory for history");
	return -1;
    }
    HistoryBlock = hist_ptr;

    for (i = 0; i < maxLinesInHistory; i++) {
	HistoryMsg[i] = hist_ptr;
	hist_ptr[0] = '\0';
	hist_ptr += MAX_CHARS;
    }
    return 0;
}

void Free_selectionAndHistory()
{
    XFREE(HistoryBlock);
    XFREE(selection.txt);
}

/*
 * Clear bms info for all messages of the specified type.
 */
static void Bms_clear(MsgBms type)
{
    int i;

    for (i = 0; i < maxMessages && TalkMsg[i].len > 0; i++)
	if (TalkMsg[i].bmsinfo == type)
	    TalkMsg[i].bmsinfo = BmsNone;
}

bool Bms_test_state(MsgBms bms)
{
    switch (bms) {
    case BmsBall:
	return ball_shout;
    case BmsCover:
	return need_cover;
    case BmsSafe:
	return !ball_shout;
    case BmsPop:
	return !need_cover;
    default:
	dumpcore("Bms_test_state(): invalid message type");
	return 0;
    }
}

void Bms_set_state(MsgBms bms)
{
    switch (bms) {
    case BmsBall:
	ball_shout = true;
	Bms_clear(BmsSafe);
	break;
    case BmsSafe:
	ball_shout = false;
	Bms_clear(BmsBall);
	break;
    case BmsCover:
	need_cover = true;
	Bms_clear(BmsPop);
	break;
    case BmsPop:
	need_cover = false;
	Bms_clear(BmsCover);
	break;
    case BmsNone:
	ball_shout = false;
	need_cover = false;
	Bms_clear(BmsBall);
	Bms_clear(BmsSafe);
	Bms_clear(BmsCover);
	Bms_clear(BmsPop);
	break;
    default:
	dumpcore("Bms_set_state(): invalid message type");
    }
}

/*
 * add an incoming talk/game message.
 * however, buffer new messages if there is a pending selection.
 * Add_pending_messages() will be called later in Talk_cut_from_messages().
 */
void Add_message(String message)
{
    int i, last_msg_index;
    Message *msg, **msg_set;
    MsgBms bmsinfo = BmsNone;
    String msg2;
    bool is_game_msg = false;
    bool is_drawn_talk_message	= false; /* not pending */
    bool scrolling = false; /* really moving */

    is_game_msg = Msg_is_game_msg(message);
    if (!is_game_msg) {
	if (selection.draw.state == SEL_PENDING) {
	    /* the buffer for the pending messages */
	    msg_set = TalkMsg_pending;
	} else {
	    msg_set = TalkMsg;
	    is_drawn_talk_message = true;
	}
    } else {
	if (selection.draw.state == SEL_PENDING)
	    msg_set = GameMsg_pending;
	else
	    msg_set = GameMsg;
    }

    if (is_game_msg)
	Msg_scan_game_msg(message);

    else if (Msg_is_in_angle_brackets(message))
	Msg_scan_angle_bracketed_msg(message);

    else if (!is_game_msg
	     && Setup.mode.get( TEAM_PLAY)
	     && Msg_is_from_our_team(message, &msg2))
	bmsinfo = Msg_do_bms(msg2);

    if (is_drawn_talk_message) {
	/* how many talk messages */
        last_msg_index = 0;
        while (last_msg_index < maxMessages
		&& TalkMsg[last_msg_index].len != 0)
            last_msg_index++;
        last_msg_index--; /* make it an index */

	/*
	 * keep the emphasizing ('jumping' from talk window to talk messages)
	 */
	if (selection.keep_emphasizing) {
	    selection.keep_emphasizing = false;
	    selection.talk.state = SEL_NONE;
	    selection.draw.state = SEL_EMPHASIZED;
	    selection.draw.y1 = -1;
	    selection.draw.y2 = -1;
	} /* talk window emphasized */
    } /* talk messages */

    msg = msg_set[maxMessages - 1];
    for (i = maxMessages - 1; i > 0; i--)
	msg_set[i] = msg_set[i - 1];

    msg_set[0] = msg;
    msg.lifeTime = MSG_LIFE_TIME;
    strlcpy(msg.txt, message, MSG_LEN);
    msg.len = strlen(message);
    msg.bmsinfo = bmsinfo;

    /* Clear bms flags for out of date messages. */
    if (bmsinfo != BmsNone)
	Bms_set_state(bmsinfo);

    /*
     * scroll also the emphasizing
     */
    if (is_drawn_talk_message
	&& selection.draw.state == SEL_EMPHASIZED ) {

	if ((scrolling && selection.draw.y2 == 0)
	    || (selection.draw.y1 == maxMessages - 1)) {
	    /*
	     * the emphasizing vanishes, as it's 'last' line
	     * is 'scrolled away'
	     */
	    selection.draw.state = SEL_SELECTED;
	} else {
	    if (scrolling) {
		selection.draw.y2--;
		if (selection.draw.y1 == 0)
		    selection.draw.x1 = 0;
		else
		    selection.draw.y1--;
	    } else {
		selection.draw.y1++;
		if (selection.draw.y2 == maxMessages - 1)
		    selection.draw.x2 = msg_set[selection.draw.y2].len - 1;
		else
		    selection.draw.y2++;
	    }
	}
    }

    /* Print messages to standard output.
     */
    if (messagesToStdout == 2 ||
	(messagesToStdout == 1 &&
	 message[0] &&
	 message[strlen(message)-1] == ']'))
	xpprintf("{}\n", message);
}

void Add_newbie_message(String message)
{
    char msg[MSG_LEN];

    if (!newbie)
	return;

    snprintf(msg, sizeof(msg), "{} [*Newbie help*]", message);

    Add_alert_message(msg,10.0);
}

/*
 * clear the buffer for the pending messages
 */
static void Delete_pending_messages()
{
    Message *msg;
    int i;

    for (i = 0; i < maxMessages; i++) {
	msg = TalkMsg_pending[i];
	if (msg.len > 0) {
            msg.txt[0] = '\0';
            msg.len = 0;
	}
	msg = GameMsg_pending[i];
	if (msg.len > 0) {
            msg.txt[0] = '\0';
            msg.len = 0;
	}
    }
}


/*
 * after a pending cut has been completed,
 * add the (buffered) messages which were coming in meanwhile.
 */
void Add_pending_messages()
{
    int i;

    /* just through all messages */
    for (i = maxMessages-1; i >= 0; i--) {
	if (TalkMsg_pending[i].len > 0)
	    Add_message(TalkMsg_pending[i].txt);
	if (GameMsg_pending[i].len > 0)
	    Add_message(GameMsg_pending[i].txt);
    }
    Delete_pending_messages();
}


static void Roundend()
{
    int i;

    roundend = false;
    Bms_set_state(BmsNone);
}


void Add_roundend_messages(Other **order)
{
    static char hackbuf[MSG_LEN];
    static char hackbuf2[MSG_LEN];
    static char kdratio[16];
    static char killsperround[16];
    String s;
    int i;
    Other *other;

    Roundend();

    if (killratio_totalkills == 0)
	sprintf(kdratio, "0");
    else if (killratio_totaldeaths == 0)
	sprintf(kdratio, "infinite");
    else
	sprintf(kdratio, "%.2f",
		(double)killratio_totalkills / killratio_totaldeaths);

    if (rounds_played == 0)
	sprintf(killsperround, "0");
    else
	sprintf(killsperround, "%.2f",
		(double)killratio_totalkills / rounds_played);

    sprintf(hackbuf, "Kill ratio - Round: {}/{} Total: {}/{} ({}) "
	    "Rounds played: {}  Avg.kills/round: {}",
	    killratio_kills, killratio_deaths,
	    killratio_totalkills, killratio_totaldeaths, kdratio,
	    rounds_played, killsperround);

    killratio_kills = 0;
    killratio_deaths = 0;
    Add_message(hackbuf);

    sprintf(hackbuf, "Ballstats - Cash/Repl/Team/Lost: {}/{}/{}/{}",
	    ballstats_cashes, ballstats_replaces,
	    ballstats_teamcashes, ballstats_lostballs);
    Add_message(hackbuf);

    s = hackbuf;
    s += sprintf(s, "Points - ");
    /*
     * Scores are nice to see e.g. in cup recordings.
     */
    for (i = 0; i < num_others; i++) {
	other = order[i];
	if (other.mychar == 'P')
	    continue;

	if (Using_score_decimals()) {
	    sprintf(hackbuf2, "{}: %.*f ", other.nick_name,
		    showScoreDecimals, other.score);
	    if ((s - hackbuf) + strlen(hackbuf2) > MSG_LEN) {
		Add_message(hackbuf);
		s = hackbuf;
	    }
	    s += sprintf(s, "{}", hackbuf2);
	} else {
	    double score = other.score;
	    int sc = (int)(score >= 0.0 ? score + 0.5 : score - 0.5);
	    sprintf(hackbuf2, "{}: {} ", other.nick_name, sc);
	    if ((s - hackbuf) + strlen(hackbuf2) > MSG_LEN) {
		Add_message(hackbuf);
		s = hackbuf;
	    }
	    s += sprintf(s,"{}",hackbuf2);
	}
    }
    Add_message(hackbuf);
}

/*
 * Print all available messages to stdout.
 */
void Print_messages_to_stdout()
{
    int i;

    xpprintf("[talk messages]\n");
    for (i = 0; i < maxMessages; i++) {
	if (TalkMsg[i] && TalkMsg[i].len > 0)
	    xpprintf("  {}\n", TalkMsg[i].txt);
    }

    xpprintf("[server messages]\n");
    for (i = maxMessages - 1; i >= 0; i--) {
	if (GameMsg[i] && GameMsg[i].len > 0)
	    xpprintf("  {}\n", GameMsg[i].txt);
    }
    xpprintf("\n");
}
