package net.sf.jxpilot.util;

/**
 * Holds data for a certain type of class.
 * @author Vlad Firoiu
 *
 * @param <T> The subclass type.
 */
public interface Holder<T extends Holder<T>> {
	/**Sets another object to have the same information as this Holder.
	 * @param other The other object.
	 */
	public void set(T other);
	
	/**
	 * Sets this Holder to have the same data as other.
	 * @param other
	 */
	//public void setFrom(T other);
	
	public void setFrom(Holder<T> other);
}
