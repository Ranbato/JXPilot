package net.sf.jxpilot.map;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Color;

import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.geom.Polygon2D;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class RecRUBlock extends Block {
	public static final Color REC_RU_COLOR = Color.BLUE;
	
	private static final Polygon RU_POLYGON;
	
	private static final Polygon2D RU_POLYGON2D;
	public static Polygon2DAdaptor getRUPolygon2D(){return RU_POLYGON2D;}
	
	private static final BufferedImage RU_IMAGE;
	
	static {
		int[] xpoints = {BLOCK_SIZE, BLOCK_SIZE, 0};
		int[] ypoints = {0, BLOCK_SIZE, BLOCK_SIZE};
		RU_POLYGON = new Polygon(xpoints, ypoints, 3);
		RU_POLYGON2D = new Polygon2D(RU_POLYGON);
		RU_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = RU_IMAGE.createGraphics();
		g2d.setColor(REC_RU_COLOR);
		g2d.setTransform(BLOCK_TRANSFORM);
		RU_POLYGON2D.fill(g2d);
		g2d.dispose();
	}
	
	public RecRUBlock(int num, int x, int y) {
		super(BlockType.REC_RU, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(RU_IMAGE, x, y, g2d);
	}
}
