/*
 * XPilotNG/SDL, an SDL/OpenGL XPilot client.
 *
 * Copyright (C) 2003-2004 Erik Andersson <deity_at_home.se>
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

#ifndef GLWIDGETS_H
#define GLWIDGETS_H

#include "xpclient_sdl.h"

#include "sdlkeys.h"
#include "text.h"

/****************************************************/
/* BEGIN: Main GLWidget stuff	    	    	    */
/****************************************************/
/* Basically the init function will set default width, height in bounds
 * Then the caller should reshape and position the widget defaults to 0,0
 * (top left atm, EVEN sub-widgets so you MUST use the SetBounds_GLWidget
 * function)
 */
/* if this structure is changed, make sure that the generic functions below still work! */
typedef struct glwidget_struct GLWidget;
struct glwidget_struct {
    int     	    WIDGET;
    void    	    *wid_info;

    SDL_Rect	    bounds; /* atm this really is 'inner bounds' which
    	    	    	     * the children aren't allowed to exceed
			     */
    void    	    (*Draw)( GLWidget *widget );
    
    void    	    (*Close)( GLWidget *widget );
    void    	    (*SetBounds)( GLWidget *widget, SDL_Rect *b );
    
    void    	    (*button)( Uint8 button, Uint8 state , Uint16 x , Uint16 y, void *data );
    void    	    *buttondata;
    void    	    (*motion)( Sint16 xrel, Sint16 yrel, Uint16 x, Uint16 y, void *data );
    void    	    *motiondata;
    void    	    (*hover)( int over, Uint16 x , Uint16 y , void *data );
    void    	    *hoverdata;

    GLWidget	    **list;
    GLWidget	    *children;
    GLWidget	    *next;   /* use to build widget lists */
};

GLWidget *Init_EmptyBaseGLWidget( void );
/*GLWidget *Init_BaseGLWidget( int WIDGET, void *wid_info, SDL_Rect bounds,
    	    	    	    void (*Draw)( GLWidget *widget ), void (*Close)( GLWidget *widget ),
			    void (*SetBounds)( GLWidget *widget, SDL_Rect *b ),
			    void (*button)( Uint8 button, Uint8 state , Uint16 x , Uint16 y, void *data ), void *buttondata,
			    void (*motion)( Sint16 xrel, Sint16 yrel, Uint16 x, Uint16 y, void *data ), void *motiondata,
			    void (*hover)( int over, Uint16 x , Uint16 y , void *data ), void *hoverdata,
			    GLWidget *children, GLWidget *next
			     );*/

extern GLWidget *MainWidget;

/* Two Methods Needed for widget management */
/* new types need to implement theese methods */
    
/* should free any resources committed by the init_foo function */
void Close_Widget ( GLWidget **widget );
/*void Close_WidgetTree ( GLWidget **widget );*/
/* to reshape the widget, and automagically reshape and place sub-widgets */
void SetBounds_GLWidget(GLWidget *wid, SDL_Rect *b );
/* Initializes the appropriate config widget (if implemented), returns null otherwise */
GLWidget *Init_OptionWidget( xp_option *opt, Uint32 *fgcolor, Uint32 *bgcolor );

boolean AppendGLWidgetList( GLWidget **list, GLWidget *widget );
void PrependGLWidgetList( GLWidget **list, GLWidget *widget );
boolean DelGLWidgetListItem( GLWidget **list, GLWidget *widget );

void DrawGLWidgets( GLWidget *list );
GLWidget *FindGLWidget( GLWidget *list, Uint16 x,Uint16 y );
void DrawGLWidgetsi( GLWidget *list, int x, int y, int w, int h);
GLWidget *FindGLWidgeti( GLWidget *widget, Uint16 x, Uint16 y );

extern GLWidget *clicktarget[NUM_MOUSE_BUTTONS];
extern GLWidget *hovertarget;

/* puts text into the copy buffer */
void load_textscrap(String text);
/****************************************************/
/* END: Main GLWidget stuff 	    	    	    */
/****************************************************/

/****************************************************/
/* widget-specific stuff is below   	    	    */
/****************************************************/

/***********************/
/* Begin:  ArrowWidget */
/***********************/
/* Basically a triangle that stays lit while mousebutton 1 is down
 * And each time it is drawn lit, it calls (*action).
 * Button two causes it set 'tap' and calls (*action),
 * (*action) needs to reset it for it to work again. (still haven't
 * decided whether automatic reset is better)
 */
public static final int ARROWWIDGET = 0; typedef enum {RIGHTARROW,UPARROW,LEFTARROW,DOWNARROW} ArrowWidget_dir_t;
typedef struct {
    ArrowWidget_dir_t	direction;
    boolean    	    	press;/*this is set/unset automagically (set:call action each draw)*/
    boolean    	    	tap;/*action needs to clear this (action called once)*/
    boolean    	    	locked;/*Won't call action for any reason*/
    void    	    	(*action)(void *data);
    void    	    	*actiondata;
} ArrowWidget;
GLWidget *Init_ArrowWidget( ArrowWidget_dir_t direction, int width, int height,
    	    	    	    void (*action)( void *data ), void *actiondata );
/*********************/
/* End:  ArrowWidget */
/*********************/

/***********************/
/* Begin: ButtonWidget  */
/***********************/
public static final int BUTTONWIDGET = 1; typedef struct {
    Uint32  	*normal_color;
    Uint32  	*pressed_color;
    boolean    	pressed;
    Uint8  	depress_time;
    int  	press_time;
    void    	(*action)(void *data);
    void    	*actiondata;
} ButtonWidget;
GLWidget *Init_ButtonWidget( Uint32 *normal_color, Uint32 *pressed_color, Uint8 depress_time, void (*action)(void *data), void *actiondata);
/*********************/
/* End: ButtonWidget  */
/*********************/

/***********************/
/* Begin: SlideWidget  */
/***********************/
public static final int SLIDEWIDGET = 2; typedef struct {
    boolean    sliding;/*Don't slide*/
    boolean    locked;/*Don't slide*/
    void    (*release)( void *releasedata );
    void    *releasedata;
} SlideWidget;
GLWidget *Init_SlideWidget( boolean locked,
    	     void (*motion)( Sint16 xrel, Sint16 yrel, Uint16 x, Uint16 y, void *data ), void *motiondata,
	     void (*release)( void *releasedata),void *releasedata );
/*********************/
/* End: SlideWidget  */
/*********************/

/***************************/
/* Begin: ScrollbarWidget  */
/***************************/
typedef enum {SB_VERTICAL, SB_HORISONTAL} ScrollWidget_dir_t;
/* note 0.0 <= pos && pos + size <= 1.0 */
public static final int SCROLLBARWIDGET = 3; typedef struct {
    GLWidget	    	*slide;
    GLfloat 	    	pos;
    GLfloat 	    	size;
    Sint16  	    	oldmoves;
    ScrollWidget_dir_t	dir;
    void    	    (	*poschange)( GLfloat pos , void *poschangedata );
    void    	    	*poschangedata;
} ScrollbarWidget;
GLWidget *Init_ScrollbarWidget( boolean locked, GLfloat pos, GLfloat size,ScrollWidget_dir_t dir,
    	    	    	    	void (*poschange)( GLfloat pos , void *data), void *data );

void ScrollbarWidget_SetSlideSize( GLWidget *widget, GLfloat size );
/*************************/
/* End:  ScrollbarWidget */
/*************************/

/**********************/
/* Begin: LabelWidget */
/**********************/
public static final int LABELWIDGET = 4; typedef struct {
    string_tex_t    tex;
    Uint32     	    *fgcolor;
    Uint32     	    *bgcolor;
    int             align;  /* horizontal alignemnt */
    int             valign; /* vertical alignment   */
} LabelWidget;
GLWidget *Init_LabelWidget( String text , Uint32 *fgcolor, Uint32 *bgcolor, int align, int valign );

boolean LabelWidget_SetColor( GLWidget *widget , Uint32 *fgcolor, Uint32 *bgcolor );

/********************/
/* End: LabelWidget */
/********************/

/***********************************/
/* Begin: LabeledRadiobuttonWidget */
/***********************************/
public static final int LABELEDRADIOBUTTONWIDGET = 5; typedef struct {
    boolean    	    state;
    string_tex_t    *ontex;
    string_tex_t    *offtex;
    void    	    (*action)( boolean state, void *actiondata );
    void    	    *actiondata;
} LabeledRadiobuttonWidget;
/* TODO : add some abstraction layer to init function */
GLWidget *Init_LabeledRadiobuttonWidget( string_tex_t *ontex, string_tex_t *offtex,
    	    	    	    	    	void (*action)(boolean state, void *actiondata),
					void *actiondata, boolean start_state);
/*********************************/
/* End: LabeledRadiobuttonWidget */
/*********************************/

/****************************/
/* Begin: BoolChooserWidget */
/****************************/
public static final int BOOLCHOOSERWIDGET = 6; typedef struct {
    boolean    	    *value;
    GLWidget	    *buttonwidget;
    GLWidget	    *name;
    Uint32     	    *fgcolor;
    Uint32     	    *bgcolor;
    void    	    (*callback)(void *tmp, String value);
    void    	    *data;
} BoolChooserWidget;

GLWidget *Init_BoolChooserWidget( String name, boolean *value, Uint32 *fgcolor, Uint32 *bgcolor,
    	    	    	    	 void (*callback)(void *tmp, String value), void *data );
/**************************/
/* End: BoolChooserWidget */
/**************************/

/***************************/
/* Begin: IntChooserWidget */
/***************************/
public static final int INTCHOOSERWIDGET = 7; typedef struct {
    GLWidget	    *name;
    int     	    *value;
    int     	    minval;
    int     	    maxval;
    int     	    valuespace;
    string_tex_t    valuetex;
    GLWidget	    *leftarrow;
    GLWidget	    *rightarrow;
    int     	    direction;
    int     	    duration;
    Uint32     	    *fgcolor;
    Uint32     	    *bgcolor;
    void    	    (*callback)(void *tmp, String value);
    void    	    *data;
} IntChooserWidget;

GLWidget *Init_IntChooserWidget( String name, int *value, int minval, int maxval, Uint32 *fgcolor,
    	    	    	    	Uint32 *bgcolor, void (*callback)(void *tmp, String value), void *data );
/*************************/
/* End: IntChooserWidget */
/*************************/

/******************************/
/* Begin: DoubleChooserWidget */
/******************************/
public static final int DOUBLECHOOSERWIDGET = 8; typedef struct {
    GLWidget	    *name;
    double  	    *value;
    double     	    minval;
    double     	    maxval;
    int     	    valuespace;
    string_tex_t    valuetex;
    GLWidget	    *leftarrow;
    GLWidget	    *rightarrow;
    int     	    direction;
    Uint32     	    *fgcolor;
    Uint32     	    *bgcolor;
    void    	    (*callback)(void *tmp, String value);
    void    	    *data;
} DoubleChooserWidget;

GLWidget *Init_DoubleChooserWidget( String name, double *value, double minval, double maxval,
    	    	    	    	    Uint32 *fgcolor, Uint32 *bgcolor,
    	    	    	    	    void (*callback)(void *tmp, String value), void *data );
/****************************/
/* End: DoubleChooserWidget */
/****************************/

/*****************************/
/* Begin: ColorChooserWidget */
/*****************************/
public static final int COLORCHOOSERWIDGET = 9; typedef struct {
    GLWidget	    *mod;
    GLWidget	    *name;
    Uint32  	    *value;
    GLWidget	    *button;
    Uint32     	    *fgcolor;
    Uint32     	    *bgcolor;
    boolean    	    expanded;
    void    	    (*callback)(void *tmp, String value);
    void    	    *data;
} ColorChooserWidget;

GLWidget *Init_ColorChooserWidget( String name, Uint32 *value, Uint32 *fgcolor, Uint32 *bgcolor,
    	    	    	    	    void (*callback)(void *tmp, String value), void *data );
/***************************/
/* End: ColorChooserWidget */
/***************************/

/*************************/
/* Begin: ColorModWidget */
/*************************/
public static final int COLORMODWIDGET = 10; typedef struct {
    Uint32  	    *value;
    int  	    red;
    int  	    green;
    int  	    blue;
    int  	    alpha;
    GLWidget	    *redpick;
    GLWidget	    *greenpick;
    GLWidget	    *bluepick;
    GLWidget	    *alphapick;
    Uint32     	    *fgcolor;
    Uint32     	    *bgcolor;
    void    	    (*callback)(void *tmp, String value);
    void    	    *data;
} ColorModWidget;

GLWidget *Init_ColorModWidget( Uint32 *value, Uint32 *fgcolor, Uint32 *bgcolor,
    	    	    	    	    void (*callback)(void *tmp, String value), void *data );
/***********************/
/* End: ColorModWidget */
/***********************/

/**********************/
/* Begin: ListWidget  */
/**********************/
typedef enum {HORISONTAL, VERTICAL} ListWidget_direction;
typedef enum {LW_DOWN, LW_VCENTER, LW_UP} ListWidget_ver_dir_t;
typedef enum {LW_RIGHT, LW_HCENTER, LW_LEFT} ListWidget_hor_dir_t;
public static final int LISTWIDGET = 11; typedef struct {
     int num_elements;
     Uint32 *bg1;
     Uint32 *bg2;
     Uint32 *highlight_color;/*not used (yet) */
     boolean   reverse_scroll;
     ListWidget_direction   direction;
     ListWidget_ver_dir_t   v_dir;
     ListWidget_hor_dir_t   h_dir;    
} ListWidget;

GLWidget *Init_ListWidget( Uint16 x, Uint16 y, Uint32 *bg1, Uint32 *bg2, Uint32 *highlight_color
    	    	    	    ,ListWidget_ver_dir_t v_dir, ListWidget_hor_dir_t h_dir
			    ,ListWidget_direction direction, boolean reverse_scroll );

/*TODO: allow lists in prepen,append (needs to check against loops) */

/* Adds a new item last in the list */
boolean ListWidget_Append( GLWidget *list, GLWidget *item );
/* Adds a new item first in the list */
boolean ListWidget_Prepend( GLWidget *list, GLWidget *item );
/* Adds a new item just before target in the list */
boolean ListWidget_Insert( GLWidget *list, GLWidget *target, GLWidget *item );
/* Removes an item from the list */
boolean ListWidget_Remove( GLWidget *list, GLWidget *item );

boolean ListWidget_SetScrollorder( GLWidget *list, boolean order );

int ListWidget_NELEM( GLWidget *list );
/* first item is indexed [0], last is [ListWidget_NELEM - 1]*/
GLWidget *ListWidget_GetItemByIndex( GLWidget *list, int i );

/*******************/
/* End: ListWidget */
/*******************/

/****************************/
/* Begin: ScrollPaneWidget  */
/****************************/
public static final int SCROLLPANEWIDGET = 12; typedef struct {
    GLWidget	*vert_scroller;
    GLWidget	*hori_scroller;
    GLWidget	*masque;
    GLWidget	*content;
} ScrollPaneWidget;

GLWidget *Init_ScrollPaneWidget( GLWidget *content );

/**************************/
/* End: ScrollPaneWidget  */
/**************************/

/**********************/
/* Begin: RadarWidget */
/**********************/
public static final int RADARWIDGET = 13;
extern GLWidget *Init_RadarWidget( void );
/********************/
/* End: RadarWidget */
/********************/

/**************************/
/* Begin: ScorelistWidget */
/**************************/
public static final int SCORELISTWIDGET = 14;
extern GLWidget *Init_ScorelistWidget( void );
/************************/
/* End: ScorelistWidget */
/************************/

/**********************/
/* Begin: MainWidget  */
/**********************/
public static final int MAINWIDGET = 15; typedef struct {
    boolean	showconf;
    GLWidget	*confmenu;
    GLWidget	*radar;
    GLWidget	*scorelist;
    GLWidget	*chat_msgs;
    GLWidget	*game_msgs;
    GLWidget	*alert_msgs;
    int	    	BORDER;
    font_data	*font;
} WrapperWidget;

GLWidget *Init_MainWidget( font_data *font );
void MainWidget_ShowMenu( GLWidget *widget, boolean show );
/*******************/
/* End: MainWidget */
/*******************/

/**************************/
/* Begin: ConfMenuWidget  */
/**************************/
public static final int CONFMENUWIDGET = 16; typedef struct {
    boolean	showconf;
    boolean	paused;
    int     	team;
    GLWidget	*scrollpane;
    GLWidget	*main_list;
    GLWidget	*button_list;
    GLWidget	*join_list;
    GLWidget	*qlb;
    GLWidget	*clb;
    GLWidget	*slb;
    GLWidget	*jlb;
} ConfMenuWidget;

GLWidget *Init_ConfMenuWidget( Uint16 x, Uint16 y );
/***********************/
/* End: ConfMenuWidget */
/***********************/

/*****************************/
/* Begin: ImageButtonWidget  */
/*****************************/
public static final int IMAGEBUTTONWIDGET = 17; typedef struct {
    Uint32 fg;
    Uint32 bg;
    Uint8 state;
    string_tex_t tex;
    GLuint imageUp;
    GLuint imageDown;
    texcoord_t txcUp;
    texcoord_t txcDown;
    void (*onClick)(GLWidget *widget);
} ImageButtonWidget;

GLWidget *Init_ImageButtonWidget(String text,
				 String upImage,
				 String downImage,
				 Uint32 bg, 
				 Uint32 fg,
				 void (*onClick)(GLWidget *widget));
/**************************/
/* End: ImageButtonWidget */
/**************************/

/*****************************/
/* Begin: LabelButtonWidget  */
/*****************************/
public static final int LABELBUTTONWIDGET = 18; typedef struct {
    GLWidget *button;
    GLWidget *label;
} LabelButtonWidget;

GLWidget *Init_LabelButtonWidget(   String text,
				    Uint32 *text_color,
    	    	    	    	    Uint32 *bg_color,
    	    	    	    	    Uint32 *active_color,
    	    	    	    	    Uint8 depress_time,
    	    	    	    	    void (*action)(void *data),
				    void *actiondata);
/**************************/
/* End: LabelButtonWidget */
/**************************/

#endif
