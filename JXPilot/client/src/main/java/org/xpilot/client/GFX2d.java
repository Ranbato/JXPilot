package org.xpilot.client;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xpilot.client.xpm.Xpm;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

import static org.xpilot.common.Const.RES;
import static org.xpilot.common.XPMath.tcos;
import static org.xpilot.common.XPMath.tsin;


public class GFX2d {

    static Logger logger = LoggerFactory.getLogger(GFX2d.class);

    private static final GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();


        int RGB24(int r, int g,int b) {
return ((((b)&255) << 16) | (((g)&255) << 8) | ((r)&255));}

int RED_VALUE(int col){return  ((col) &255);}
	int GREEN_VALUE(int col) {return(((col) >> 8) &255);}
	int BLUE_VALUE(int col) {return (((col) >>16) &255);}

	private int c;

/*
 * Purpose: bounding box for one image or a set of images.
 * The xmin and ymin elements give the lowest coordinate which
 * has a non-black color value.  The xmax and ymax elements
 * give the highest coordinate which has a non-black color.
 * The number of pixels covered by one box is given by:
 * (xmax + 1 - xmin, ymax + 1 - ymin).
 */
static class BBox{
    int xmin, ymin;
    int xmax, ymax;
}


/*
 * Purpose: A device/os independent structure to do keep 24bit images in.
 * an instance of XPPicture can contain more than 1 image,
 * This feature is  useful for structural identical bitmaps (example: items), 
 * and rotated images. When dealing with rotated images, the first image
 * in the XPPicture structure is used as texture for the transformation of
 * the others.
 */

static public class XPPicture {
    int	width, height;
    int		count;

    public ArrayList<BufferedImage> getData() {
        return data;
    }

    ArrayList<BufferedImage> data;

    ArrayList<BBox>	bbox;
}


String texturePath = null;    /* Configured list of texture directories */
String realTexturePath = null; /* Real texture lookup path */

/*
 *   Purpose: initialize xp_picture structure and load it from file.
 *   Error handling is incomplete.
 *
 *   return 0 on success.
 *   return -1 on error.
 */

public	XPPicture Picture_init ( String filename, int count)
{

    XPPicture picture = new XPPicture();
    picture.count = count;
    picture.data = new ArrayList<>( Math.abs(count));

    if (!Picture_load(picture, filename))
	return null;

    if (count > 1)
        if (Picture_rotate(picture) == -1)
	    return null;

    picture.bbox = new ArrayList<>( Math.abs(count));

    Picture_get_bounding_box(picture);

    return picture;
}


/*
 * Find full path for a picture filename.
 */
 File Picture_find_path(String filename)
{
    if (filename == null|| filename.isEmpty())
	return null;

    /*
     * If filename doesn't contain a slash
     * then we also try the realTexturePath, if it exists.
     */
    File file = null;
    if(realTexturePath != null){
    	String[] dirs = realTexturePath.split(":");
    	for(String dir:dirs){
    		file = new File(dir,filename);
    		if(file.canRead()){
    			break;
			}
		}
	}else {
        file = new File(filename);
        if(!file.canRead()){
            file = null;
        }
	}



    /*logger.logger.error("Can't find PPM file \"{}\"", filename);*/
    return(file);
}

/*
 * Purpose: load images in to the xp_picture structure.
 * format is only binary PPM's at the moment.
 * More error handling and a better understanding of the PPM standard
 * would be good. But suffices for a proof of concept.
 *
 * return 0 on success.
 * return -1 on error.
 */
 boolean Picture_load(XPPicture picture, String filename)
{

    File path;


    if ((path = Picture_find_path(filename)) == null) {
	logger.logger.error("Cannot find picture file \"{}\"", filename);
	return false;
    }

    try{


	int count = 0;
        BufferedImage mainImage = null;
        Image tempImage = null;

	if(path.getName().endsWith("xpm")){
	     tempImage = Xpm.xpmToImage(path);

    }else{
        ImageIO.scanForPlugins();
        tempImage = ImageIO.read(path);

    }


        //Images returned from ImageIO are NOT managedImages
        //Therefore, we copy it into a ManagedImage
        mainImage = gc.createCompatibleImage(tempImage.getWidth(null),tempImage.getHeight(null));
        Graphics2D g2d = mainImage.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.drawImage(tempImage,0,0,null);
        g2d.dispose();




        picture.height = mainImage.getHeight();
        if (picture.count > 0) {
            count = 1;
            picture.width = mainImage.getWidth();
        } else  {
            count = -picture.count;
            picture.width = mainImage.getWidth() / count;
        }

        for(int i = 0; i<count;i++) {
            logger.debug("Cutting {}:{}-{} of {}",i,picture.width*i,(picture.width*i)+(picture.width-1),mainImage.getWidth());
            picture.data.add(mainImage.getSubimage(picture.width*i,0,picture.width,picture.height));
        }


    } catch (FileNotFoundException e) {
        logger.logger.error("Cannot open \"{}\"", path);
    } catch (IOException e) {
        e.printStackTrace();
    }

    return true;
}


    /*
     * Purpose: We want to provide rotation, a picture which is rotated has
     * just 1 image with index=0 as source, which is rotated 360 degrees,
     * more pictures = higher resolution.
     *
     * Note that this is done by traversing the target image, and selecting
     * the corresponding source colorvalue, this assures there will be no
     * gaps in the image.
     * @todo use a transform?
     */
int Picture_rotate(XPPicture picture)
{
    int size, x, y, image;
    int color;
    BufferedImage newImage, oldImage;
    
    size = picture.height;
    for (image = 1; image < picture.count; image++) {
        oldImage = picture.data.get(image);
        newImage = new BufferedImage(oldImage.getWidth(),oldImage.getHeight(),oldImage.getType());

	for (y = 0; y < size; y++) {
	    for (x = 0; x < size; x++) {
		color = Picture_get_rotated_pixel(picture, x, y, image);
		newImage.setRGB(x, y, color);
	    }
	    picture.data.set(image,newImage);
	}
    }
    return 0;
}



/*
 * Purpose: get the color value of a 1x1 pixel,
 * This is a wrapper for looking up in the data array.
 */
int Picture_get_pixel( XPPicture picture, int image,
			    int x, int y)
{
    if (x < 0 || y < 0
	|| x >= (int)picture.width || y >= (int)picture.height) {
	return RGB24(0, 0, 0);
	/*
	 * this might be an error, but it can be a convenience to allow the
	 * function to be called with indexes out of range, so i won't
	 * introduce error handling here. Return value is defaulted to black.
	 * There is already code that relies on this behavior
	 */
    } else
	return picture.data.get(image).getRGB(x, y);
}

/*
 * Purpose: Find the color value of the 1x1 pixel with upperleft corner x,y.
 * Note that x and y is doubles.
 */
 int Picture_get_pixel_avg( XPPicture picture,
				       int image, double x, double y)
{
    int		r_x, r_y;
    double	frac_x, frac_y;
    int		i;
    int	[]c = new int[4];
    double[]	p = new double[4];
    double	r, g, b;

    frac_x = x - (int)(x);
    frac_y = y - (int)(y);

    r_x = (int)x;
    r_y = (int)y;

    c[0] = Picture_get_pixel(picture, image, r_x, r_y);
    c[1] = Picture_get_pixel(picture, image, r_x + 1, r_y);
    c[2] = Picture_get_pixel(picture, image, r_x, r_y + 1);
    c[3] = Picture_get_pixel(picture, image, r_x + 1, r_y + 1);

    p[0] = (1 - frac_x) * (1 - frac_y);
    p[1] = (frac_x) * (1 - frac_y);
    p[2] = (1 - frac_x) * frac_y;
    p[3] = frac_x * frac_y;

    r = 0;
    g = 0;
    b = 0;

    for (i = 0; i < 4; i++) {
	r += RED_VALUE(c[i]) * p[i];
	g += GREEN_VALUE(c[i]) * p[i];
	b += BLUE_VALUE(c[i]) * p[i];
    }
    return RGB24(( char)r, ( char)g, ( char)b);
}

/*
 * Purpose: Rotate a point around the center of an image
 * and return the matching color in the base image.
 * A picture that contains a rotated image uses all it images to make
 * a full 360 degree rotation, which is reflected in the angle calculation.
 * (first image is ang=0 and is used to index the texture for the color value)
 * Note: this function is used by the rotation code,
 * and that is why the it's rotating the "wrong" direction.
 */
int Picture_get_rotated_pixel( XPPicture picture,
				    int x, int y, int image)
{

    int		angle;
    double	rot_x, rot_y;

    angle = ((image  * RES) / picture.count) % 128;

    x -= picture.width / 2;
    y -= picture.height / 2;

    rot_x = (tcos(angle) * x - tsin(angle) * y) + picture.width / 2;
    rot_y = (tsin(angle) * x + tcos(angle) * y) + picture.height / 2;

    return (Picture_get_pixel_avg(picture, 0, rot_x, rot_y));
}

//
//
///*
// * Purpose: find color values from x + xfrac to x + xfrac + scale.
// * This is the most called function in the scaling routine,
// * so i address the picture data directly.
// */
//static void Picture_scale_x_slice( XPPicture  picture, int image,
//				  int *r, int *g, int *b, int x, int y,
//				  double xscale, double xfrac, double yfrac)
//
//{
//    double weight;
//    RGB_COLOR col;
//    RGB_COLOR *image_data = picture.data[image] + x + y * picture.width ;
//
//    if (xscale > xfrac) {
//	col = *image_data;
//	weight = xfrac * yfrac;
//	*r += (int)(RED_VALUE(col) * weight);
//        *g += (int)(GREEN_VALUE(col) * weight);
//	*b += (int)(BLUE_VALUE(col) * weight);
//
//	xscale -= xfrac;
//	image_data++;
//
//        weight = yfrac;
//	if (yfrac == 1) {
//    	    while(xscale >= 1.0) {
//		col = *image_data;
//		*r += (int)(RED_VALUE(col));
//		*g += (int)(GREEN_VALUE(col));
//		*b += (int)(BLUE_VALUE(col));
//		image_data++;
//		xscale -=1.0;
//	    }
//	} else {
//	    while(xscale >= 1.0) {
//		col = *image_data;
//		*r += (int)(RED_VALUE(col) * weight);
//		*g += (int)(GREEN_VALUE(col) * weight);
//		*b += (int)(BLUE_VALUE(col) * weight);
//		image_data++;
//		xscale -=1.0;
//	    }
//	}
//    }
//    if (xscale > .00001) {
//	col = *image_data;
//	weight = yfrac * xscale;
//	*r += (int)(RED_VALUE(col) * weight);
//        *g += (int)(GREEN_VALUE(col) * weight);
//	*b += (int)(BLUE_VALUE(col) * weight);
//    }
//}
//
///*
// * Purpose: Calculate the average color of a rectangle in an image,
// * This is used by the scaling algorithm.
// */
//int Picture_get_pixel_area(const XPPicture *picture, int image,
//				 double x_1, double y_1, double dx, double dy)
//{
//    int r, g, b;
//    double area;
//
//    int x, y;
//    double xfrac, yfrac;
//
//    r = 0;
//    g = 0;
//    b = 0;
//
//    x = (int)x_1;
//    y = (int)y_1;
//
//    xfrac = (x + 1) - x_1;
//    yfrac = (y + 1) - y_1;
//
//    area = dx * dy;
//
//    if (dy > yfrac) {
//	Picture_scale_x_slice(picture, image, &r, &g, &b, x, y, dx,
//			      xfrac, yfrac);
//	dy -= yfrac;
//	y++;
//	while (dy >= 1.0) {
//	    Picture_scale_x_slice(picture, image, &r, &g, &b, x, y, dx,
//				  xfrac, 1.0);
//	    y++;
//	    dy -=1.0;
//	}
//    }
//    if (dy > .00001)
//	Picture_scale_x_slice(picture, image, &r, &g, &b, x, y, dx, xfrac, dy);
//
//    return RGB24((unsigned char)(r/area), (unsigned char)(g/area),
//		 (unsigned char)(b/area));
//}

/*
 * Purpose: We want to know the bounding box of a picture,
 * so that we can reduce the number of operations done on
 * a picture.
 */
void Picture_get_bounding_box(XPPicture picture) {
    int x, y, i;
    BBox  box;

    for (i = 0; i < Math.abs(picture.count); i++) {
        box = new BBox();
        picture.bbox.add(i,box);
        box.xmin = picture.width - 1;
        box.xmax = 0;
        box.ymin = picture.height - 1;
        box.ymax = 0;

        for (y = 0; y < (int) picture.height; y++) {
            for (x = 0; x < (int) picture.width; x++) {
                int color = Picture_get_pixel(picture, i, x, y);
                if (color != 0) {
                    if (box.xmin > x)
                        box.xmin = x;
                    if (box.xmax < x)
                        box.xmax = x;
                    if (box.ymin > y)
                        box.ymin = y;
                    if (box.ymax < y)
                        box.ymax = y;
                }
            }
        }
    }
}



}
