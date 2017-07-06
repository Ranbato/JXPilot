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

#include "xpclient_sdl.h"

#include "console.h"
#include "SDL_console.h"
#include "sdlwindow.h"

static sdl_window_t console_window;
static ConsoleInformation *console;

void command_handler(ConsoleInformation *, String );

static void Console_refresh()
{
    SDL_FillRect(console_window.surface, null, 0);
    CON_UpdateConsole(console);
    CON_DrawConsole(console);
    sdl_window_refresh(&console_window);
}

int Console_init()
{
    SDL_Rect cr;
    cr.w = 500;
    cr.h = 100;
    if (cr.w > draw_width) cr.w = draw_width;
    if (cr.h > draw_height) cr.h = draw_height;
    cr.x = (draw_width - cr.w) / 2;
    cr.y = (draw_height - cr.h) / 2;

    if (sdl_window_init(&console_window, cr.x, cr.y, cr.w, cr.h)) {
	error("failed to init console window");
	return -1;
    }
    
    console = CON_Init(CONF_FONTDIR "ConsoleFont.bmp", console_window.surface, 100, cr);
    if (console == null) {
	error("failed to init SDL_console");
	sdl_window_destroy(&console_window);
	return -1;
    }
    CON_SetExecuteFunction(console, command_handler);
    CON_Topmost(console);
    CON_Alpha(console, 200);
    CON_SetPrompt(console, "xp> ");
    Console_print("XPilot console ready");
    /*Console_print("Type /help to get help on commands.");
    Console_show();*/
    return 0;
}

void Console_paint()
{
    if (!Console_isVisible()) return;
    if (console.Visible != CON_OPEN) Console_refresh();
    console_window.x = (draw_width - console_window.w) / 2;
    console_window.y = (draw_height - console_window.h) / 2;
    sdl_window_paint(&console_window);
    glBegin(GL_LINE_LOOP);
    glColor4ub(0, 0, 0, 0xff);
    glVertex2i(console_window.x, console_window.y + console_window.h + 2);    
    glColor4ub(0, 0x90, 0x00, 0xff);
    glVertex2i(console_window.x, console_window.y);
    glColor4ub(0, 0, 0, 0xff);
    glVertex2i(console_window.x + console_window.w, console_window.y);
    glColor4ub(0, 0x90, 0x00, 0xff);
    glVertex2i(console_window.x + console_window.w, 
	       console_window.y + console_window.h + 2);
    glEnd();
}

void Console_show()
{
    CON_Show(console);
    Console_refresh();
}

void Console_hide()
{
    CON_Hide(console);
}

int Console_isVisible()
{
    return (console.Visible != CON_CLOSED);
}

int Console_process(SDL_Event *e)
{
    if (CON_Events(e) == null) {
	Console_refresh();
	return 1;
    }
    return 0;
}

void Paste_String_to_Console(String text)
{
    Add_String_to_Console(text);
    Console_refresh();
}

void Console_cleanup()
{
    CON_Destroy(console);
    sdl_window_destroy(&console_window);
}

void Console_print(String str, ...)
{
    va_list marker;
    va_start(marker, str);
    CON_Out(console, str, marker);
    va_end(marker);
    Console_refresh();
}

void command_handler(ConsoleInformation *con, String command)
{
    Net_talk(command);
    Talk_set_state(false);
}
