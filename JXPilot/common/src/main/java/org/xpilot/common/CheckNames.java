package org.xpilot.common;
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

import java.net.URI;
import java.net.URISyntaxException;

import static org.xpilot.common.Pack.MAX_DISP_LEN;
import static org.xpilot.common.Pack.MAX_HOST_LEN;
import static org.xpilot.common.Pack.MAX_NAME_LEN;

public class CheckNames{
public static final boolean NAME_OK = true;
public static final boolean NAME_ERROR = false;

public static final char PROT_EXT = '~';

private static int nameCount =0;


public boolean Check_user_name(String name)
{

    if (name.length() > MAX_NAME_LEN - 1)
	return NAME_ERROR;
    if (name.trim().isEmpty())
	return NAME_ERROR;

    if(!name.matches("\\p{Print}")) return NAME_ERROR;

    return NAME_OK;
}

public String Fix_user_name(String name)
{

    if (name.length() > MAX_NAME_LEN - 1)
    {
        name = name.substring(0, MAX_NAME_LEN - 1);
    }else if (name.trim().isEmpty()) {
        name = new String("XXXXXXXXXXXXXXX");
    }

    name = name.replaceAll("[^\\p{Print}]","x");

    return name;

}

boolean Check_nick_name(String name)
{
    if (name.length() > MAX_NAME_LEN - 1)
        return NAME_ERROR;
    if (name.trim().isEmpty())
        return NAME_ERROR;

    if(!Character.isUpperCase(name.charAt(0)))
    {
        return NAME_ERROR;
    }
    if(!name.matches("\\p{Print}")) return NAME_ERROR;

    if (name.endsWith(" ") || name.endsWith("\t"))
	return NAME_ERROR;

    return NAME_OK;
}

String Fix_nick_name(String name)
{
    if (name.length() > MAX_NAME_LEN - 1)
    {
        name = name.substring(0, MAX_NAME_LEN - 1);
    }else if (name.trim().isEmpty()) {
        name = "X" + nameCount++;
    }

    name = name.replaceAll("[^\\p{Print}]","x");
    name = name.trim();
    StringBuilder newName = new StringBuilder(name.length());
    for(int i = 0; i<name.length();i++)
    {
        char c = name.charAt(i);
        if (i == 0)
        {
            newName.append(Character.toUpperCase(c));
        }
        else
        {
            newName.append(Character.toLowerCase(c));
        }
    }


    return name;
}


boolean Check_host_name(String name)
{

    if (name.length() > MAX_HOST_LEN - 1)
	return NAME_ERROR;

    try{
        // WORKAROUND: add any scheme to make the resulting URI valid.
        URI uri = new URI("dummy://" + name); // may throw URISyntaxException
        if (uri.getHost() == null ) {
            throw new URISyntaxException(uri.toString(),
                    "Invalid host name");
        }

        // here, additional checks can be performed, such as
        // presence of path, query, fragment, ...

    } catch (URISyntaxException ex) {
        return NAME_ERROR;
    }

    // validation succeeded
    return NAME_OK;
}

public String Fix_host_name(String name)
{

    if (name.length() > MAX_HOST_LEN - 1)
        name = name.substring(0, MAX_NAME_LEN - 1);

    // A tiny bit weaker than original code
    name = name.replaceAll("[^0-9a-zA-Z\\-:.]","x");

    return name;
}

/*
 */
boolean Check_disp_name(String name)
{

    if (name.length() > MAX_DISP_LEN - 1)
        return NAME_ERROR;
    if (name.trim().isEmpty())
        return NAME_ERROR;

    if(!name.matches("\\p{Print}")) return NAME_ERROR;

    return NAME_OK;
}

String Fix_disp_name(String name)
{


        if (name.length() > MAX_DISP_LEN - 1)
        {
            name = name.substring(0, MAX_DISP_LEN - 1);
        }else if (name.trim().isEmpty()) {
            name = new String("XXXXXXXXXXXXXXX");
        }

        name = name.replaceAll("[^\\p{Print}]","x");

        return name;
}
}