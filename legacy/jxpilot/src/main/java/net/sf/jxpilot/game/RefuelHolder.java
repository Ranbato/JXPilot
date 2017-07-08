package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds refuel data.
 * @author Vlad Firoiu
 */
public class RefuelHolder implements Holder<RefuelHolder> {
	protected short x0, y0, x1, y1;
	
	public short getX0(){return x0;}
	public short getY0(){return y0;}
	public short getX1(){return x1;}
	public short getY1(){return y1;}
	
	public RefuelHolder setRefuel(short x0, short y0, short x1, short y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		return this;
	}
	
	@Override
	public void set(RefuelHolder other) {
		other.setRefuel(x0, y0, x1, y1);
	}

	@Override
	public void setFrom(Holder<RefuelHolder> other) {
		other.set(this);
	}

}
