package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;
import net.sf.jxpilot.util.Utilities;

public class ScoreObjectHolder implements Holder<ScoreObjectHolder>
{
	protected short score;
	/**
	 * Unsigned shorts.
	 */
	protected int x,y;
	protected String message;
	
	/**
	 * Note that null chars are removed from end of message.
	 * @param score
	 * @param x
	 * @param y
	 * @param message
	 * @return This ScoreObjectHolder.
	 */
	public ScoreObjectHolder setScoreObject(short score, int x, int y, String message)
	{
		this.score = score;
		this.x = x;
		this.y = y;
		this.message= Utilities.removeNullCharacter(message);
		return this;
	}
	
	@Override
	public void set(ScoreObjectHolder other)
	{
		other.setScoreObject(score, x, y, message);
	}
	
	public void setFrom(Holder<ScoreObjectHolder> other)
	{
		other.set(this);
	}
	
	public short getScore(){return score;}
	public int getX(){return x;}
	public int getY(){return y;}
	public String getMessage(){return message;};
}
