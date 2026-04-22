package net.sf.jxpilot.game;

import static net.sf.jxpilot.map.BlockMapSetup.*;
import static net.sf.jxpilot.map.MapBlock.BLOCK_SIZE;

import java.awt.*;

import net.sf.jxpilot.graphics.Drawable;

public class Cannon extends CannonHolder implements Drawable 
{
	public static final Color CANNON_COLOR = Color.WHITE;

	/**
	 * Measured in blocks.
	 */
	private final int x, y;
	private final CannonType type;
	private final Shape cannonShape;
	
	private Cannon(CannonType type, int num, int x, int y)
	{
		this.type = type;
		super.num = num;
		this.x = x;
		this.y = y;
		cannonShape = type.shape;
	}
	
	public static Cannon createCannon(byte block_type, int num, int x, int y)
	{
		CannonType type = CannonType.getCannonType(block_type);
		
		if (type!=null)
		{
			return new Cannon(type, num, x, y);
		}
		return null;
	}
	
	public int getX(){return x;}
	public int getY(){return y;}
	public CannonType getCannonType(){return type;}
	
	public void paintDrawable(Graphics2D g2d)
	{
		if(dead_time>0) return;
		
		//AffineTransform saved = g2d.getTransform();
		
		int x = this.x*BLOCK_SIZE;
		int y = this.y*BLOCK_SIZE;
		
		g2d.translate(x, y);
		g2d.setColor(CANNON_COLOR);
		g2d.draw(cannonShape);
		g2d.translate(-x, -y);
		
		//g2d.setTransform(saved);
	}
	
	public static enum CannonType
	{
		RIGHT(SETUP_CANNON_RIGHT), LEFT(SETUP_CANNON_LEFT), UP(SETUP_CANNON_UP), DOWN(SETUP_CANNON_DOWN);
		
		
		static
		{
			for(CannonType c : values())
			{
				c.shape = getCannonShape(c);
			}
		}
		
		public final byte block_type;
		private Shape shape;
		
		private CannonType(byte block_type)
		{
			this.block_type = block_type;
			//this.shape = getCannonShape(this);
		}
		
		public Shape getShape(){return shape;}
		
		public byte getBlockType()
		{
			return block_type;
		}
		
		public static CannonType getCannonType(byte block_type)
		{	
			
			for(CannonType type : CannonType.values())
			{
				if(type.block_type==block_type) return type;
			}
			
			return null;
		}
		
		public static Polygon getCannonShape(CannonType type)
		{
			Polygon poly = new Polygon();
			
			switch(type)
			{
			case RIGHT:
				poly.addPoint(0, 0);
				poly.addPoint(0, BLOCK_SIZE);
				poly.addPoint(BLOCK_SIZE/4, BLOCK_SIZE/2);
				break;
			case LEFT:
				poly.addPoint(BLOCK_SIZE, 0);
				poly.addPoint(BLOCK_SIZE, BLOCK_SIZE);
				poly.addPoint(BLOCK_SIZE*3/4, BLOCK_SIZE/2);
				break;
			case DOWN:
				poly.addPoint(0, BLOCK_SIZE);
				poly.addPoint(BLOCK_SIZE, BLOCK_SIZE);
				poly.addPoint(BLOCK_SIZE/2, BLOCK_SIZE*3/4);
				break;
			case UP:
				poly.addPoint(0, 0);
				poly.addPoint(BLOCK_SIZE, 0);
				poly.addPoint(BLOCK_SIZE/2, BLOCK_SIZE/4);
				break;
			}
			
			return poly;
		}
	}
}