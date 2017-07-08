package net.sf.jxpilot.game;

import static net.sf.jxpilot.map.MapBlock.BLOCK_SIZE;
import java.awt.*;

import net.sf.jxpilot.graphics.Drawable;

public class FuelStation extends FuelHolder implements Drawable
{
	
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
	
	private final int x, y;
	private Rectangle fuelShape;
	
	public FuelStation(int num, int x, int y)
	{
		super.num = num;
		super.fuel = MAX_STATION_FUEL;
		this.x=x;
		this.y = y;
		fuelShape = new Rectangle();
	}
	
	public int getX(){return x;}
	public int getY(){return y;}
	
	/**
	 * ratio of fuel left to max fuel.
	 */
	private double fuel_ratio;
	private void setFuelRatio()
	{
		fuel_ratio = (double)super.fuel/MAX_STATION_FUEL;
	}
	
	/**
	 * Sets internal fuel rectangle to reflect fuel amount.
	 */
	private void setRect()
	{
		setFuelRatio();
		fuelShape.setLocation(x*BLOCK_SIZE, y*BLOCK_SIZE);
		fuelShape.setSize(BLOCK_SIZE, (int)(fuel_ratio*BLOCK_SIZE));
	}
	
	@Override
	public void paintDrawable(Graphics2D g2d)
	{
		setRect();
		g2d.setColor(FUEL_COLOR);
		g2d.fill(fuelShape);
	}
}