/* 
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 2003 Kristian Söderblom <kps@users.sourceforge.net>
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

import java.util.BitSet;

public class Option {

public enum xp_option_type_t{
    xp_noarg_option,
    xp_bool_option,
    xp_int_option,
    xp_double_option,
    xp_string_option,
    xp_key_option
}

public enum xp_option_origin_t {
    xp_option_origin_default,	/* originates in code */
    xp_option_origin_cmdline,	/* command line */
    xp_option_origin_env,	/* environment variable */
    xp_option_origin_xpilotrc,	/* xpilotrc file */
    xp_option_origin_config,	/* config menu or such */
    xp_option_origin_setcmd	/* set client command */
}

//
///* flag bits */
///* option shows up in default menu in X client */
//#define XP_OPTFLAG_CONFIG_DEFAULT		(1 << 1)
///* option shows up in colors menu in X client */
//#define XP_OPTFLAG_CONFIG_COLORS		(1 << 2)
///* option is not saved in xpilotrc if it isn't there already */
//#define XP_OPTFLAG_KEEP				(1 << 3)
///* option value from xpilotrc won't be accepted */
//#define XP_OPTFLAG_NEVER_SAVE			(1 << 4)
///* default flags, nothing here yet */
//#define XP_OPTFLAG_DEFAULT         		(0)
//
//
///*
// * NOTE: DON'T ACCESS THIS STRUCTURE DIRECTLY, USE THE INITIALIZER MACROS,
// * AND OTHER ACCESS FUNCTIONS.
// */
//public class xp_option {
//    xp_option_type_t type;
//
//    String name;
//
//    BitSet flags;
//    xp_option_origin_t origin;
//
//    String help;
//    byte [] private_data;		/* currently only used for string options */
//
//    /* noarg option stuff */
//
//#define XP_NOARG_OPTION_DUMMY \
//	null
//
//    bool *noarg_ptr;
//
//    /* bool option stuff */
//
//#define XP_BOOL_OPTION_DUMMY \
//	false, null, null
//
//    bool bool_defval;
//    bool *bool_ptr;
//    xp_bool_option_setfunc_t bool_setfunc;
//
//    /* integer option stuff */
//
//#define XP_INT_OPTION_DUMMY \
//	0, 0, 0, null, null
//
//    int int_defval;
//    int int_minval;
//    int int_maxval;
//    int *int_ptr;
//    xp_int_option_setfunc_t int_setfunc;
//
//    /* double option stuff */
//
//#define XP_DOUBLE_OPTION_DUMMY \
//	0, 0, 0, null, null
//
//    double dbl_defval;
//    double dbl_minval;
//    double dbl_maxval;
//    double *dbl_ptr;
//    xp_double_option_setfunc_t dbl_setfunc;
//
//    /* string option stuff */
//
//#define XP_STRING_OPTION_DUMMY \
//	null, null, 0, null, null
//
//    String str_defval;
//    String str_ptr;
//    size_t str_size;
//    xp_string_option_setfunc_t str_setfunc;
//    xp_string_option_getfunc_t str_getfunc;
//
//    /* key option stuff */
//
//#define XP_KEY_OPTION_DUMMY \
//	null, null, KEY_DUMMY
//
//    String key_defval;
//    String key_string;
//    keys_t key;
//
//    /* ... */
//
//};
//
//void Store_option(xp_option *);
//
//#define STORE_OPTIONS(option_array) \
//{ \
//    int ii; \
//    for (ii = 0; ii < NELEM(option_array); ii++) \
//	Store_option(& (option_array) [ii]); \
//} \
//
//static inline String Option_get_name(xp_option *opt)
//{
//    assert(opt);
//    return opt.name;
//}
//
//static inline xp_option_type_t Option_get_type(xp_option *opt)
//{
//    assert(opt);
//    return opt.type;
//}
//
//static inline xp_option_flags_t Option_get_flags(xp_option *opt)
//{
//    assert(opt);
//    return opt.flags;
//}
//
//static inline String Option_get_help(xp_option *opt)
//{
//    assert(opt);
//    return opt.help;
//}
//
//static inline xp_option_origin_t Option_get_origin(xp_option *opt)
//{
//    assert(opt);
//    return opt.origin;
//}
//
//static inline keys_t Option_get_key(xp_option *opt)
//{
//    assert(opt);
//    return opt.key;
//}
//
//static inline void *Option_get_private_data(xp_option *opt)
//{
//    assert(opt);
//    return opt.private_data;
//}
//
//static inline xp_option *Option_by_index(int ind)
//{
//    if (ind < 0 || ind >= num_options)
//	return null;
//    return &options[ind];
//}
//
///*
// * Macros for initalizing options.
// */
//
//#define XP_NOARG_OPTION(name, valptr, flags, help) \
//{ \
//    xp_noarg_option,\
//	name,\
//	flags, xp_option_origin_default,\
//	help,\
//	null,\
//	valptr,\
//	XP_BOOL_OPTION_DUMMY,\
//	XP_INT_OPTION_DUMMY,\
//	XP_DOUBLE_OPTION_DUMMY,\
//	XP_STRING_OPTION_DUMMY,\
//	XP_KEY_OPTION_DUMMY,\
//}
//
//#define XP_BOOL_OPTION(name, defval, valptr, setfunc, flags, help) \
//{ \
//    xp_bool_option,\
//	name,\
//	flags, xp_option_origin_default,\
//	help,\
//	null,\
//	XP_NOARG_OPTION_DUMMY,\
//	defval,\
//	valptr,\
//	setfunc,\
//	XP_INT_OPTION_DUMMY,\
//	XP_DOUBLE_OPTION_DUMMY,\
//	XP_STRING_OPTION_DUMMY,\
//	XP_KEY_OPTION_DUMMY,\
//}
//
//#define XP_INT_OPTION(name, defval, minval, maxval, valptr, setfunc, flags, help) \
//{ \
//    xp_int_option,\
//	name,\
//	flags, xp_option_origin_default,\
//	help,\
//	null,\
//	XP_NOARG_OPTION_DUMMY,\
//	XP_BOOL_OPTION_DUMMY,\
//	defval,\
//	minval,\
//	maxval,\
//	valptr,\
//	setfunc,\
//	XP_DOUBLE_OPTION_DUMMY,\
//	XP_STRING_OPTION_DUMMY,\
//	XP_KEY_OPTION_DUMMY,\
//}
//
//#define COLOR_INDEX_OPTION(name, defval, valptr, help) \
//XP_INT_OPTION(name, defval, 0, MAX_COLORS-1, valptr, null, XP_OPTFLAG_CONFIG_COLORS, help)
//
//#define COLOR_INDEX_OPTION_WITH_SETFUNC(name, defval, valptr, setfunc, help) \
//XP_INT_OPTION(name, defval, 0, MAX_COLORS-1, valptr, setfunc, XP_OPTFLAG_CONFIG_COLORS, help)
//
//
//#define XP_DOUBLE_OPTION(name, defval, minval, maxval, valptr, setfunc, flags, help) \
//{ \
//    xp_double_option,\
//	name,\
//	flags, xp_option_origin_default,\
//	help,\
//	null,\
//	XP_NOARG_OPTION_DUMMY,\
//	XP_BOOL_OPTION_DUMMY,\
//	XP_INT_OPTION_DUMMY,\
//	defval,\
//	minval,\
//	maxval,\
//	valptr,\
//	setfunc,\
//	XP_STRING_OPTION_DUMMY,\
//	XP_KEY_OPTION_DUMMY,\
//}
//
//#define XP_STRING_OPTION(name, defval, valptr, size, setfunc, private_data, getfunc, flags, help) \
//{ \
//    xp_string_option,\
//	name,\
//	flags, xp_option_origin_default,\
//	help,\
//	private_data,\
//	XP_NOARG_OPTION_DUMMY,\
//	XP_BOOL_OPTION_DUMMY,\
//	XP_INT_OPTION_DUMMY,\
//	XP_DOUBLE_OPTION_DUMMY,\
//	defval,\
//	valptr,\
//	size,\
//	setfunc,\
//	getfunc,\
//	XP_KEY_OPTION_DUMMY,\
//}
//
//#define XP_KEY_OPTION(name, defval, key, help) \
//{ \
//    xp_key_option,\
//	name,\
//	XP_OPTFLAG_DEFAULT, xp_option_origin_default,\
//	help,\
//	null,\
//	XP_NOARG_OPTION_DUMMY,\
//	XP_BOOL_OPTION_DUMMY,\
//	XP_INT_OPTION_DUMMY,\
//	XP_DOUBLE_OPTION_DUMMY,\
//	XP_STRING_OPTION_DUMMY,\
//	defval,\
//	null,\
//	key,\
//}
//
//#define XP_KS_UNKNOWN (-1)
//typedef int xp_keysym_t;
///* no const because of mfc client */
//extern xp_keysym_t String_to_xp_keysym(/*const*/ String str);
//extern keys_t Generic_lookup_key(xp_keysym_t ks, bool reset);
//
//typedef struct {
//    xp_keysym_t	keysym;
//    keys_t	key;
//} xp_keydefs_t;
//
//extern xp_keydefs_t	*keydefs;
//extern int		num_keydefs;
//extern int		max_keydefs;


}