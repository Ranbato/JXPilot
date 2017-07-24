/*
 * XPilotNG/SDL, an SDL/OpenGL XPilot client.
 *
 * Copyright (C) 2003-2004 Juha Lindström <juhal@users.sourceforge.net>
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

#ifndef IMAGES_H
#define IMAGES_H

#include "xpclient_sdl.h"

typedef enum {
    IMG_STATE_UNINITIALIZED,
    IMG_STATE_ERROR,
    IMG_STATE_READY
} image_state_e;

/*
 * This structure holds the information needed to paint an image with OpenGL.
 * One image may contain multiple frames that represent the same object in
 * different states. If rotate flag is true, the image will be rotated
 * when it is created to generate num_frames-1 of new frames.
 * Width is the cumulative width of all frames. Frame_width is the width
 * of any single frame. Because OpenGL requires the dimensions of the images
 * to be powers of 2, data_width and data_height are the nearest powers of 2
 * corresponding to width and height respectively.
 */
typedef struct {
    GLuint          name;         /* OpenGL texture "name" */
    char            *filename;    /* the name of the image file */
    int             num_frames;   /* the number of frames */
    bool            rotate;       /* should this image be rotated */
    bool            scale;        /* should this image be scaled to fill data */
    image_state_e   state;        /* the state of the image */
    int             width;        /* width of the whole image */
    int             height;       /* height of the whole image */
    int             data_width;   /* width of the image data */
    int             data_height;  /* height of the image data */
    int             frame_width;  /* width of one image frame */
    unsigned int    *data;        /* the image data */
} image_t;

public static final int IMG_HOLDER_FRIEND = 0; public static final int IMG_HOLDER_ENEMY = 1; public static final int IMG_BALL = 2; public static final int IMG_SHIP_SELF = 3; public static final int IMG_SHIP_FRIEND = 4; public static final int IMG_SHIP_ENEMY = 5; public static final int IMG_BULLET = 6; public static final int IMG_BULLET_OWN = 7; public static final int IMG_BASE_DOWN = 8; public static final int IMG_BASE_LEFT = 9; public static final int IMG_BASE_UP = 10; public static final int IMG_BASE_RIGHT = 11; public static final int IMG_FUELCELL = 12; public static final int IMG_FUEL = 13; public static final int IMG_ALL_ITEMS = 14; public static final int IMG_CANNON_DOWN = 15; public static final int IMG_CANNON_LEFT = 16; public static final int IMG_CANNON_UP = 17; public static final int IMG_CANNON_RIGHT = 18; public static final int IMG_SPARKS = 19; public static final int IMG_PAUSED = 20; public static final int IMG_REFUEL = 21; public static final int IMG_WORMHOLE = 22; public static final int IMG_MINE_TEAM = 23; public static final int IMG_MINE_OTHER = 24; public static final int IMG_CONCENTRATOR = 25; public static final int IMG_PLUSGRAVITY = 26; public static final int IMG_MINUSGRAVITY = 27; public static final int IMG_CHECKPOINT = 28; public static final int IMG_METER = 29; public static final int IMG_ASTEROIDCONC = 30; public static final int IMG_SHIELD = 31; public static final int IMG_ACWISEGRAV = 32; public static final int IMG_CWISEGRAV = 33; public static final int IMG_MISSILE = 34; public static final int IMG_ASTEROID = 35; public static final int IMG_TARGET = 36; public static final int IMG_HUD_ITEMS = 37;
int Images_init();
void Images_cleanup();
void Image_paint(int ind, int x, int y, int frame, int c);
void Image_paint_area(int ind, int x, int y, int frame, Rectangle *r, int c);
void Image_paint_rotated(int ind, int center_x, int center_y, int dir, int color);
image_t *Image_get(int ind);
image_t *Image_get_texture(int ind);
void Image_use_texture(int ind);
void Image_no_texture();

#endif
