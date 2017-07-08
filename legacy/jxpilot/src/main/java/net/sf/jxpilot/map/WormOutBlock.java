package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class WormOutBlock extends Block {
	public static final Color WARM_OUT_COLOR = Color.MAGENTA;
	
	private static final BufferedImage WORM_OUT_IMAGE;
	
	static {
		WORM_OUT_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
		Graphics2D g2d = WORM_OUT_IMAGE.createGraphics();
		g2d.drawOval(0, 0, BLOCK_SIZE, BLOCK_SIZE);
		g2d.dispose();
	}
	
	public WormOutBlock(int num, int x, int y) {
		super(BlockType.WORM_OUT, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(WORM_OUT_IMAGE, x, y, g2d);
	}
}
