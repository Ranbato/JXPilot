package net.sf.jxpilot.game;

import java.awt.*;
import java.util.regex.*;
//import net.sf.jgamelibrary.geom.MovingPolygon2D;

/**
 * Simple class that defines the basic elements of a shipshape and
 * provides a way to turn ship into a string and back.
 * @author Vlad Firoiu
 */
public class ShipShape implements Cloneable {
	public static final String intRegex = "\\-?\\d+";
	private static final Pattern intPattern = Pattern.compile(intRegex);
	
	public static final String POINT_SEPARATOR = ",";
	public static final String pointRegex = intRegex + POINT_SEPARATOR + intRegex;
	private static final Pattern pointPattern = Pattern.compile(pointRegex);
	
	public static final String parenthesesRegex = "\\([^\\(\\)]*\\)";
	private static final Pattern parenthesesPattern = Pattern.compile(parenthesesRegex);
	
	public static final String markerRegex = "[A-Z]{2}";
	private static final Pattern markerPattern = Pattern.compile(markerRegex);
	
	private static final String SHIP_MARKER = "SH";
	private static final String ENGINE_MARKER = "EN";
	private static final String MAIN_GUN_MARKER = "MG";
	private static final String LEFT_LIGHT_MARKER = "LL";
	private static final String RIGHT_LIGHT_MARKER = "RL";
	private static final String MAIN_RACK_MARKER = "MR";
	
	public static String toString(Point p) {
		return p == null ? "null" :
			p.x + POINT_SEPARATOR + p.y;
	}
	
	/**
	 * @param str The String to be parsed.
	 * @return The first point (x,y) if the String contains the pattern int,int
	 * 			null otherwise.
	 */
	public static Point parsePoint(String str) {
		Matcher matcher = pointPattern.matcher(str);
		
		if (!matcher.find()) return null;
		
		String point = matcher.group();
		
		//System.out.println("Found point group: "+point);
		
		matcher = intPattern.matcher(point);
		
		if (!matcher.find()) {
			//System.out.println("No ints found in point parse!");
			return null;
		}
		int x = Integer.parseInt(matcher.group());
		
		if (!matcher.find()) {
			//System.out.println("No ints found in point parse!");
			return null;		
		}
		int y = Integer.parseInt(matcher.group());
	
		return new Point(x, y);
	}
	
	/**
	 * Default triangular {@code ShipShape}.
	 */
	public static final ShipShape DEFAULT_SHIP = 
		new ShipShape(new Point(-8,0), new Point(14,0), new Point(-8,8), new Point(-8,-8), new Point(14,0));
	
	private short version = 0x3200;
	private Polygon shape;
	//private MovingPolygon2D shape2D;
	private Point engine, main_gun,
					left_light, right_light,
					main_rack;
	private String extras = "\0";
	
	private ShipShape() {
		shape = new Polygon();
	}
	
	public ShipShape(Point engine, Point main_gun, Point left_light, Point right_light, Point main_rack) {
		this.engine = engine;
		this.main_gun = main_gun;
		this.left_light = left_light;
		this.right_light = right_light;
		this.main_rack = main_rack;
		shape = new Polygon();
		shape.addPoint(main_gun.x, main_gun.y);
		shape.addPoint(left_light.x, left_light.y);
		shape.addPoint(right_light.x, right_light.y);
		//shape2D = new MovingPolygon2D(shape);
	}
	
	public Polygon getShape(){return shape;}
	public Point getEngine(){return engine;}
	public Point getMainGun(){return main_gun;}
	public Point getLeftLight(){return left_light;}
	public Point getRightLight(){return right_light;}
	public Point getMainRack(){return main_rack;}
	//public MovingPolygon2D getShape2D(){return shape2D;}
	public String getExtras(){return extras;}
	
	/**
	 * Creates a clone of this {@code ShipShape}.
	 * @return The clone.
	 */
	@Override
	public ShipShape clone() {
		return new ShipShape(engine, main_gun, left_light, right_light, main_rack);
	}
	
	/**
	 * @return A String representation of the ShipShape that is understood by XPilot
	 * 			and can be sent over the Internet.
	 */
	public String toString() {
		StringBuilder temp= new StringBuilder("(SH:");
		for(int i =0;i<shape.npoints;i++) {
			temp.append(' ').append(shape.xpoints[i]).append(POINT_SEPARATOR).append(shape.ypoints[i]);
		}
		
		temp.append(")(EN: ").append(toString(engine)) 
				.append(")(MG: ").append(toString(main_gun))
				.append(")(LL: ").append(toString(left_light))
				.append(")(RL: ").append(toString(right_light))
				.append(")(MR: ").append(toString(main_rack)).append(")\0")
				.append(extras);
		
		return temp.toString();
	}
	
	public static ShipShape parseShip(String shipStr, String extension)
	{
		ShipShape ship=new ShipShape();
		
		Matcher parenthesesMatcher = parenthesesPattern.matcher(shipStr);
		
		/**
		 * Whether or not the ship's polygon is empty.
		 * If it is, then the other points should be added in.
		 */
		boolean empty = true;
		
		while(parenthesesMatcher.find())
		{
			String parentheses = parenthesesMatcher.group();
			//System.out.println("Found parentheses group: "+ parentheses);
			
			Matcher markerMatcher = markerPattern.matcher(parentheses);
			markerMatcher.find();
			String marker = markerMatcher.group();
			
			if (marker.equals(SHIP_MARKER))
			{
				Matcher pointMatcher = pointPattern.matcher(parentheses);
				
				while(pointMatcher.find())
				{	
					String pointStr = pointMatcher.group();
					//System.out.println("Found point group: "+pointStr);
					
					Point p = parsePoint(pointStr);
					
					ship.shape.addPoint(p.x, p.y);
					empty = false;
				}
				continue;
			}
			
			if (marker.equals(ENGINE_MARKER))
			{
				Matcher pointMatcher = pointPattern.matcher(parentheses);
				pointMatcher.find();
				ship.engine = parsePoint(pointMatcher.group());
				continue;
			}
			if (marker.equals(MAIN_GUN_MARKER))
			{
				Matcher pointMatcher = pointPattern.matcher(parentheses);
				pointMatcher.find();
				ship.main_gun = parsePoint(pointMatcher.group());
				continue;
			}
			if (marker.equals(LEFT_LIGHT_MARKER))
			{
				Matcher pointMatcher = pointPattern.matcher(parentheses);
				pointMatcher.find();
				ship.left_light = parsePoint(pointMatcher.group());
				continue;
			}
			if (marker.equals(RIGHT_LIGHT_MARKER))
			{
				Matcher pointMatcher = pointPattern.matcher(parentheses);
				pointMatcher.find();
				
				ship.right_light = parsePoint(pointMatcher.group());
				continue;
			}
			if (marker.equals(MAIN_RACK_MARKER))
			{
				Matcher pointMatcher = pointPattern.matcher(parentheses);
				pointMatcher.find();
				
				ship.main_rack = parsePoint(pointMatcher.group());
				continue;
			}
		}
		
		//makes sure ship isn't empty
		if(empty) {
			Polygon shape = ship.shape;
			
			//shape.addPoint(ship.engine.x, ship.engine.y);
			shape.addPoint(ship.main_gun.x, ship.main_gun.y);
			shape.addPoint(ship.left_light.x, ship.left_light.y);
			shape.addPoint(ship.right_light.x, ship.right_light.y);
			//shape.addPoint(ship.main_rack.x, ship.main_rack.y);	
		}
		ship.extras = extension;
		
		return ship;
		
	}
	
}