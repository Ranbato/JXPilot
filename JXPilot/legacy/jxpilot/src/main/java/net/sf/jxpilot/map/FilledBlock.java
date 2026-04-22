package net.sf.jxpilot.map;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2D;
import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class FilledBlock extends Block {
	public static final Color FILLED_COLOR = Color.BLUE;
	
	private static final Polygon FILLED_POLYGON;
	
	private static final Polygon2D FILLED_POLYGON2D;
	public static Polygon2DAdaptor getFilledPolygon2D() {return FILLED_POLYGON2D;}
	
	private static final BufferedImage FILLED_IMAGE;
	
	static {
		int[] xpoints = {0, BLOCK_SIZE, BLOCK_SIZE, 0};
		int[] ypoints = {0, 0, BLOCK_SIZE, BLOCK_SIZE};
		FILLED_POLYGON = new Polygon(xpoints, ypoints, 4);
		FILLED_POLYGON2D = new Polygon2D(FILLED_POLYGON);
		FILLED_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = FILLED_IMAGE.createGraphics();
		g2d.setTransform(BLOCK_TRANSFORM);
		g2d.setColor(FILLED_COLOR);
		FILLED_POLYGON2D.fill(g2d);
		g2d.dispose();
	}
	
	public FilledBlock(int num, int x, int y) {
		super(BlockType.FILLED, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		//g2d.setColor(FILLED_COLOR);
		//GfxUtil.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE, g2d);
		GfxUtil.drawImage(FILLED_IMAGE, x, y, g2d);
	}
}
