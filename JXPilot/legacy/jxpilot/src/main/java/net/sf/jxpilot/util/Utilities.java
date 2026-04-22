package net.sf.jxpilot.util;

import java.awt.*;
import java.awt.geom.*;

/**
 * Class with various useful functions.
 * @author vlad
 */
public class Utilities {
	
	public static double trueMod(double x, double m) {
		//return ((x%m)+m)%m;
		
		double doubleMod = x%m;
		return doubleMod >= 0 ? doubleMod : doubleMod+m;
	}
	
	/**
	 * 
	 * @param x The number to get modded.
	 * @param m The modulus.
	 * @return A positive value for x mod m, since x%m sometimes returns negative values.
	 */
	public static int trueMod(int x, int m) {
		//return ((x%m)+m)%m;
		
		int intMod = x%m;
		return intMod >= 0 ? intMod : intMod+m;
	}
	
	public static int getUnsignedShort(short val) {
		return (int)((char)val);
	}
	public static short getUnsignedByte(byte val) {
		return (short)(0xff & val);
	}
	
	/**
	 * @param heading The angle in 128 degree mode.
	 * @return The angle in radians.
	 */
	public static double getAngleFrom128(double heading) {
		return Math.PI/64.0 * heading;
	}
	
	public static double pow(double base, int exponent) {
		double product=1.0;
		if(exponent<0) {
			for(;exponent != 0;exponent++) product *= base;
			return 1.0/product;
		} else {
			for(;exponent != 0;exponent--) product *= base;
			return product;
		}
	}
	
	public static long pow(int base, int exponent) {
		long product = 1;
		for(;exponent != 0;exponent--) product *= base;
		return product;
	}
	
	public static int square(int base){return base*base;}
	
	public static int times2(int n) {return n << 1;}
	public static int pow2(int exp) {return 1 << exp;}
	public static int timesPow2(int n, int exp) {return n << exp;}
	
	/**
	 * Draws a String taking into account a flip of the Graphics object.
	 * @param g2d Graphics object on which to draw the String.
	 * @param s The String to be drawn.
	 * @param x Where the String is drawn.
	 * @param y Where the String is drawn.
	 */
	public static void drawFlippedString(Graphics2D g2d, String s, float x, float y)
	{
		g2d.scale(1, -1);
		g2d.drawString(s, x, -y);
		g2d.scale(1, -1);
	}
	
	/**
	 * Draws a String taking into account a flip of the Graphics object.
	 * (x,y) is where the top middle of where the string will be.
	 * @param g2d Graphics object on which to draw the String.
	 * @param s The String to be drawn.
	 * @param x Where the String is drawn.
	 * @param y Where the String is drawn.
	 */
	public static void drawAdjustedStringDown(Graphics2D g2d, String s, float x, float y)
	{
		Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(s, g2d);
		drawFlippedString(g2d, s, (float)(x-bounds.getWidth()/2.0), (float)(y-bounds.getHeight()));
	}
	
	/**
	 * Draws a String taking into account a flip of the Graphics object.
	 * (x,y) is where the bottom middle of where the string will be.
	 * @param g2d Graphics object on which to draw the String.
	 * @param s The String to be drawn.
	 * @param x Where the String is drawn.
	 * @param y Where the String is drawn.
	 */
	public static void drawAdjustedStringUp(Graphics2D g2d, String s, float x, float y)
	{
		Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(s, g2d);
		drawFlippedString(g2d, s, (float)(x-bounds.getWidth()/2.0), (float)(y+bounds.getHeight()*3.0/2.0));
	}
	
	/**
	 * Draws a String taking into account a flip of the Graphics object.
	 * (x,y) is where the right middle of where the string will be.
	 * @param g2d Graphics object on which to draw the String.
	 * @param s The String to be drawn.
	 * @param x Where the String is drawn.
	 * @param y Where the String is drawn.
	 */
	public static void drawAdjustedStringLeft(Graphics2D g2d, String s, float x, float y)
	{
		Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(s, g2d);
		drawFlippedString(g2d, s, (float)(x-bounds.getWidth()-1.0), (float)(y-bounds.getHeight()/2.0));
	}
	
	/**
	 * Draws a String taking into account a flip of the Graphics object.
	 * (x,y) is where the left middle of where the string will be.
	 * @param g2d Graphics object on which to draw the String.
	 * @param s The String to be drawn.
	 * @param x Where the String is drawn.
	 * @param y Where the String is drawn.
	 */
	public static void drawAdjustedStringRight(Graphics2D g2d, String s, float x, float y)
	{
		drawFlippedString(g2d, s, x+1, (float)(y-g2d.getFontMetrics().getHeight()/2.0));
	}
	
	/**
	 * Note that 0<= start, end <= size
	 * @param size The modulus.
	 * @param start The start value.
	 * @param end The end value.
	 * @return A value for end that is the closest possible to start without changing end mod size.
	 */
	public static int wrap(int size, int start, int end)
	{
		int dif = end-start;
		
		if (dif > size/2)
		{
			return end-size;
		}
		
		if (dif < -size/2)
		{
			return end+size;
		}
		
		return end;
	}
	
	/**
	 * Note that 0<= start, end <= size
	 * @param size The modulus.
	 * @param start The start value.
	 * @param end The end value.
	 * @return A value for end that is the closest possible to start without changing end mod size.
	 */
	public static double wrap(double size, double start, double end) {
		double dif = end-start;
		if (dif > size/2) return end-size;
		if (dif < -size/2) return end+size;
		return end;
	}
	
	public static Point2D wrapPoint(double width, double height, double startX, double startY, Point2D point) {
		point.setLocation(wrap(width, startX, point.getX()),wrap(height, startY, point.getY()));
		return point;
	}
	
	/**
	 * Invokes wrap() on x2 and y2 with respect to the width/x1 and height/y1.
	 * @param width The wrapping width.
	 * @param height The wrapping height.
	 * @param line The current line dimensions.
	 * @return The given line.
	 */
	public static Line2D wrapLine(int width, int height, Line2D line) {
		line.setLine(line.getX1(), line.getY1(), wrap(width, line.getX1(), line.getX2()), wrap(height, line.getY1(), line.getY2()));
		return line;
	}
	
	public static Line2D wrapLine(double width, double height, double startX, double startY, Line2D line) {
		line.setLine(wrap(width, startX, line.getX1()), wrap(height, startY, line.getY1()),
					wrap(width, startX, line.getX2()), wrap(height, startY, line.getY2()));
		return line;
	}
	
	/**
	 * Null char = 0. Often found at end of strings.
	 */
	public static final char NULL_CHAR = '\0';
	
	/**
     * Removes null character from end of string.<br>
     * It doesn't copy the string. See {@link String#substring(int, int)}.
     * 
     * @param stringToFix
     *            String with null character in end.
     * @return Fixed string, w/o nul character in end.
     */
    public static String removeNullCharacter(String stringToFix) {
    	if(stringToFix.isEmpty()) return stringToFix;
    	
    	if (stringToFix.charAt(stringToFix.length()-1) != NULL_CHAR) return stringToFix;
    	
    	return stringToFix.substring(0, stringToFix.length() - 1);
    }
}