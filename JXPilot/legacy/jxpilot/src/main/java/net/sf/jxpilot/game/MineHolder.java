package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

public class MineHolder implements Holder<MineHolder>{

	/**
	 * Assumed that no player has this id.
	 */
	public static final int EXPIRED_MINE_ID = 4096;
	
	protected short x,y,id;
	protected byte team_mine;

	public MineHolder setMine(short x, short y, byte team_mine, short id)
	{
		this.x = x;
		this.y = y;
		this.team_mine = team_mine;
		this.id = id;
		return this;
	}

	public void set(MineHolder other)
	{
		other.setMine( x,  y,  team_mine,  id);
	}
	
	public void setFrom(Holder<MineHolder> other)
	{
		other.set(this);
	}
	
	public short getX(){return x;}
	public short getY(){return y;}
	public byte getTeamMine(){return team_mine;}
	public short getId(){return id;}
}
