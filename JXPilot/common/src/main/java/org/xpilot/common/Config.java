package org.xpilot.common;

import org.slf4j.*;

import static java.lang.System.getenv;
import static org.xpilot.common.Version.PACKAGE;
import static org.xpilot.common.Version.VERSION;
import static org.xpilot.common.XPConfig.*;
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

public class Config{

private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Config.class);

String Conf_datadir()
{
    String conf = CONF_DATADIR;

    return conf;
}

String Conf_defaults_file_name()
{
    String conf = CONF_DEFAULTS_FILE_NAME;

    return conf;
}

String Conf_password_file_name()
{
    String conf = CONF_PASSWORD_FILE_NAME;

    return conf;
}


String Conf_mapdir()
{
    String conf = CONF_MAPDIR;

    return conf;
}

String Conf_fontdir()
{
    String conf = CONF_FONTDIR;

    return conf;
}

String Conf_default_map()
{
    String conf = CONF_DEFAULT_MAP;

    return conf;
}

String Conf_servermotdfile()
{
    String conf = CONF_SERVERMOTDFILE;
    String env = "XPILOTSERVERMOTD";
    String filename;

    filename = getenv(env);
    if (filename == null)
	filename = conf;

    return filename;
}

String Conf_localmotdfile()
{
    String conf = CONF_LOCALMOTDFILE;

    return conf;
}

String conf_logfile_string = CONF_LOGFILE;

String Conf_logfile()
{
    return conf_logfile_string;
}

String Conf_ship_file()
{
    String conf = CONF_SHIP_FILE;

    return conf;
}

String Conf_texturedir()
{
    String conf = CONF_TEXTUREDIR;

    return conf;
}

String Conf_localguru()
{
    String conf = CONF_LOCALGURU;

    return conf;
}

String Conf_robotfile()
{
    String conf = CONF_ROBOTFILE;

    return conf;
}

String Conf_zcat_ext()
{
    String conf = CONF_ZCAT_EXT;

    return conf;
}

String Conf_zcat_format()
{
    String conf = CONF_ZCAT_FORMAT;

    return conf;
}

String Conf_sounddir()
{
    String conf = CONF_SOUNDDIR;

    return conf;
}

String Conf_soundfile()
{
    String conf = CONF_SOUNDFILE;

    return conf;
}


void Conf_print()
{
    logger.warn("============================================================");
    logger.warn("VERSION                   = %s", VERSION);
    logger.warn("PACKAGE                   = %s", PACKAGE);

    /*
    TODO which of these will be enabled?

#ifdef DBE
    logger.warn("DBE");
#endif
#ifdef MBX
    logger.warn("MBX");
#endif
#ifdef PLOCKSERVER
    logger.warn("PLOCKSERVER");
#endif
#ifdef DEVELOPMENT
    logger.warn("DEVELOPMENT");
#endif
*/
    logger.warn("Conf_localguru()          = %s", Conf_localguru());
    logger.warn("Conf_datadir()            = %s", Conf_datadir());
    logger.warn("Conf_defaults_file_name() = %s", Conf_defaults_file_name());
    logger.warn("Conf_password_file_name() = %s", Conf_password_file_name());
    logger.warn("Conf_mapdir()             = %s", Conf_mapdir());
    logger.warn("Conf_default_map()        = %s", Conf_default_map());
    logger.warn("Conf_servermotdfile()     = %s", Conf_servermotdfile());
    logger.warn("Conf_robotfile()          = %s", Conf_robotfile());
    logger.warn("Conf_logfile()            = %s", Conf_logfile());
    logger.warn("Conf_localmotdfile()      = %s", Conf_localmotdfile());
    logger.warn("Conf_ship_file()          = %s", Conf_ship_file());
    logger.warn("Conf_texturedir()         = %s", Conf_texturedir());
    logger.warn("Conf_fontdir()            = %s", Conf_fontdir());
    logger.warn("Conf_sounddir()           = %s", Conf_sounddir());
    logger.warn("Conf_soundfile()          = %s", Conf_soundfile());
    logger.warn("Conf_zcat_ext()           = %s", Conf_zcat_ext());
    logger.warn("Conf_zcat_format()        = %s", Conf_zcat_format());
    logger.warn("============================================================");
}
}