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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class MapData{

    static final Logger logger = LoggerFactory.getLogger(MapData.class);


    /* kps - you should be able to change this without a recompile */
static final String DATADIR =".xpilot_data";
static final int COPY_BUF_SIZE =8192;


static boolean setup_done = false;

boolean Mapdata_setup(String urlstr)
{
    URL url;
    String name;
            File dir;
    File path;
    boolean rv = false;

    if (setup_done)
	return true;

    try
    {
        url = new URL(urlstr);
    } catch (MalformedURLException e)
    {
        logger.warn("malformed URL: {}", urlstr,e);
        return false;
    }


    String urlPath = url.getPath();
    if (urlPath.isEmpty()) {
	logger.warn("no file name in URL: {}", urlstr);
	return rv;
    }

    int idx = urlPath.lastIndexOf('/');
    name = urlPath.substring(idx);
    // todo from gfx2d
//    if (realTexturePath != null) {
//	for (dir = strtok(realTexturePath, ":"); dir; dir = strtok(null, ":"))
//	    if (access(dir, R_OK | W_OK | X_OK) == 0)
//		break;
//    }
//
    if (dir == null) {

	/* realTexturePath hasn't got a directory with proper access rights */
	/* so lets create one into users home dir */

	String home = System.getenv("user.home");
	if (home == null) {
	    logger.error("couldn't access any dir in {} and HOME is unset", path);
        return rv;
	}

	    dir = new File(home,DATADIR);

	// todo add Permissions?
	if(!dir.mkdir())  {
		logger.error("failed to create directory {}", dir);
        return rv;
	    }
	}


    path = new File(dir,name);

    if (!name.contains(".")) {
	logger.error("no extension in file name {}.", name);
        return rv;
    }

    /* temporarily make path point to the directory name */
//    ptr = strrchr(path, '.');
//    *ptr = '\0';

    /* add this new texture directory to texturePath */
    if (realTexturePath == null) {
	realTexturePath = path.getPath();
    } else {
	realTexturePath = realTexturePath + ":"+path.getPath();
    }

    if (path.exists()) {
	logger.warn("Required bitmaps have already been downloaded.");
	rv = true;
        return rv;
    }
    /* reset path so that it points to the package file name */
//    *ptr = '.';

    logger.warn("Downloading map data from {} to {}.", urlstr, path);

    if (!Mapdata_download(url, path)) {
	logger.warn("downloading map data failed");
        return rv;
    }

    if (!Mapdata_extract(path)) {
	logger.warn("extracting map data failed");
        return rv;
    }

    rv = true;
    setup_done = true;

    return rv;
}


static boolean Mapdata_extract(File name)
{

    File out;
    int retval;
    size_t rlen, wlen;
    File dir;
    byte buf[] = new byte[COPY_BUF_SIZE];
    String fname[256];
    int size;
    int count, i;


    String fileOnly = name.getName();
    if (!fileOnly.contains(".")) {
        logger.error("no extension in file name {}.", name);
        return false;
    }

    // todo add Permissions?
    if(!dir.mkdir())  {
        logger.error("failed to create directory {}", dir);
        return false;
    }

    try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(name)))
    {

    int offset = 0;

    if (in.read(buf, 0,COPY_BUF_SIZE) == -1) {
	logger.error("failed to read header from {}", name);
	in.close();
	return false;
    }

    String data = new String(buf, Charset.defaultCharset());
    if (sscanf(buf, "XPD %d\n", &count) != 1) {
	error("invalid header in %s", name);
	gzclose(in);
	return 0;
    }

    for (i = 0; i < count; i++) {

	if (gzgets(in, buf, COPY_BUF_SIZE) == Z_NULL) {
	    error("failed to read file info from %s", name);
	    gzclose(in);
	    return 0;
	}

	sprintf(fname, "%s%c", dir, PATHNAME_SEP);

	if (sscanf(buf, "%s\n%ld\n", fname + strlen(dir) + 1, &size) != 2) {
	    error("failed to parse file info %s", buf);
	    gzclose(in);
	    return 0;
	}

	/* security check */
	if (strchr(fname + strlen(dir) + 1, PATHNAME_SEP) != null) {
	    error("file name %s is illegal", fname);
	    gzclose(in);
	    return 0;
	}

	logger.warn("Extracting %s (%ld)", fname, size);

	if ((out = fopen(fname, "wb")) == null) {
	    error("failed to open %s for writing", buf);
	    gzclose(in);
	    return 0;
	}

	while (size > 0) {
	    retval = gzread(in, buf, Math.min(COPY_BUF_SIZE, (unsigned)size));
	    if (retval == -1) {
		error("error when reading %s", name);
		gzclose(in);
		fclose(out);
		return 0;
	    }
	    if (retval == 0) {
		error("unexpected end of file %s", name);
		gzclose(in);
		fclose(out);
		return 0;
	    }

	    rlen = retval;
	    wlen = fwrite(buf, 1, rlen, out);
	    if (wlen != rlen) {
		error("failed to write to %s", fname);
		gzclose(in);
		fclose(out);
		return 0;
	    }

	    size -= rlen;
	}

	fclose(out);
    }

    gzclose(in);
    } catch (IOException e)
    {
        logger.error("failed to open {} for reading", name.toString());
        return false;
    }

    return true;
}


static int Mapdata_download(const URL *url, String filePath)
{
    char buf[1024];
    int rv, header, c, len, i;
    sock_t s;
    FILE *f = null;
    size_t n;

    if (strncmp("http", url.protocol, 4) != 0) {
	error("unsupported protocol %s", url.protocol);
	return false;
    }

    if (sock_open_tcp(&s) == SOCK_IS_ERROR) {
	error("failed to create a socket");
	return false;
    }
    if (sock_connect(&s, url.host, url.port) == SOCK_IS_ERROR) {
	error("couldn't connect to download address");
	sock_close(&s);
	return false;
    }

    if (url.query) {
	if (snprintf(buf, sizeof buf,
	     "GET %s?%s HTTP/1.1\r\nHost: %s:%d\r\nConnection: close\r\n\r\n",
	     url.path, url.query, url.host, url.port) == -1) {
	    error("too long URL");
	    sock_close(&s);
	    return false;
	}

    } else {
	if (snprintf(buf, sizeof buf,
	     "GET %s HTTP/1.1\r\nHost: %s:%d\r\nConnection: close\r\n\r\n",
	     url.path, url.host, url.port) == -1) {

	    error("too long URL");
	    sock_close(&s);
	    return false;
	}
    }

    if (sock_write(&s, buf, (int)strlen(buf)) == -1) {
	error("socket write failed");
	sock_close(&s);
	return false;
    }

    header = 2;
    c = 0;

    for(;;) {
	len = 0;
	while (len < 100) {
	    if ((i = sock_read(&s, buf + len, sizeof(buf) - len)) == -1) {
		error("socket read failed");
		rv = false;
		goto done;
	    }
	    if (i == 0)
		break;
	    len += i;
	}

	if (len == 0) {
	    rv = !header;
	    break;
	}

	if (header == 2) {
	    if (strncmp(buf, "HTTP", 4)) {
		rv = false;
		break;
	    }
	    i = 0;
	    while (buf[i] != ' ') {
		i++;
		if (i >= len - 1) {
		    rv = false;
		    goto done;
		}
	    }
	    i++;
	    if (buf[i] != '2') {   /* HTTP status code starts with 2 */
		rv = false;
		break;
	    }
	    header = 1;
	}

	printf("#");
	fflush(stdout);

	if (header) {
	    for (i = 0; i < len; i++) {
		if (c % 2 == 0 && buf[i] == '\r')
		    c++;
		else if (c % 2 == 1 && buf[i] == '\n')
		    c++;
		else
		    c = 0;

		if (c == 4) {
		    header = 0;
		    if ((f = fopen(filePath, "wb")) == null) {
			error("failed to open %s", filePath);
			rv = false;
			goto done;
		    }
		    if (i < len - 1) {
			n = len - i - 1;
			memmove(buf, buf + i + 1, n);
			len = len - i - 1;
		    } else if (i == len - 1) {
			len = 0;
		    }
		}
	    }
	}

	if (!header && len) {
	    n = len;
	    if (fwrite(buf, 1, n, f) < n) {
		error("file write failed");
		rv =  false;
		break;
	    }
	}
    }
 done:
    printf("\n");
    if (f)
	if (fclose(f) != 0)
	    error("Error closing texture file %s", filePath);
    sock_close(&s);
    return rv;
}

