package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.sf.jgamelibrary.graphics.Accelerator;
import net.sf.jgamelibrary.graphics.GfxUtil;

public class TreasureBlock extends Block {
	public static final Color TREASURE_COLOR = Color.RED;

	private static final BufferedImage TREASURE_IMAGE = Accelerator.createCompatibleImage(BLOCK_SIZE, BLOCK_SIZE);
	
	static {
		Graphics2D g2d = TREASURE_IMAGE.createGraphics();
		g2d.setColor(TREASURE_COLOR);
		g2d.drawOval(0, 0, BLOCK_SIZE, BLOCK_SIZE);
		g2d.dispose();
	}
	
	public final int team;
	
	public TreasureBlock(BlockType type, int team, int num, int x, int y) {
		super(type, num, x, y);
		this.team = team;
	}

	public TreasureBlock(BlockType type, int num, int x, int y) {
		this(type, type.ordinal()-BlockType.TREASURE_0.ordinal(), num, x, y);
	}
	
	public TreasureBlock(int team, int num, int x, int y) {
		this(BlockType.getBlockType((byte)(BlockType.TREASURE_0.ordinal() + team)), team, num, x, y);
	}
	
	public int getTeam() {return team;}
	
	@Override
	public void render(int x, int y, Graphics2D g2d) {
		GfxUtil.drawImage(TREASURE_IMAGE, x, y, g2d);
	}

}
