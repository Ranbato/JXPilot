package net.sf.jxpilot.map;

import java.awt.Color;
import java.awt.Graphics2D;

import net.sf.jgamelibrary.graphics.GfxUtil;

import net.sf.jxpilot.game.FuelHolder;

public class FuelBlock extends FuelHolder implements AbstractBlock {
	/*
	#define FUEL_SCALE_BITS         8
	#define FUEL_SCALE_FACT         (1<<FUEL_SCALE_BITS)
	#define FUEL_MASS(f)            ((f)*0.005/FUEL_SCALE_FACT)
	#define MAX_STATION_FUEL	(500<<FUEL_SCALE_BITS)
	#define START_STATION_FUEL	(20<<FUEL_SCALE_BITS)
	#define STATION_REGENERATION	(0.06*FUEL_SCALE_FACT)
	#define MAX_PLAYER_FUEL		(2600<<FUEL_SCALE_BITS)
	#define MIN_PLAYER_FUEL		(350<<FUEL_SCALE_BITS)
	#define REFUEL_RATE		(5<<FUEL_SCALE_BITS)
	#define ENERGY_PACK_FUEL        ((500+(randomMT()&511))<<FUEL_SCALE_BITS)
	*/
	
	public static final int FUEL_SCALE_BITS = 8;
	
	/**
	 * Maximum fuel in a FuelStation.
	 */
	public static final int MAX_STATION_FUEL = 500;
	public static final int START_STATION_FUEL = 20;
	
	public static final Color FUEL_COLOR = Color.RED;

	@Override
	public BlockType getBlockType() {return BlockType.FUEL;}

	public final int x, y;
	
	public FuelBlock(int num, int x, int y) {
		this.num = num;
		this.x = x;
		this.y = y;
		super.fuel = MAX_STATION_FUEL;
	}
	
	@Override
	public int getX() {return x;}

	@Override
	public int getY() {return y;}

	@Override
	public void render(int x, int y, Graphics2D g) {
		g.setColor(FUEL_COLOR);
		GfxUtil.fillRect(x, y, BLOCK_SIZE, (BLOCK_SIZE * super.fuel)/MAX_STATION_FUEL, g);
		//GfxUtil.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE, g);
	}
}
