/*
 * XPilotNG/SDL, an SDL/OpenGL XPilot client.
 *
 * Copyright (C) 2003-2004 by 
 *
 *      Juha Lindström       <juhal@users.sourceforge.net>
 *      Erik Andersson       <deity_at_home.se>
 *      Darel Cullen         <darelcullen@users.sourceforge.net>
 *
 * Copyright (C) 1991-2002 by
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

#include "xpclient_sdl.h"

#include "SDL_gfxPrimitives.h"
#include "sdlpaint.h"
#include "images.h"
#include "console.h"
#include "radar.h"
#include "sdlwindow.h"
#include "text.h"
#include "glwidgets.h"
#include "radar.h"

public static final int SCORE_BORDER = 5;
/*
 * Globals.
 */
static TTF_Font     *scoreListFont;
static String scoreListFontName = CONF_FONTDIR "VeraMoBd.ttf";
static sdl_window_t scoreListWin;
static SDL_Rect     scoreEntryRect; /* Bounds for the last painted score entry */
static boolean         scoreListMoving;

int paintSetupMode;

GLWidget *MainWidget = null;

static void Scorelist_button(Uint8 button, Uint8 state, Uint16 x, Uint16 y, void *data)
{
    GLWidget *widget = (GLWidget *)data;
    if (state == SDL_PRESSED) {
    	if (button == 1) {
	    scoreListMoving = true;
    	    if (DelGLWidgetListItem( widget.list, widget ))
	    	AppendGLWidgetList( widget.list, widget );
	}
    	if (button == 2) {
    	    if (DelGLWidgetListItem( widget.list, widget ))
	    	PrependGLWidgetList( widget.list, widget );
	}
    }
    
    if (state == SDL_RELEASED) {
    	if (button == 1)
	    scoreListMoving = false;
    }
}

static void Scorelist_move(Sint16 xrel, Sint16 yrel, Uint16 x, Uint16 y, void *data)
{
    if (scoreListMoving) {
	((GLWidget *)data).bounds.x = scoreListWin.x += xrel;
	((GLWidget *)data).bounds.y = scoreListWin.y += yrel;
    }
}


static void Scorelist_cleanup( GLWidget *widget )
{
    TTF_CloseFont(scoreListFont);
    sdl_window_destroy(&scoreListWin);
}

static void SetBounds_ScoreList(GLWidget *widget, SDL_Rect *b )
{
    widget.bounds.x = scoreListWin.x = b.x;
    widget.bounds.y = scoreListWin.y = b.y;
}

static void Scorelist_paint(GLWidget *widget)
{
    if (scoresChanged) {
	/* This is the easiest way to track if
	 * the height of the score window should be changed */
	int y = scoreEntryRect.y;
        Paint_score_table();
	if (y != scoreEntryRect.y) {
	    sdl_window_resize(&scoreListWin, scoreListWin.w,
			      scoreEntryRect.y + scoreEntryRect.h
			      + 2 * SCORE_BORDER);
	    /* Unfortunately the resize loses the surface
	     * so I have to repaint it */
	    scoresChanged = true;
	    Paint_score_table();
	    widget.bounds.w = scoreListWin.w+2;
	    widget.bounds.h = scoreListWin.h+2;
	}
	sdl_window_refresh(&scoreListWin);
    }
    glColor4ub(0, 0x20, 0, 0x90);
    glEnable(GL_BLEND);
    glBegin(GL_QUADS);
    	glVertex2i(scoreListWin.x, scoreListWin.y + scoreListWin.h + 2);    
    	glVertex2i(scoreListWin.x, scoreListWin.y);
    	glVertex2i(scoreListWin.x + scoreListWin.w, scoreListWin.y);
    	glVertex2i(scoreListWin.x + scoreListWin.w,scoreListWin.y + scoreListWin.h + 2);
    glEnd();
    sdl_window_paint(&scoreListWin);
    glBegin(GL_LINE_LOOP);
    	glColor4ub(0, 0, 0, 0xff);
    	glVertex2i(scoreListWin.x, scoreListWin.y + scoreListWin.h + 2);    
    	glColor4ub(0, 0x90, 0x00, 0xff);
    	glVertex2i(scoreListWin.x, scoreListWin.y);
    	glColor4ub(0, 0, 0, 0xff);
    	glVertex2i(scoreListWin.x + scoreListWin.w, scoreListWin.y);
    	glColor4ub(0, 0x90, 0x00, 0xff);
    	glVertex2i(scoreListWin.x + scoreListWin.w, scoreListWin.y + scoreListWin.h + 2);
    glEnd();
}

GLWidget *Init_ScorelistWidget()
{
    GLWidget *tmp	= Init_EmptyBaseGLWidget();
    if ( !tmp ) {
        logger.error("Failed to malloc in Init_ScorelistWidget");
	return null;
    }

    tmp.WIDGET     	= SCORELISTWIDGET;
    tmp.bounds.x   	= 10;
    tmp.bounds.y   	= 240;
    tmp.bounds.w   	= 200;
    tmp.bounds.h   	= 100;

    scoreListFont = TTF_OpenFont(scoreListFontName, 11);
    if (scoreListFont == null) {
	logger.error("opening font {} failed", scoreListFontName);
	free(tmp);
	return null;
    }
    if (sdl_window_init(&scoreListWin, tmp.bounds.x, tmp.bounds.y, tmp.bounds.w, tmp.bounds.h)) {
	logger.error("failed to init scorelist window");
	free(tmp);
	return null;
    }
    tmp.Draw	    	= Scorelist_paint;
    tmp.Close	    	= Scorelist_cleanup;
    tmp.button     	= Scorelist_button;
    tmp.SetBounds     	= SetBounds_ScoreList;
    tmp.buttondata 	= tmp;
    tmp.motion     	= Scorelist_move;
    tmp.motiondata 	= tmp;

    return tmp;
}

boolean Set_scaleFactor(xp_option *opt, double val)
{
    clData.scaleFactor = val;
    clData.scale = 1.0 / val;
    clData.fscale = (float)clData.scale;
    return true;
}

boolean Set_altScaleFactor(xp_option *opt, double val)
{
    clData.altScaleFactor = val;
    return true;
}

int Paint_init()
{
    if (Init_wreckage() == -1)
	return -1;
    
    if (Images_init() == -1) 
	return -1;

    scoresChanged = true;
    players_exposed = true;
    
    return 0;
}

void Paint_cleanup()
{
    int i;
    Images_cleanup();

    for (i=0;i<MAX_SCORE_OBJECTS;++i)
    	if (score_object_texs[i].tex_list) free_string_texture(&score_object_texs[i]);
    for (i=0;i<MAX_METERS;++i)
    	if (meter_texs[i].tex_list) free_string_texture(&meter_texs[i]);
}

/* This one works best for things that are fixed in position
 * since they won't appear to move relative to eachother
 */
void setupPaint_stationary()
{
    if (paintSetupMode & STATIONARY_MODE) return;
    paintSetupMode = STATIONARY_MODE;
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();
    glTranslatef(rint(-world.x * clData.scale),
		 rint(-world.y * clData.scale),
		 0);
    glScalef(clData.scale, clData.scale, 0);
}

/* This one works best for things that move, since they don't get
 * painted differently depending on map position
 */
void setupPaint_moving()
{
    if (paintSetupMode & MOVING_MODE) return;
    paintSetupMode = MOVING_MODE;
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();
    glTranslatef(-world.x * clData.scale, -world.y * clData.scale, 0);
    glScalef(clData.scale, clData.scale, 0);
}

void setupPaint_HUD()
{
    if (paintSetupMode & HUD_MODE) return;
    paintSetupMode = HUD_MODE;
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    gluOrtho2D(0, draw_width, draw_height, 0);
}

void Paint_frame()
{
    struct timeval tv1, tv2;

    gettimeofday(&tv1, null);

    Paint_frame_start();

    if (damaged <= 0) {
    	/*glClear(GL_COLOR_BUFFER_BIT);*/
	/* on my machine this seems about 10 times faster
	 * with seemingly the same result
	 */
	set_alphacolor(blackRGBA);
	glBegin(GL_QUADS);
	    glVertex2i(0,0);
	    glVertex2i(draw_width,0);
	    glVertex2i(draw_width,draw_height);
	    glVertex2i(0,draw_height);
	glEnd();

	glEnable(GL_BLEND);
    	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

	/* This one works best for things that are fixed in position
	 * since they won't appear to move relative to eachother
	 */
    	
    	glPushMatrix();
    	setupPaint_stationary();
	
    	Paint_world();

	if (oldServer) {
	    Paint_vfuel();
	    Paint_vdecor();
	    Paint_vcannon();
	    Paint_vbase();
	} else
	    Paint_objects();

    	Paint_score_objects();
	
	Paint_shots();

	setupPaint_moving();
	Paint_ships();

	setupPaint_HUD();

    	Paint_meters();
    	Paint_HUD();
    	Paint_HUD_values();

	Paint_messages();       
	Console_paint();
	Paint_select();

	if (UpdateRadar) {
	  Radar_update();
	  UpdateRadar = false;
	}
    	DrawGLWidgets(MainWidget);
    		
	glPopMatrix();
    }
    
    SDL_GL_SwapBuffers();

    if (newSecond) {
	gettimeofday(&tv2, null);
	clData.clientLag = 1e-3 * timeval_sub(&tv2, &tv1);
    }
}

void Paint_score_start()
{
    char	headingStr[MSG_LEN];
    SDL_Surface *header;
	SDL_Color fg;

    if (showUserName)
	strlcpy(headingStr, "NICK=USER@HOST", sizeof(headingStr));
    else if (Setup.mode.get( TEAM_PLAY))
	strlcpy(headingStr, "  SCORE NAME           LIFE", sizeof(headingStr));
    else {
	strlcpy(headingStr, "  ", sizeof(headingStr));
	if (Setup.mode.get( TIMING))
	    strcat(headingStr, "LAP ");
	strlcpy(headingStr, " AL ", sizeof(headingStr));
	strcat(headingStr, "  SCORE  ");
	if (Setup.mode.get( LIMITED_LIVES))
	    strlcat(headingStr, "LIFE", sizeof(headingStr));
	strlcat(headingStr, " NAME", sizeof(headingStr));
    }
	
    fg.r = (scoreColorRGBA >> 24) & 255;
	fg.g = (scoreColorRGBA >> 16) & 255;
	fg.b = (scoreColorRGBA >> 8) & 255;
	fg.unused = scoreColorRGBA & 255;
    SDL_FillRect(scoreListWin.surface, null, 0);
    header = TTF_RenderText_Blended(scoreListFont, headingStr, fg);
    if (header == null) {
	logger.error("scorelist header rendering failed: {}", SDL_GetError());
	return;
    }
    scoreEntryRect.x = scoreEntryRect.y = SCORE_BORDER;
    SDL_SetAlpha(header, 0, 0);
    SDL_BlitSurface(header, null, scoreListWin.surface, &scoreEntryRect);
    lineRGBA(scoreListWin.surface, SCORE_BORDER,
	     scoreEntryRect.y + header.h + 2,
	     scoreListWin.w - SCORE_BORDER,
	     scoreEntryRect.y + header.h + 2,
	     0, 128, 0, 255);
    SDL_FreeSurface(header);
}

void Paint_score_entry(int entry_num, Other *other, boolean is_team)
{
    static char		raceStr[8], teamStr[4], lifeStr[8], label[MSG_LEN];
    static int		lineSpacing = -1, firstLine;
    char		scoreStr[16];
    SDL_Surface         *line;
	SDL_Color fg;
    int     	    	color;

    /*
     * First time we're here, set up miscellaneous strings for
     * efficiency and calculate some other constants.
     */
    if (lineSpacing == -1) {
	memset(raceStr, 0, sizeof raceStr);
	memset(teamStr, 0, sizeof teamStr);
	memset(lifeStr, 0, sizeof lifeStr);
	teamStr[1] = ' ';
	raceStr[2] = ' ';

	lineSpacing = TTF_FontLineSkip(scoreListFont) + 1;
	/*
	 * SDL_ttf 1.2 seems to have a broken TTF_FontLineSkip.
	 * Enable workaround and print a warning.
	 */
	if (lineSpacing == 1) {
	    static boolean warned = false;
	    if (!warned) {
		logger.warn("Enabling workaround for bug in SDL_ttf 1.2.");
		logger.warn("SDL_ttf 2.0 or newer should not have this problem.");
		warned = true;
	    }
	    lineSpacing = 15;
	}
	/* End of SDL_ttf 1.2 bug workaround. */

	firstLine = 2*SCORE_BORDER + lineSpacing;
    }
    scoreEntryRect.y = firstLine + lineSpacing * entry_num;

    /*
     * Setup the status line
     */
    if (showUserName)
	sprintf(label, "{}={}@{}",
		other.nick_name, other.user_name, other.host_name);
    else {
	if (Setup.mode.get( TIMING)) {
	    raceStr[0] = ' ';
	    raceStr[1] = ' ';
	    if ((other.mychar == ' ' || other.mychar == 'R')
		&& other.round + other.check > 0) {
		if (other.round > 99)
		    sprintf(raceStr, "%3d", other.round);
		else
		    sprintf(raceStr, "{}.%c",
			    other.round, other.check + 'a');
	    }
	}
	if (Setup.mode.get( TEAM_PLAY))
	    teamStr[0] = other.team + '0';
	else
	    sprintf(teamStr, "%c", other.alliance);

	if (Setup.mode.get( LIMITED_LIVES))
	    sprintf(lifeStr, " %3d", other.life);

	if (Using_score_decimals())
	    sprintf(scoreStr, "%*.*f",
		    7 - showScoreDecimals, showScoreDecimals,
		    other.score);
	else {
	    double score = other.score;
	    int sc = (int)(score >= 0.0 ? score + 0.5 : score - 0.5);
	    sprintf(scoreStr, "%6d", sc);
	}

	if (Setup.mode.get( TEAM_PLAY))
	    sprintf(label, "%c{} %-15s{}",
		    other.mychar, scoreStr, other.nick_name, lifeStr);
	else
	    sprintf(label, "%c {}{}{}{}  {}",
		    other.mychar, raceStr, teamStr,
		    scoreStr, lifeStr,
		    other.nick_name);
    }

    /*
     * Draw the line
     * e94_msu eKthHacks
     */
    if (!is_team && strchr("DPW", other.mychar)) {
	if (other.id == self.id)
	    color = scoreInactiveSelfColorRGBA;
	else
	    color = scoreInactiveColorRGBA;
    } else {
	if (!is_team) {
	    if (other.id == self.id)
		color = scoreSelfColorRGBA;
	    else
		color = scoreColorRGBA;
	} else {
	    color = Team_color(other.team);
	    if (!color) {
		if (other.team == self.team)
		    color = scoreOwnTeamColorRGBA;
		else
		    color = scoreEnemyTeamColorRGBA;
	    }
	}
    }
    fg.r = (color >> 24) & 255;
	fg.g = (color >> 16) & 255;
	fg.b = (color >> 8) & 255;
	fg.unused = color & 255;
    line = TTF_RenderText_Blended(scoreListFont, label, fg);
    if (line == null) {
	logger.error("scorelist rendering failed: {}", SDL_GetError());
	return;
    }
    SDL_SetAlpha(line, 0, 0);
    SDL_BlitSurface(line, null, scoreListWin.surface, &scoreEntryRect);
    scoreEntryRect.h = line.h;

    /*
     * Underline the teams
     */
    if (is_team) {
	lineRGBA(scoreListWin.surface, scoreEntryRect.x, 
		 scoreEntryRect.y + line.h - 1,
		 scoreEntryRect.x + scoreEntryRect.w,
		 scoreEntryRect.y + line.h - 1,
		 fg.r, fg.g, fg.b, 255);
    }

    SDL_FreeSurface(line);
}

