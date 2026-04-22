package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds Laser data.
 * @author Vlad Firoiu
 */
public class LaserHolder implements Holder<LaserHolder> {
	
	protected byte color;
	protected short x, y, len;
	protected byte dir;
	
	public byte getColor(){return color;}
	public short getX(){return x;}
	public short getY(){return y;}
	public short getLen(){return len;}
	public byte getDir(){return dir;}
	
	public LaserHolder setLaser(byte color, short x, short y, short len, byte dir) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.len = len;
		this.dir = dir;
		return this;
	}
	
	@Override
	public void set(LaserHolder other) {
		other.setLaser(color, x, y, len, dir);
	}

	@Override
	public void setFrom(Holder<LaserHolder> other) {
		other.set(this);
	}

}
