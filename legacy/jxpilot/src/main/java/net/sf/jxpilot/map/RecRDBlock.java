package net.sf.jxpilot.map;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Color;

import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.geom.Polygon2D;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class RecRDBlock extends Block {
	public static final Color REC_RD_COLOR = Color.BLUE;
	
	private static final Polygon RD_POLYGON;
	
	private static final Polygon2D RD_POLYGON2D;
	public static Polygon2DAdaptor getRDPolygon2D(){return RD_POLYGON2D;}
	
	private static final BufferedImage RD_IMAGE;
	
	static {
		int[] xpoints = {0, BLOCK_SIZE, BLOCK_SIZE};
		int[] ypoints = {0, 0, BLOCK_SIZE};
		RD_POLYGON = new Polygon(xpoints, ypoints, 3);
		RD_POLYGON2D = new Polygon2D(RD_POLYGON);
		RD_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = RD_IMAGE.createGraphics();
		g2d.setColor(REC_RD_COLOR);
		g2d.setTransform(BLOCK_TRANSFORM);
		RD_POLYGON2D.fill(g2d);
		g2d.dispose();
	}
	
	public RecRDBlock(int num, int x, int y) {
		super(BlockType.REC_RD, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(RD_IMAGE, x, y, g2d);
	}
}