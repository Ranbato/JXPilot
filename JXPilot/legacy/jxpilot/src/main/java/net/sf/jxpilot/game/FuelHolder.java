package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds Fuel data.
 * @author Vlad Firoiu
 */
public class FuelHolder implements Holder<FuelHolder> {
	/**
	 * Unsigned short.
	 */
	protected int num, fuel;
	
	public int getNum(){return num;}
	public int getFuel(){return fuel;}
	
	public FuelHolder setFuel(int num, int fuel) {
		this.num = num;
		this.fuel = fuel;
		return this;
	}
	
	@Override
	public void set(FuelHolder other) {
		other.setFuel(num, fuel);
	}
	@Override
	public void setFrom(Holder<FuelHolder> other) {
		other.set(this);
	}
}
