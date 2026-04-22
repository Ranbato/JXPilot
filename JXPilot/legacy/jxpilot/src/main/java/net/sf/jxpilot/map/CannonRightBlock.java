package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;
import static net.sf.jxpilot.game.CannonType.RIGHT;
import net.sf.jxpilot.game.NewCannon;

public class CannonRightBlock extends NewCannon implements AbstractBlock {
	public static final Color CANNON_RIGHT_COLOR = Color.WHITE;
	
	public static final Polygon2DAdaptor getCannonRightPolygon2D() {return RIGHT.getPolygon2D();}
	
	private static final BufferedImage CANNON_RIGHT_IMAGE;
	
	static {	
		CANNON_RIGHT_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = CANNON_RIGHT_IMAGE.createGraphics();
		g2d.setTransform(BLOCK_TRANSFORM);
		g2d.setColor(CANNON_RIGHT_COLOR);
		getCannonRightPolygon2D().draw(g2d);
		g2d.dispose();
	}
	
	public CannonRightBlock(int num, int x, int y) {
		super(RIGHT, num, x, y);
	}
	
	@Override
	public BlockType getBlockType() {return BlockType.CANNON_RIGHT;}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		if(super.dead_time > 0) return;
		GfxUtil.drawImage(CANNON_RIGHT_IMAGE, x, y, g2d);
	}
}
