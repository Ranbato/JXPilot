package net.sf.jxpilot.map;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Color;

import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.geom.Polygon2D;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class RecLDBlock extends Block {
	public static final Color REC_LD_COLOR = Color.BLUE;
	
	private static final Polygon LD_POLYGON;
	
	private static final Polygon2D LD_POLYGON2D;
	public static Polygon2DAdaptor getLDPolygon2D(){return LD_POLYGON2D;}
	
	private static final BufferedImage LD_IMAGE;
	
	static {
		int[] xpoints = {0, BLOCK_SIZE, 0};
		int[] ypoints = {0, 0, BLOCK_SIZE};
		LD_POLYGON = new Polygon(xpoints, ypoints, 3);
		LD_POLYGON2D = new Polygon2D(LD_POLYGON);
		LD_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = LD_IMAGE.createGraphics();
		g2d.setColor(REC_LD_COLOR);
		g2d.setTransform(BLOCK_TRANSFORM);
		LD_POLYGON2D.fill(g2d);
		g2d.dispose();
	}
	
	public RecLDBlock(int num, int x, int y) {
		super(BlockType.REC_LD, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(LD_IMAGE, x, y, g2d);
	}
}