package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds Seek data.
 * @author Vlad Firoiu
 */
public class SeekHolder implements Holder<SeekHolder> {
	protected short programmer_id, robot_id, sought_id;
	
	public short getProgrammerId(){return this.programmer_id;}
	public short getRobotId(){return this.robot_id;}
	public short getSoughtId(){return this.sought_id;}
	
	public SeekHolder setSeekHolder(short programmer_id, short robot_id, short sought_id) {
		this.programmer_id = programmer_id;
		this.robot_id = robot_id;
		this.sought_id = sought_id;
		return this;
	}
	
	@Override
	public void set(SeekHolder other) {
		other.setSeekHolder(programmer_id, robot_id, sought_id);
	}

	@Override
	public void setFrom(Holder<SeekHolder> other) {
		other.set(this);
	}
	
}
