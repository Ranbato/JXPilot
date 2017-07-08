package net.sf.jxpilot.map;

import java.awt.Graphics2D;

public class SpaceBlock extends Block {

	public SpaceBlock(int num, int x, int y) {
		super(BlockType.SPACE, num, x, y);
	}

	@Override
	public void render(int x, int y, Graphics2D g2d){}
}
