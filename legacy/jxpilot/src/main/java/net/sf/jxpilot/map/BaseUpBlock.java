package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;

import net.sf.jgamelibrary.graphics.GfxUtil;
import net.sf.jxpilot.game.BaseType;
import net.sf.jxpilot.game.NewBase;

public class BaseUpBlock extends NewBase implements AbstractBlock {
	public static final Color BASE_UP_COLOR = Color.WHITE;
	
	public final BlockType block_type;
	
	public BaseUpBlock(BlockType block_type, int team, int num, int x, int y) {
		super(BaseType.UP, team, num, x, y);
		this.block_type = block_type;
	}

	@Override
	public BlockType getBlockType() {return block_type;}

	@Override
	public void render(int x, int y, Graphics2D g2d) {
		g2d.setColor(BASE_UP_COLOR);
		GfxUtil.drawLine(x, y, x+BLOCK_SIZE, y, g2d);
		if(player != null) {
			GfxUtil.drawCenteredStringDown(player.getName(), x+BLOCK_SIZE/2, y, g2d);
		}
	}
}
