package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds Asteroid data.
 * @author Vlad Firoiu
 * @since xpilot version 4.4.0
 */
public class AsteroidHolder implements Holder<AsteroidHolder> {
	protected short x, y;
	protected byte type_size, type, size, rot;

	public short getX(){return x;}
	public short getY(){return y;}
	public byte getTypeSize(){return type_size;}
	public byte getType(){return type;}
	public byte getSize(){return size;}
	public byte getRot(){return rot;}
	
	public AsteroidHolder setAsteroid(short x, short y, byte type_size, byte type, byte size, byte rot) {
		this.x = x;
		this.y = y;
		this.type_size = type_size;
		this.type = type;
		this.size = size;
		this.rot = rot;
		return this;
	}
	
	@Override
	public void set(AsteroidHolder other) {
		other.setAsteroid(x, y, type_size, type, size, rot);
	}
	@Override
	public void setFrom(Holder<AsteroidHolder> other) {
		other.set(this);
	}
	
}
