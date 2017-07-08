package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;
import static net.sf.jxpilot.game.CannonType.DOWN;
import net.sf.jxpilot.game.NewCannon;

public class CannonDownBlock extends NewCannon implements AbstractBlock {
	public static final Color CANNON_DOWN_COLOR = Color.WHITE;
	
	public static final Polygon2DAdaptor getCannonDownPolygon2D() {return DOWN.getPolygon2D();}
	
	private static final BufferedImage CANNON_DOWN_IMAGE;
	
	static {	
		CANNON_DOWN_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = CANNON_DOWN_IMAGE.createGraphics();
		g2d.setTransform(BLOCK_TRANSFORM);
		g2d.setColor(CANNON_DOWN_COLOR);
		getCannonDownPolygon2D().draw(g2d);
		g2d.dispose();
	}
	
	public CannonDownBlock(int num, int x, int y) {
		super(DOWN, num, x, y);
	}
	
	@Override
	public BlockType getBlockType() {return BlockType.CANNON_DOWN;}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		if(super.dead_time > 0) return;
		GfxUtil.drawImage(CANNON_DOWN_IMAGE, x, y, g2d);
	}
}
