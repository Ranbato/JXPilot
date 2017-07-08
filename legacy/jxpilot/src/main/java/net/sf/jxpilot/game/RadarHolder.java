package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds radar data.
 * @author Vlad Firoiu
 */
public class RadarHolder implements Holder<RadarHolder> {
	
	/**
	 * Location of detection.
	 */
	protected short x, y,
	/**
	 * Distance from us.
	 */
	size;
	
	public RadarHolder setRadar(short x, short y, short size) {
		this.x = x;
		this.y = y;
		this.size = size;
		return this;
	}
	
	@Override
	public void set(RadarHolder other) {
		other.setRadar(x, y, size);
	}
	
	@Override
	public void setFrom(Holder<RadarHolder> other) {
		other.set(this);
	}
	
	public short getX(){return x;}
	public short getY(){return y;}
	public short getSize(){return size;}
}
