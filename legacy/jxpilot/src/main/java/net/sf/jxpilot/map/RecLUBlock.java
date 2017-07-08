package net.sf.jxpilot.map;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Color;

import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.geom.Polygon2D;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class RecLUBlock extends Block {
	public static final Color REC_LU_COLOR = Color.BLUE;
	
	private static final Polygon LU_POLYGON;
	
	private static final Polygon2D LU_POLYGON2D;
	public static Polygon2DAdaptor getLUPolygon2D(){return LU_POLYGON2D;}
	
	private static final BufferedImage LU_IMAGE;
	
	static {
		int[] xpoints = {0, BLOCK_SIZE, 0};
		int[] ypoints = {0, BLOCK_SIZE, BLOCK_SIZE};
		LU_POLYGON = new Polygon(xpoints, ypoints, 3);
		LU_POLYGON2D = new Polygon2D(LU_POLYGON);
		LU_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = LU_IMAGE.createGraphics();
		g2d.setColor(REC_LU_COLOR);
		g2d.setTransform(BLOCK_TRANSFORM);
		LU_POLYGON2D.fill(g2d);
		g2d.dispose();
	}
	
	public RecLUBlock(int num, int x, int y) {
		super(BlockType.REC_LU, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(LU_IMAGE, x, y, g2d);
	}
}