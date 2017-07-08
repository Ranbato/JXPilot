package net.sf.jxpilot.graphics;

import static net.sf.jxpilot.map.MapBlock.BLOCK_SIZE;

import java.util.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.sf.jxpilot.map.BlockMap;
import net.sf.jxpilot.map.BlockMapSetup;
import net.sf.jxpilot.map.MapBlock;
import net.sf.jxpilot.util.Utilities;

/**
 * Class responsible for rendering the game.
 * @author Vlad Firoiu
 */
public class GameRenderer {
	
	private final BlockMap blockMap;
	private final BlockMapSetup setup;
	private final MapBlock[][] blocks;
	private final Iterable<? extends Drawable> drawables;
	
	/**
	 * Whether or not to render the game using a large buffered image for the map.
	 * Otherwise, the map is rendered block by block.
	 */
	private final boolean USE_MAP_BUFFER = false;
	
	/**
	 * How many times smaller the scaled map buffer should be.
	 */
	private final double scale_factor = 7;
	/**
	 * The scaled map buffer.
	 */
	private BufferedImage scaledMapBuffer;
	
	private double viewX, viewY,
					centerX, centerY;
	private int viewWidth, viewHeight;
	private double scale;
	
	private AffineTransform identity = new AffineTransform();
	/**
	 * Current display transform.
	 */
	private AffineTransform currentTransform = new AffineTransform();
	/**
	 * transform for switching between Cartesian mode and AWT mode. (y is changed with -y)
	 */
	private AffineTransform flippedTransform = new AffineTransform();
	private AffineTransform translated = new AffineTransform();

	private Color blockColor = Color.BLUE;
	private Color spaceColor = Color.BLACK;
	private Color shipColor = Color.white;
	
	public GameRenderer(BlockMap map, Iterable<? extends Drawable> drawables)
	{
		this.blockMap =map;
		this.setup = map.getSetup();
		this.blocks = map.getBlocks();
		this.drawables = drawables;
		
		initFlippedTransform();
		
		if(USE_MAP_BUFFER)
		{
			createMapBuffer();
		}
	}
	
	private void initFlippedTransform()
	{
		flippedTransform.scale(1, -1);
		flippedTransform.translate(0, -blockMap.getHeight());
	}
	
	private void setTransform()
	{	
		currentTransform.setToIdentity();
		currentTransform.translate(centerX, centerY);
		currentTransform.scale(scale, scale);
		currentTransform.translate(-viewX*BLOCK_SIZE, viewY*BLOCK_SIZE-setup.getY()*BLOCK_SIZE);
	}

	/**
	 * 
	 * @param screenG2D Graphics object on which to render
	 * @param viewX Focus of view within the game, in blocks.
	 * @param viewY Focus of view within the game, in blocks.
	 * @param centerX Desired location of focus, in pixels.
	 * @param centerY Desired location of focus, in pixels.
	 * @param viewWidth Width of view, in blocks.
	 * @param viewHeight Height of view, in blocks.
	 * @param scale Scale factor, in pixels/block.
	 */
	public void renderGame(Graphics2D screenG2D, 
			double viewX, double viewY, double centerX, double centerY, int viewWidth, int viewHeight, double scale)
	{
		this.viewX=viewX;
		this.viewY=viewY;
		this.centerX=centerX;
		this.centerY=centerY;
		this.viewWidth=viewWidth;
		this.viewHeight=viewHeight;
		this.scale = (scale / BLOCK_SIZE);
		
		setTransform();
		
		//screenG2D.setColor(spaceColor);
		//screenG2D.fillRect(0, 0, super.getWidth(), super.getHeight());
		
		int xRadius = 1+(viewWidth+1)/2,
			yRadius = 1+(viewHeight+1)/2;
		
		screenG2D.setTransform(currentTransform);
		screenG2D.transform(flippedTransform);
		screenG2D.setBackground(spaceColor);
		
		screenG2D.clearRect((int)(viewX-xRadius)*BLOCK_SIZE, (int)(viewY-yRadius)*BLOCK_SIZE,
							(2*xRadius)*BLOCK_SIZE, (2*yRadius)*BLOCK_SIZE);
		
		if(USE_MAP_BUFFER)
		{
			//screenG2D.setTransform(currentTransform);
			//screenG2D.transform(flippedTransform);
			paintScaledMapBuffer(screenG2D);
		}
		else
		{
			//screenG2D.setTransform(currentTransform);
			//screenG2D.transform(flippedTransform);
			paintBlocksArea(screenG2D, (int)viewX, (int)viewY, xRadius, yRadius);
			//paintBlocks(screenG2D);
		}
		
		//paintBlocks(screenG2D);
		
		/*
		for(int x= -1; x<=1;x++)
		{
			for (int y = -1; y<=1;y++)
			{
				translated.setTransform(currentTransform);
				translated.translate(x*blockMap.getWidth(), y*blockMap.getHeight());
				//screenG2D.setTransform(currentTransform);
				//screenG2D.translate(x*setup.getX()*BLOCK_SIZE, y*setup.getY()*BLOCK_SIZE);
				paintDrawables(screenG2D, translated);
			}
		}
		*/
		
		paintDrawables(screenG2D, currentTransform);		
		
		//screenG2D.setTransform(identity);
		//screenG2D.setColor(Color.WHITE);
		//screenG2D.drawString("TEST", 30, 30);

		//messagePool.render(screenG2D);

		//screenG2D.setTransform(currentTransform);
		//screenG2D.drawImage(mapBuffer, 0, 0, this);
	}

	private void createMapBuffer()
	{
		//scaledMapBuffer = Accelerator.createCompatibleImage((int)(mapWidth/scale_factor+0.5), (int)(mapHeight/scale_factor+0.5));
		scaledMapBuffer = new BufferedImage((int)(blockMap.getWidth()/scale_factor+0.5), 
				(int)(blockMap.getHeight()/scale_factor+0.5), 
				BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D mapG2D = scaledMapBuffer.createGraphics();
		
		mapG2D.scale(1.0/scale_factor, 1.0/scale_factor);
		
		mapG2D.setColor(spaceColor);
		mapG2D.fillRect(0, 0, scaledMapBuffer.getWidth(), scaledMapBuffer.getHeight());
		paintBlocks(mapG2D);
	}
	
	private void paintScaledMapBuffer(Graphics2D screenG2D)
	{
		//screenG2D.scale(scale_factor, scale_factor);
		screenG2D.drawImage(scaledMapBuffer, 0, 0, blockMap.getWidth(), blockMap.getHeight(), null);
		
		//screenG2D.
		//screenG2D.scale(1.0/scale_factor, 1.0/scale_factor);
	}
	
	/**
	 * The AffineTransform used for drawing.
	 */
	private AffineTransform drawTransform = new AffineTransform();
	/**
	 * Paints all drawables in the GameWorld.
	 * @param g2d The Graphics object on which to draw.
	 * @param transform The transform used (some drawables may change the transform,
	 * so this is used to change it back if necessary).
	 */
	private void paintDrawables(Graphics2D g2d, AffineTransform transform)
	{
		drawTransform.setTransform(transform);
		drawTransform.concatenate(flippedTransform);
		
		//g2d.setTransform(transform);
		//g2d.drawImage(mapBuffer, 0, 0, this);
		
		//g2d.transform(flippedTransform);
		
		if (drawables!=null)
		{
			for (Drawable d : drawables)
			{
				//g2d.setTransform(transform);

				//g2d.setTransform(transform);
				//g2d.transform(flippedTransform);

				//System.out.println("\nPainting drawable: ****************************************");
				g2d.setTransform(drawTransform);
				d.paintDrawable(g2d);
			}
			
		}
	}
	
	private void paintBlocks(Graphics2D g2)
	{
		for (MapBlock[] array : blocks)
		{
			for (MapBlock o : array)
			{		
				o.paintDrawable(g2);
			}
		}
	}

	/**
	 * Paints the map.
	 * @param g2 Graphics on which to paint the blocks
	 * @param centerX center of view area
	 * @param centerY center of view area
	 * @param xRadius half of view width
	 * @param yRadius half of view height
	 */
	private void paintBlocksArea(Graphics2D g2, int centerX, int centerY, int xRadius, int yRadius)
	{
		for(int x=centerX-xRadius;x<=centerX+xRadius;x++)
		{
			for(int y=centerY-yRadius;y<=centerY+yRadius;y++)
			{
				blocks[Utilities.trueMod(x, setup.getX())][Utilities.trueMod(y, setup.getY())].paintBlock(g2, x, y);
			}
		}
	}
}