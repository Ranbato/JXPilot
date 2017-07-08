package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

public class BallHolder implements Holder<BallHolder>
{
	protected short x,y;
	protected short id;
	
	public BallHolder setBall(short x, short y, short id)
	{
		this.x = x;
		this.y = y;
		this.id = id;
		return this;
	}

	public void set(BallHolder other)
	{
		other.setBall(x, y, id);
	}
	
	public void setFrom(Holder<BallHolder> other)
	{
		other.set(this);
	}
	
	public short getX(){return x;}
	public short getY(){return y;}
	public short getId(){return id;}
}
