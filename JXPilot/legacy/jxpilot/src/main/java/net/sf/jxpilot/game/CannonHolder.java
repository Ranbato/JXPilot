package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;


/**
 * Contains information given by server about a cannon.
 * Contains num which is the cannon's index in the map_data.
 * Contains dead_time which is the number of frames that the cannon will be dead for.
 * @author Vlad Firoiu
 */
public class CannonHolder implements Holder<CannonHolder>{
	/**
	 * Unsigned short.
	 */
	protected int num, dead_time;
	
	public int getNum(){return num;}
	public int getDeadTime(){return dead_time;}
	
	public CannonHolder set(int num, int dead_time) {
		this.num = num;
		this.dead_time = dead_time;
		return this;
	}
	
	public void set(CannonHolder other) {
		other.set(num, dead_time);
	}
	
	public void setFrom(Holder<CannonHolder> other) {
		other.set(this);
	}
}
