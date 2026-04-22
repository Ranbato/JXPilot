package net.sf.jxpilot.graphics;

import java.awt.Graphics2D;

import net.sf.jxpilot.game.*;
import static net.sf.jxpilot.map.MapBlock.BLOCK_SIZE;

public class WorldRenderer extends GameRenderer{
	private final GameWorld world;
	
	public WorldRenderer(GameWorld world)
	{
		super(world.getMap(), world.getDrawables());
		this.world = world;
	}
	
	public void renderGame(Graphics2D screenG2D, double centerX, double centerY, int viewWidth, int viewHeight, double scale)
	{
		super.renderGame(screenG2D, (double)world.getSelfX()/BLOCK_SIZE, (double)world.getSelfY()/BLOCK_SIZE, 
				centerX, centerY, viewWidth, viewHeight, scale);
	}
}
