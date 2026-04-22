package net.sf.jxpilot.map;

import static net.sf.jxpilot.map.BlockMapSetup.*;
import static net.sf.jxpilot.map.MapBlock.BLOCK_SIZE;
import net.sf.jxpilot.graphics.*;
import net.sf.jxpilot.util.Utilities;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Class that holds (most) drawing info for blocks.
 * @author Vlad Firoiu
 */
public class DrawableBlock implements Drawable
{
	public static final Color BLOCK_COLOR = Color.BLUE;
	public static final Color TREASURE_COLOR = Color.RED;
	public static final Color BASE_COLOR = Color.WHITE;
	public static final Color FUEL_COLOR = Color.RED;
	public static final Color WORMHOLE_COLOR = Color.MAGENTA;

	/**
	 * Default rectangle.
	 */
	private static final Rectangle REC;
	
	/**
	 * Default circle.
	 */
	private static final Ellipse2D CIRCLE;
	
	/**
	 * Various polygons for recs.
	 */
	private static final Polygon REC_RU, REC_RD, REC_LU, REC_LD;
	
	/**
	 * Array of blocks that contain only drawing info.
	 */
	protected static final DrawableBlock[] staticBlocks;
	
	static {
		REC = getDefaultRectangle();
		CIRCLE = getDefaultCircle();
		
		REC_RU = getRec(true, true);
		REC_RD = getRec(true, false);
		REC_LU = getRec(false, true);
		REC_LD = getRec(false, false);
		
		staticBlocks = new DrawableBlock[256];
		
		initBlocks();
	}

	protected static DrawableBlock getBlock(byte block_type) {
		return staticBlocks[Utilities.getUnsignedByte(block_type)];
	}
	
	private static Rectangle getDefaultRectangle() {
		return new Rectangle(0,0, BLOCK_SIZE,BLOCK_SIZE);
	}
	
	private static Ellipse2D getDefaultCircle() {
		return new Ellipse2D.Float(0,0, BLOCK_SIZE,BLOCK_SIZE);
	}	
	
	private static Polygon getRec(boolean right, boolean up) {
		Polygon poly = new Polygon();
		if (right)
		{
			if (up)
			{
				poly.addPoint(BLOCK_SIZE, 0);
				poly.addPoint(BLOCK_SIZE, BLOCK_SIZE);
				poly.addPoint(0,BLOCK_SIZE);
			}
			else
			{
				poly.addPoint(BLOCK_SIZE, 0);
				poly.addPoint(BLOCK_SIZE, BLOCK_SIZE);
				poly.addPoint(0, 0);			
			}
		} else {
			if (up)
			{
				poly.addPoint(0, 0);
				poly.addPoint(BLOCK_SIZE, BLOCK_SIZE);
				poly.addPoint(0, BLOCK_SIZE);
			}
			else
			{
				poly.addPoint(0, 0);
				poly.addPoint(BLOCK_SIZE, 0);
				poly.addPoint(0, BLOCK_SIZE);	
			}
		}
		return poly;
	}
	
	private static void initBlocks() {
		for(int i =0;i<staticBlocks.length;i++) {
			staticBlocks[i] = createBlock((byte)i);
		}
	}
	
	private static DrawableBlock createBlock(byte block_type)
	{		
		switch(block_type)
		{
		case SETUP_SPACE: return new DrawableBlock(SETUP_SPACE);
		case SETUP_FILLED: return new DrawableBlock(SETUP_FILLED, REC, BLOCK_COLOR, true);
		case SETUP_FUEL: return  new DrawableBlock(SETUP_FUEL, REC, BLOCK_COLOR, false);
		case SETUP_REC_RU: return new DrawableBlock(SETUP_REC_RU, REC_RU, BLOCK_COLOR, true);
		case SETUP_REC_RD: return new DrawableBlock(SETUP_REC_RD, REC_RD, BLOCK_COLOR, true);
		case SETUP_REC_LU: return new DrawableBlock(SETUP_REC_LU, REC_LU, BLOCK_COLOR, true);
		case SETUP_REC_LD: return new DrawableBlock(SETUP_REC_LD, REC_LD, BLOCK_COLOR, true);
		case SETUP_WORM_NORMAL: return new DrawableBlock(SETUP_WORM_NORMAL, CIRCLE, WORMHOLE_COLOR, true);
		case SETUP_WORM_IN: return new DrawableBlock(SETUP_WORM_IN, CIRCLE, WORMHOLE_COLOR, true);
		case SETUP_WORM_OUT: return new DrawableBlock(SETUP_WORM_OUT, CIRCLE, WORMHOLE_COLOR, true);
		}
		
		if (block_type>= SETUP_TREASURE && block_type <SETUP_TREASURE + 10) {
			return new DrawableBlock(block_type, CIRCLE, TREASURE_COLOR,false);
		}
		
		/*
		if (block_data >= SETUP_BASE_LOWEST && block_data <= SETUP_BASE_HIGHEST)
		{
			if (block_data>= SETUP_BASE_DOWN && block_data < SETUP_BASE_DOWN +10)
			{
				return new DrawableBlock(block_data,num, x, y, 
						new Line2D.Float(0,BLOCK_SIZE,BLOCK_SIZE, BLOCK_SIZE), BASE_COLOR, false);
			}
			
			return new DrawableBlock(block_data,num, x, y, 
					new Line2D.Float(0, 0, BLOCK_SIZE, 0), BASE_COLOR, false);
		}
		*/
		
		//default case
		return new DrawableBlock(block_type);
	}
	
	//instance variable
	/**
	 * Data that is stored in map.
	 */
	public final byte block_type;
	
	/**
	 * Whether the block is actually drawn.
	 */
	public final boolean isDrawn;
	
	//drawing information
	public final Shape shape;
	public final Color color;
	public final boolean isFilled;
	private final BufferedImage image;	
	
	/**
	 * This constructor is used for non-drawable blocks.
	 * @param block_type
	 */
	private DrawableBlock(byte block_type) {
		this.block_type=block_type;
		this.isDrawn=false;
		this.shape = null;
		this.color = null;
		this.isFilled = false;
		image = null;	
	}
	
	/**
	 * Creates a clone of other. To be used by subclasses.
	 * @param other 
	 */
	protected DrawableBlock(DrawableBlock other) {
		this.block_type = other.block_type;
		this.isDrawn = other.isDrawn;
		this.shape = other.shape;
		this.color = other.color;
		this.isFilled = other.isFilled;
		this.image = other.image;
	}
	
	/**
	 * This constructor is used for blocks that are drawn.
	 * @param block_type
	 * @param shape
	 * @param color
	 * @param fill
	 */
	private DrawableBlock(byte block_type, Shape shape, Color color, boolean fill) {
		this.block_type = block_type;
		this.isDrawn=true;
		this.shape = shape;
		this.color = color;
		this.isFilled = fill;
		image = createImage();
	}
	
	private BufferedImage createImage() {
		//BufferedImage temp = new BufferedImage(BLOCK_SIZE, BLOCK_SIZE, BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage temp = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		this.paintDrawable(temp.createGraphics());
		return temp;
	}
	
	public byte getBlockType(){return block_type;}
	public Shape getShape(){return shape;}
	public Color getColor(){return color;}
	public boolean isFilled(){return isFilled;}
	
	/**
	 * Note that this method draws without using the block's stored image.
	 */
	@Override
	public void paintDrawable(Graphics2D g2d)
	{
		if(!isDrawn) return;
		
		g2d.setColor(color);
		if(isFilled)
		{
			g2d.fill(shape);
		}
		else
		{
			g2d.draw(shape);
		}
	}
	
	/**
	 * @param g2d
	 * @param x Position in blocks.
	 * @param y Position in blocks.
	 */
	public void paintBlock(Graphics2D g2d, int x, int y)
	{
		if(isDrawn)
		g2d.drawImage(image, x*BLOCK_SIZE, y*BLOCK_SIZE, null);
	}
}