package de.tub.tlabs.qu.mpi.util;

import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class Pool<T> {
	public Stack<T> released; // TODO make clean
	public Set<T> acquired;
	
	public Pool(Collection<T> instances) {
		released = new Stack<T>();
		acquired = new HashSet<T>();
		
		for (T instance : instances) {
			released.push(instance);
		}
	}
	
	public synchronized T acquire() {
		try {
			T instance = released.pop();
			acquired.add(instance);
			return instance;
		} catch (EmptyStackException ese) {
			return null;
		}
	}
	
	public synchronized void release(T instance) {
		if (instance != null) {
			acquired.remove(instance);
			released.push(instance);
		}
	}
}
