package org.xpilot.client;

/*
 * XPilot NG, a multiplayer space war game.
 *
 * Copyright (C) 2001 Juha Lindström <juhal@users.sourceforge.net>
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpilot.common.ShipShape;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class MapData{

    static final Logger logger = LoggerFactory.getLogger(MapData.class);


    /* kps - you should be able to change this without a recompile */
static final String DATADIR =".xpilot_data";
static final int COPY_BUF_SIZE =8192;

// todo This needs to come from gfx2d
String realTexturePath;


static boolean setup_done = false;

boolean Mapdata_setup(String urlstr) {
    URL url;
    String name;
    File dir=null;
    File path = null;
    boolean rv = false;

    if (setup_done)
        return true;

    try {
        url = new URL(urlstr);
    } catch (MalformedURLException e) {
        logger.warn("malformed URL: {}", urlstr, e);
        return false;
    }


    String urlPath = url.getPath();
    if (urlPath.isEmpty()) {
        logger.warn("no file name in URL: {}", urlstr);
        return rv;
    }

    int idx = urlPath.lastIndexOf('/');
    name = urlPath.substring(idx);

    if (realTexturePath != null) {
        String[] paths = realTexturePath.split(":");

        for (int i = 0; i < paths.length; i++) {
            dir = new File(paths[i]);
            if (dir.canRead() && dir.canWrite() && dir.canExecute()) {
                break;
            }
            dir = null;
        }

        if (dir == null) {

	/* realTexturePath hasn't got a directory with proper access rights */
	/* so lets create one into users home dir */

            String home = System.getenv("user.home");
            if (home == null) {
                logger.error("couldn't access any dir in {} and HOME is unset", path);
                return rv;
            }

            dir = new File(home, DATADIR);

            // todo add Permissions?
            if (!dir.mkdir()) {
                logger.error("failed to create directory {}", dir);
                return rv;
            }
        }


        path = new File(dir, name);

        if (!name.contains(".")) {
            logger.error("no extension in file name {}.", name);
            return rv;
        }


    /* add this new texture directory to texturePath */
        if (realTexturePath == null) {
            realTexturePath = path.getPath();
        } else {
            realTexturePath = realTexturePath + ":" + path.getPath();
        }

        if (path.exists()) {
            logger.warn("Required bitmaps have already been downloaded.");
            rv = true;
            return rv;
        }


        logger.warn("Downloading map data from {} to {}.", urlstr, path);

        if (!Mapdata_download(url, path)) {
            logger.warn("downloading map data failed");
            return rv;
        }

        if (!Mapdata_extract(path)) {
            logger.warn("extracting map data failed");
            return rv;
        }
    }

        rv = true;
        setup_done = true;

        return rv;
    }



static boolean Mapdata_extract(File name)
{

   int retval;
    int rlen, wlen;
    File dir;
    byte buf[] = new byte[COPY_BUF_SIZE];
    String data;
    File fname;
    int size;
    int count, i;


    int ext = name.getPath().lastIndexOf('.');
    if (ext == -1) {
        logger.error("no extension in file name {}.", name);
        return false;
    }
    // create directory to hold file contents
    dir = new File(name.getPath().substring(0,ext-1));

    // todo add Permissions?
    if(!dir.mkdir())  {
        logger.error("failed to create directory {}", dir);
        return false;
    }

    try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(name)))
    {

    Scanner input = new Scanner(in);
    input.useDelimiter("[\\r\\n]");
    String header = input.next();
        if(header.matches("XPD \\d+")) {
            count = Integer.parseInt(header.substring(4));
        }else {
            logger.error("invalid header in {}", name);

            return false;
        }
  

    for (i = 0; i < count; i++) {

        data = input.next();

        fname = new File(dir, data);
        size = input.nextInt();

        logger.warn("Extracting {} ({})", fname.getPath(), size);

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fname))) {
            while (size > 0) {
                retval = in.read(buf, 0, Math.min(COPY_BUF_SIZE, size));
                if (retval == -1) {
                    logger.error("error when reading {}", name);
                    return false;
                }
                rlen = retval;
                try {
                    out.write(buf, 0, rlen);
                } catch (IOException e) {

                    logger.error("failed to write to {}", fname);
                    return false;
                }

                size -= rlen;
            }
        } catch (IOException e) {
            logger.error("failed to open {} for writing", fname.getPath(), e);
            return false;
        }

    }

    } catch (IOException e)
    {
        logger.error("failed to open {} for reading", name.toString());
        return false;
    }

    return true;
}


static boolean Mapdata_download(URL url, File filePath) {
    byte[] buf = new byte[1024];
    int header, c, len, i;
    int read;
    int write;
    File f = null;
    int n;
    boolean rv = true;

    if (!url.getProtocol().equalsIgnoreCase("http")) {
        logger.warn("unsupported protocol {}, trying anyway", url.getProtocol());
    }
    try {
        URLConnection conn = url.openConnection();
        BufferedInputStream inputStream = new BufferedInputStream(conn.getInputStream());
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath));

        while((read = inputStream.read(buf,0,buf.length)) != -1){
            outputStream.write(buf,0,read);
        }
        outputStream.close();
        inputStream.close();


    } catch (IOException ex) {
        logger.error("failed to get map textures data", ex);
        return false;
    }

    return rv;
}
}

