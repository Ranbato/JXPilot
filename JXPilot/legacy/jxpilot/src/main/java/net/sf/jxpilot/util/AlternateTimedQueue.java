package net.sf.jxpilot.util;

import java.util.*;

/**
 * Alternate implementation of TimedQueue that is actually a Queue (a Deque really).
 * Only add() and remove() methods should be used, since others are not time-supported.
 * @author vlad
 *
 * @param <T>
 */
public class AlternateTimedQueue<T> extends ArrayDeque<T>
{
	/**
	 * Holds times that elements were added.
	 */
	private ArrayDeque<Long> times = new ArrayDeque<Long>();
	
	/**
	 * How long objects should be held.
	 */
	private long holdTime;
	
	public AlternateTimedQueue(long holdTime)
	{
		this.holdTime = holdTime;
	}
	
	@Override
	public boolean add(T entry)
	{
		times.add(System.currentTimeMillis());
		super.add(entry);
		return true;
	}
	
	public void clear()
	{
		super.clear();
		times.clear();
	}
	
	public void update()
	{
		long currentTime = System.currentTimeMillis();
		
		while(super.size()>0 && currentTime-times.peek()>holdTime)
		{
			remove();
		}
	}
	
	public T remove()
	{	
		T temp = super.remove();
		times.remove();
		
		return temp;
	}
}