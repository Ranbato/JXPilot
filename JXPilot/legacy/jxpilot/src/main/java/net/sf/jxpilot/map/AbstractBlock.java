package net.sf.jxpilot.map;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import net.sf.jgamelibrary.geom.Vector2D;
import net.sf.jgamelibrary.graphics.AbstractRenderer;

public interface AbstractBlock {
	/**
	 * The width and height of a Block.
	 */
	public static final int BLOCK_SIZE = 35;

	public static final AbstractRenderer BLOCK_RENDERER = new AbstractRenderer() {
		private final Vector2D CENTER_SCALE = new Vector2D(0.0,1.0);//bottom left
		@Override
		protected Vector2D getCenterScale() {return CENTER_SCALE;}

		@Override
		protected int getHeight() {return BLOCK_SIZE;}

		@Override
		protected int getWidth() {return BLOCK_SIZE;}
		
		private final Vector2D SCALE_FACTOR = new Vector2D(1.0,1.0);
		@Override
		protected Vector2D getScaleFactor() {return SCALE_FACTOR;}

		private final Vector2D VIEW_CENTER = new Vector2D();//origin
		@Override
		protected Point2D getViewCenter() {return VIEW_CENTER;}
	};
	public static final AffineTransform BLOCK_TRANSFORM = BLOCK_RENDERER.getDrawTransform();
	
	/**
	 * @see {@link BlockType}
	 * @return The corresponding {@code BlockType} constant.
	 */
	public BlockType getBlockType();
	/**
	 * @return The index of this block in the map data.
	 */
	public int getNum();
	/**
	 * @return The x coordinate of this block, in blocks.
	 */
	public int getX();
	/**
	 * @return The y coordinate of this block, in blocks.
	 */
	public int getY();
	
	/**
	 * Renders this block at the desired location.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param g2d The desired graphics context.
	 */
	public void render(int x, int y, Graphics2D g2d);
}
