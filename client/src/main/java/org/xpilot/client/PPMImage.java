package org.xpilot.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by mark on 7/17/2017.
 */
public class PPMImage {

 static final private Logger logger = LoggerFactory.getLogger(PPMImage.class);

    private static String MAGIC_PGM = "P6\n";

      int width;
      int height;
      int size;
    int maxcolval;

    private int c;

    private InputStream in;





     public BufferedImage toBufferedImage( byte[] data) {
        if (maxcolval < 256) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int r, g, b, k = 0, pixel;
            if (maxcolval == 255) {                                      // don't scale
                for (int y = 0; y < height; y++) {
                    for (int x = 0; (x < width) && ((k + 3) < data.length); x++) {
                        r = data[k++] & 0xFF;
                        g = data[k++] & 0xFF;
                        b = data[k++] & 0xFF;
                        pixel = 0xFF000000 + (r << 16) + (g << 8) + b;
                        image.setRGB(x, y, pixel);
                    }
                }
            } else {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; (x < width) && ((k + 3) < data.length); x++) {
                        r = data[k++] & 0xFF;
                        r = ((r * 255) + (maxcolval >> 1)) / maxcolval;  // scale to 0..255 range
                        g = data[k++] & 0xFF;
                        g = ((g * 255) + (maxcolval >> 1)) / maxcolval;
                        b = data[k++] & 0xFF;
                        b = ((b * 255) + (maxcolval >> 1)) / maxcolval;
                        pixel = 0xFF000000 + (r << 16) + (g << 8) + b;
                        image.setRGB(x, y, pixel);
                    }
                }
            }
            return image;
        } else {


            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int r, g, b, k = 0, pixel;
            for (int y = 0; y < height; y++) {
                for (int x = 0; (x < width) && ((k + 6) < data.length); x++) {
                    r = (data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
                    r = ((r * 255) + (maxcolval >> 1)) / maxcolval;  // scale to 0..255 range
                    g = (data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
                    g = ((g * 255) + (maxcolval >> 1)) / maxcolval;
                    b = (data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
                    b = ((b * 255) + (maxcolval >> 1)) / maxcolval;
                    pixel = 0xFF000000 + (r << 16) + (g << 8) + b;
                    image.setRGB(x, y, pixel);
                }
            }
            return image;
        }
    }

    /*
     * Fetch next ASCII character from PPM file.
     * Strip comments starting with "#" from the input.
     * On error return -1.
     */
     int getChar() throws IOException {

        c = in.read();
        if (c == '#') {
            do {
                c = in.read();
            } while (c != '\n' && c != -1);
        }

        return c;

    }

    /*
     * Verify last input character is a whitespace character
     * and skip all following whitespace.
     * On error return -1.
     */
     int skipWhitespace() throws IOException {
        if (!Character.isWhitespace(c))
            return -1;
        do {
            c = getChar();
        } while (Character.isWhitespace(c));

        return 0;
    }

    /*
     * Verify last input character is a digit
     * and extract a decimal value from the input stream.
     * On error return -1.
     */
     int getDecimal( ) throws IOException {
    StringBuilder sb = new StringBuilder();
    while(Character.isDigit(c)) {
        sb.append((char) c);
        c = getChar();
    }
        return c;
    }

    boolean readPPMFile(File path){


    try(BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(path))){

        int c1 = 0;
        int c2 = 0;

   if ((c1 = getChar()) != 'P' ||
            (c2 = getChar()) != '6') {
        logger.error("\"{}\" does not contain a valid binary PPM file.\n Invalid magic \"{}{}\"",
                path,c1,c2);
        inputStream.close();
        return false;
    }
    getChar();
    skipWhitespace();
    width = getDecimal();
    skipWhitespace();
    height = getDecimal();
    skipWhitespace();
    maxcolval = getDecimal();

    if (!Character.isWhitespace(c) || maxcolval != 255) {
        logger.error("\"%s\" does not contain a valid binary PPM file.\n",
                path);
        inputStream.close();
        return false;
    }

    picture.height = height;
    if (picture.count > 0) {
        count = 1;
        picture.width = width;
    } else  {
        count = -picture.count;
        picture.width = width / count;
    }

    for (int p = 0; p < count; p++) {
        if (!(picture.data[p] =
                XMALLOC(RGB_COLOR, picture.width * picture.height))) {
            logger.error("Not enough memory.");
            return -1;
        }
    }

    for (int y = 0 ; y < (int)picture.height ; y++) {
        for (int p = 0; p < count ; p++) {
            for (int x = 0; x < (int)picture.width ; x++) {
                r = getc(inputStream);
                g = getc(inputStream);
                b = getc(inputStream);
                Picture_set_pixel(picture, p, x, y, RGB24(r, g, b));
            }
        }
	/* skip the rest */
        for (int p = width % count * 3; p > 0; p--)
            getc(inputStream);
    }

    } catch (FileNotFoundException e) {
        logger.error("Cannot open \"{}\"", path);
    } catch (IOException e) {
        e.printStackTrace();
    }


        return 0;
}
