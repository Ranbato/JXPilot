package net.sf.jxpilot.util;

import java.util.*;

/**
 * Class that holds objects for a certain amount of time. Note that this class
 * is actually merely an Iterable, and not actually a Queue or even a Collection.
 * @author vlad
 *
 * @param <T>
 */
public class TimedQueue<T> implements Iterable<T>{
	
	private ArrayDeque<TimedEntry> queue;
	
	/**
	 * How long objects should be held, in milliseconds.
	 */
	private long holdTime;
	
	/**
	 * @param holdTime How long the objects should be held, in milliseconds.
	 */
	public TimedQueue(long holdTime)
	{
		this.holdTime = holdTime;
		queue = new ArrayDeque<TimedEntry>();
	}
	
	public boolean add(T entry)
	{
		return queue.add(new TimedEntry(entry));
	}
	
	public void clear()
	{
		queue.clear();
	}
	
	/**
	 * Removes old objects from list.
	 */
	public void update()
	{
		if (queue.size()==0) return;
		
		long currentTime = System.currentTimeMillis();
		
		while(queue.size()>0)
		{
			if(currentTime-queue.peekFirst().creationTime>holdTime)
			{
				queue.removeFirst();
			}
			else
			{
				break;
			}
		}
	}
	
	/**
	 * Class to hold entries along with a time.
	 * @author vlad
	 *
	 * @param <T>
	 */
	private class TimedEntry
	{
		protected final long creationTime;
		protected final T content;
		
		public TimedEntry(T content)
		{
			creationTime = System.currentTimeMillis();
			this.content = content;
		}
	}
	
	private class EntryIterator implements Iterator<T>
	{
		private Iterator<TimedEntry> iterator = queue.iterator();
		
		public boolean hasNext()
		{
			return iterator.hasNext();
		}
		
		public T next()
		{
			return iterator.next().content;
		}
		
		public void remove()
		{
			iterator.remove();
		}
	}
	
	public Iterator<T> iterator()
	{
		return new EntryIterator();
	}
}
