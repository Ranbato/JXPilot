package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Carries information server sends for bases.
 * @author Vlad Firoiu
 */
public class BaseHolder implements Holder<BaseHolder> {
	/**
	 * Id of player that has taken base.
	 */
	protected short id = Player.NO_ID;
	/**
	 * Base number, as appears in map data.
	 */
	protected int num;
	
	public BaseHolder setBase(short id, int num) {
		this.id = id;
		this.num = num;		
		return this;
	}
	
	public void set(BaseHolder other) {
		other.setBase(id, num);
	}
	
	public void setFrom(Holder<BaseHolder> other) {
		other.set(this);
	}
	
	public short getId(){return id;}
	public int getNum(){return num;}
}
