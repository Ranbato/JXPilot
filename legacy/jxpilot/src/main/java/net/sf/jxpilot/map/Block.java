package net.sf.jxpilot.map;

import java.awt.Graphics2D;

import net.sf.jgamelibrary.graphics.Renderable;

public abstract class Block implements AbstractBlock, Renderable {

	public final BlockType block_type;
	public final int num, x, y;
	
	public Block(BlockType type, int num, int x, int y) {
		this.block_type = type;
		this.num = num;
		this.x = x;
		this.y = y;
	}

	@Override
	public BlockType getBlockType() {return block_type;}

	@Override
	public int getNum() {return num;}

	@Override
	public int getX() {return x;}

	@Override
	public int getY() {return y;}
	
	@Override
	public void render(Graphics2D g2d) {render(x, y, g2d);}
}
