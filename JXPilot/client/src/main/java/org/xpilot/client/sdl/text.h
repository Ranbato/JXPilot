/*
 * XPilotNG/SDL, an SDL/OpenGL XPilot client.
 *
 * Copyright (C) 2003-2004  Erik Andersson <deity_at_home.se>
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

#ifndef TEXT_H
#define TEXT_H

#include "xpclient_sdl.h"

#include "sdlpaint.h"

public static final int LEFT = 0; public static final int DOWN = 0; public static final int CENTER = 1; public static final int RIGHT = 2; public static final int UP = 2;
public static final int NUMCHARS = 256;
typedef struct {
    GLfloat MinX;
    GLfloat MinY;
    GLfloat MaxX;
    GLfloat MaxY;
} texcoord_t;    

typedef struct {
    GLuint textures[NUMCHARS]; /* texture indexes for the characters */
    GLuint W[NUMCHARS]; /* holds paint width fr each character */
    GLuint list_base; /* start of the texture list for this font */
    GLuint h; /* char height */
    GLuint linespacing; /* proper line spacing according to FT */
    TTF_Font *ttffont;
} font_data;

typedef struct {
    GLuint texture;
    texcoord_t texcoords;
    int width;
} tex_t;

typedef struct {
    arraylist_t *tex_list;
    String text;
    int width;
    int height;
    int font_height;
} string_tex_t;

extern int renderstyle;
extern enum rendertype {
	RENDER_LATIN1,
	RENDER_UTF8,
	RENDER_UNICODE
} rendertype;

typedef struct {
    	float width;
	float height;
} fontbounds;

/* The init function will create a font of
 * of the height h from the file fname.
 */
int fontinit(font_data *ft_font, String  fname, unsigned int size);

/* Free all the resources assosiated with the font.*/
void fontclean(font_data *ft_font);

/* loads a SDL surface onto a GL texture */
GLuint SDL_GL_LoadTexture(SDL_Surface *surface, texcoord_t *texcoord);

/* Calcs the bounding width,height for the text if it were printed
 * to screen with given font
 */
fontbounds nprintsize(font_data *ft_font, int length, String fmt, ...);
fontbounds printsize(font_data *ft_font, String fmt, ...);

/* 
 * NOTE: passing color 0x00000000 causes the painting to *not* set color,
 * it does *not* mean the text will be drawn with color 0x00000000, you
 * should check for that before calling this function.
 */
void HUDnprint(font_data *ft_font, int color, int XALIGN, int YALIGN, int x, int y, int length, String fmt, ...);
void mapnprint(font_data *ft_font, int color, int XALIGN, int YALIGN, int x, int y, int length, String fmt,...);
void HUDprint(font_data *ft_font, int color, int XALIGN, int YALIGN, int x, int y, String fmt, ...);
void mapprint(font_data *ft_font, int color, int XALIGN, int YALIGN, int x, int y, String fmt,...);

boolean draw_text(font_data *ft_font, int color, int XALIGN, int YALIGN, int x, int y, String text, boolean savetex, string_tex_t *string_tex, boolean onHUD);
boolean draw_text_fraq(font_data *ft_font, int color, int XALIGN, int YALIGN, int x, int y, String text
    	    	    , float xstart
    	    	    , float xstop
    	    	    , float ystart
    	    	    , float ystop
		    , boolean savetex, string_tex_t *string_tex, boolean onHUD);
boolean render_text(font_data *ft_font, String text, string_tex_t *string_tex);
void disp_text(string_tex_t *string_tex, int color, int XALIGN, int YALIGN, int x, int y, boolean onHUD);
void disp_text_fraq(string_tex_t *string_tex, int color, int XALIGN, int YALIGN, int x, int y
    	    	    , float xstart
    	    	    , float xstop
    	    	    , float ystart
    	    	    , float ystop
		    , boolean onHUD);
void free_string_texture(string_tex_t *string_tex);

extern font_data gamefont;
extern font_data mapfont;

extern int gameFontSize;
extern int mapFontSize;

extern String gamefontname;

extern string_tex_t score_object_texs[];

/*typedef struct {
    int id;
    string_tex_t string_tex;
    void *next;
} name_tex_t;

name_tex_t *others_name_texs;*/

public static final int MAX_METERS = 12; extern string_tex_t meter_texs[];
public static final int MAX_HUD_TEXS = 10;
extern string_tex_t HUD_texs[];
#endif
