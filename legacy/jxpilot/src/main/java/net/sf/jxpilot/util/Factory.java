package net.sf.jxpilot.util;

/**
 * 
 * @author vlad Factory class.
 *
 * @param <T>
 */
public interface Factory<T> {
	/**
	 * @return A new object of type T.
	 */
	public T newInstance();
}
