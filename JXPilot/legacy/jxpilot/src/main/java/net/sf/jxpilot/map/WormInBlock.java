package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class WormInBlock extends Block {
	public static final Color WARM_IN_COLOR = Color.MAGENTA;
	
	private static final BufferedImage WORM_IN_IMAGE;
	
	static {
		WORM_IN_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = WORM_IN_IMAGE.createGraphics();
		g2d.fillOval(0, 0, BLOCK_SIZE, BLOCK_SIZE);
		g2d.dispose();
	}
	
	public WormInBlock(int num, int x, int y) {
		super(BlockType.WORM_IN, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(WORM_IN_IMAGE, x, y, g2d);
	}
}
