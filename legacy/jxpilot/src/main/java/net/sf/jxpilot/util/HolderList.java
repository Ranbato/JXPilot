package net.sf.jxpilot.util;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.RandomAccess;

/**
 * Holds a list of {@code Holder<? super E>} objects to store data
 * without constantly creating new objects, thus avoiding costly 
 * garbage collection.
 * 
 * @author Vlad Firoiu
 *
 * @param <E> The element type.
 */
public class HolderList<E extends Holder<? super E>> extends AbstractList<E> implements RandomAccess {
	/**
	 * The default number of elements in a {@code HolderList}.
	 */
	public static final int DEFAULT_SIZE = 10;
	
	/**
	 * Default serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Used to generate new holders.
	 */
	private Factory<? extends E> factory;
	
	/**
	 * Actual storage of holder objects.
	 */
	private ArrayList<E> list;
	
	/**
	 * Actual number of elements in use.
	 */
	private int size = 0;
	
	/**
	 * Creates a new {@code HolderList} with the specified holder factory
	 * and initial size.
	 * @param factory The desired holder factory.
	 * @param start_size The initial size.
	 */
	public HolderList(Factory<? extends E> factory, int start_size) {
		list = new ArrayList<E>(start_size);
		this.factory = factory;
		for(int i = 0;i<start_size;i++) {
			list.add(factory.newInstance());
		}
	}
	
	/**
	 * Creates a new {@code HolderList} with the specified holder factory
	 * and the default initial size.
	 * @param factory The desired holder factory.
	 */
	public HolderList(Factory<? extends E> factory) {
		this(factory, DEFAULT_SIZE);
	}
	
	/**
	 * Returns the actual number of elements used in this {@code HolderList}.
	 */
	@Override
	public int size() {
		return size;
	}
	
	/**
	 * This method sets the size to zero, but does not actually clear any data.
	 */
	@Override
	public void clear() {
		size = 0;
	}
	
	/**
	 * Stores the specified data into the list.
	 * @param holder Containing the data to store.
	 * @return The holder storing the data.
	 */
	public E add(Holder<? super E> holder) {
		if(size==list.size()) {
			list.add(factory.newInstance());
			//System.out.println("Increased HolderList size to " + (size+1));
		}
		E temp = list.get(size++);
		holder.set(temp);
		//temp.setFrom(holder);
		return temp;
	}
	
	@Override
	public boolean add(E element) {
		this.add((Holder<? super E>)element);
		return true;
	}

	/**
	 * Efficient bulk add operation for random access lists.
	 * @param <T> The list type.
	 * @param l The list.
	 * @return True.
	 */
	public <T extends List<? extends E> & RandomAccess> boolean addAll(T l) {
		int num = this.size() + l.size();
		if(num>list.size()) {
			list.ensureCapacity(num);
			for(int i = list.size();i<num;i++) {
				list.add(factory.newInstance());
			}
		}
		
		for(int i = 0;i<l.size();i++) {
			l.get(i).set(list.get(size++));
		}
		
		return true;
	}

	
	@Override
	public boolean addAll(Collection<? extends E> c) {
		int num = this.size() + c.size();
		if(num>list.size()) {
			list.ensureCapacity(num);
			for(int i = list.size();i<num;i++) {
				list.add(factory.newInstance());
			}
		}
		for(E e : c) {
			e.set(list.get(size++));
		}
		return true;
	}

	@Override
	public E get(int index) {
		return list.get(index);
	}

	@Override
	public E set(int index, E element) {
		E temp = list.get(index);
		element.set(temp);
		return temp;
	}
}
