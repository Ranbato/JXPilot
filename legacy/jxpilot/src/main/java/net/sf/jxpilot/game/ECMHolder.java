package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds ECM data.
 * @author Vlad Firoiu
 */
public class ECMHolder implements Holder<ECMHolder> {

	protected short x, y, size;
	
	public short getX(){return x;}
	public short getY(){return y;}
	public short getSize(){return size;}
	
	public ECMHolder setECM(short x, short y, short size) {
		this.x = x;
		this.y = y;
		this.size = size;
		return this;
	}
	
	@Override
	public void set(ECMHolder other) {
		other.setECM(x, y, size);
	}

	@Override
	public void setFrom(Holder<ECMHolder> other) {
		other.set(this);
	}

}
