package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;

import net.sf.jgamelibrary.graphics.GfxUtil;
import net.sf.jxpilot.game.BaseType;
import net.sf.jxpilot.game.NewBase;

public class BaseLeftBlock extends NewBase implements AbstractBlock {
	public static final Color BASE_LEFT_COLOR = Color.WHITE;
	
	public final BlockType block_type;
	
	public BaseLeftBlock(BlockType block_type, int team, int num, int x, int y) {
		super(BaseType.UP, team, num, x, y);
		this.block_type = block_type;
	}

	@Override
	public BlockType getBlockType() {return block_type;}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		g2d.setColor(BASE_LEFT_COLOR);
		GfxUtil.drawLine(x+BLOCK_SIZE, y, x+BLOCK_SIZE, y+BLOCK_SIZE, g2d);
		if(player != null) {
			GfxUtil.drawCenteredStringRight(player.getName(), x+BLOCK_SIZE, y+BLOCK_SIZE/2, g2d);
		}
	}
}
