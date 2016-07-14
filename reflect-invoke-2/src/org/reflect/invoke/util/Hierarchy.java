package org.reflect.invoke.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Hierarchy<T> implements Iterable<Class<? extends T>> {

	private class It implements Iterator<Class<? extends T>> {
		private Class<? extends T> cursor = init;

		@Override
		public boolean hasNext() {
			return cursor != null && (touch || cursor != ceil)
					&& (ceil == null || ceil.isAssignableFrom(cursor));
		}

		@Override
		public Class<? extends T> next() {
			try {
				if ((ceil == null || ceil.isAssignableFrom(cursor))
						&& (touch || ceil != cursor)) {
					try {
						return cursor;
					} finally {
						cursor = Cast.$(cursor.getSuperclass());
					}
				}
			} catch (NullPointerException e) {}
			throw new NoSuchElementException();
		}
	}

	private final Class<? extends T> init;
	private final Class<T> ceil;
	private final boolean touch;

	public Hierarchy(Class<? extends T> init, Class<T> ceil, boolean touch)
			throws NullPointerException, IllegalArgumentException {
		if ((this.ceil = ceil) == null) {
			if ((this.init = init) == null) {
				throw new NullPointerException();
			}
		} else if (!ceil.isAssignableFrom(this.init = init)) {
			throw new IllegalArgumentException();
		}
		this.touch = touch;
	}

	public Hierarchy(Class<? extends T> init, Class<T> ceil)
			throws NullPointerException {
		this(init, ceil, false);
	}

	public Hierarchy(Class<? extends T> clazz) throws NullPointerException {
		this(clazz, null, false);
	}

	@Override
	public Iterator<Class<? extends T>> iterator() {
		return new It();
	}
}