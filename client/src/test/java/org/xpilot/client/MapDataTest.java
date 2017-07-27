package org.xpilot.client;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import static java.io.File.createTempFile;
import static org.junit.Assert.*;
import static org.xpilot.client.MapData.Mapdata_download;

/**
 * Created by mark on 7/15/2017.
 */
public class MapDataTest {
    @Test
    public void mapdata_download() throws Exception {

        Logger logger = LoggerFactory.getLogger(MapDataTest.class);
        String location = "http://xpilot.sourceforge.net/maps/ndh-1.3.xpd";
        try{
            URL url = new URL(location);
            File tempfile = createTempFile("ndh",null);
            Mapdata_download(url,tempfile);
            logger.info("tempfile name {}",tempfile.getPath());



        }catch (IOException ex){
            logger.logger.error("failed to get map textures data",ex);

        }
    }

}