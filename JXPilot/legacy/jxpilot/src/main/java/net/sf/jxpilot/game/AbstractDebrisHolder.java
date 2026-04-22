package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds data for debris types.
 * @author Vlad Firoiu
 */
public class AbstractDebrisHolder implements Holder<AbstractDebrisHolder> {
	protected int type;
	/**
	 * Unsigned byte.
	 */
	protected short x,y;
	
	public AbstractDebrisHolder setAbstractDebris(int type, short x, short y) {
		this.type = type;
		this.x = x;
		this.y = y;
		return this;
	}
	
	@Override
	public void set(AbstractDebrisHolder other) {
		other.setAbstractDebris(type, x, y);
	}
	
	@Override
	public void setFrom(Holder<AbstractDebrisHolder> other) {
		other.set(this);
	}
	
	//these methods should be overridden to return the real x and y values.
	public int getX(){return x;}
	public int getY(){return y;}
}
