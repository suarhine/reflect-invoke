package org.reflect.invoke.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Invocable<T> {

	/**
	 * Method override
	 * 
	 * @param o
	 * @return
	 */
	@SafeVarargs
	public static <A extends AccessibleObject> A[] override(A... o) {
		try {
			AccessibleObject.setAccessible(o, true);
		} catch (Throwable e) {}
		return o;
	}

	/**
	 * Method override
	 * 
	 * @param o
	 * @return
	 */
	public static <A extends AccessibleObject> A override(A o) {
		try {
			o.setAccessible(true);
		} catch (Throwable e) {}
		return o;
	}

	private final Class<? extends T> init;
	private final Class<T> ceil;
	private final boolean touch;

	public Invocable(Class<? extends T> init, Class<T> ceil, boolean touch) {
		this.init = init;
		this.ceil = ceil;
		this.touch = touch;
	}

	public Invocable(Class<T> clazz) {
		this(clazz, clazz, true);
	}

	public <R> R get(T target, Function<Field, Boolean> filter)
			throws NullPointerException,
			IllegalArgumentException,
			ClassCastException,
			NoSuchFieldException {
		for (Class<? extends T> clazz : new Hierarchy<>(init, ceil, touch)) {
			for (Field field : clazz.getDeclaredFields()) {
				if (filter == null || filter.apply(field)) {
					try {
						return Cast.$(override(field).get(target));
					} catch (IllegalAccessException e) {}
				}
			}
		}
		throw new NoSuchFieldException(filter + " not found.");
	}

	public void set(T target,
			Function<Field, Boolean> filter,
			Function<Field, Object> arguments)
			throws NullPointerException,
			IllegalArgumentException,
			ClassCastException,
			NoSuchFieldException {
		for (Class<? extends T> clazz : new Hierarchy<>(init, ceil, touch)) {
			for (Field field : clazz.getDeclaredFields()) {
				if (filter == null || filter.apply(field)) {
					try {
						override(field).set(target,
								field.getType().cast(arguments.apply(field)));
					} catch (IllegalAccessException e) {}
				}
			}
		}
		throw new NoSuchFieldException(filter + " not found.");
	}

	public <R> R call(T target,
			Function<Method, Boolean> filter,
			BiFunction<Integer, Parameter, Object> arguments)
			throws NullPointerException,
			IllegalArgumentException,
			ClassCastException,
			InvocationTargetException,
			NoSuchMethodException {
		for (Class<? extends T> clazz : new Hierarchy<>(init, ceil, touch)) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (filter == null || filter.apply(method)) {
					try {
						Parameter[] p = method.getParameters();
						Object[] a = new Object[p.length];
						for (int i = 0; i < a.length; i++) {
							a[i] = arguments.apply(i, p[i]);
						}
						return Cast.$(override(method).invoke(target, a));
					} catch (IllegalAccessException e) {}
				}
			}
		}
		throw new NoSuchMethodException(filter + " not found.");
	}

	public T found(Function<Constructor<?>, Boolean> filter,
			BiFunction<Integer, Parameter, Object> arguments)
			throws NullPointerException,
			IllegalArgumentException,
			ClassCastException,
			InvocationTargetException,
			NoSuchMethodException {
		for (Constructor<?> constructor : init.getDeclaredConstructors()) {
			if (filter == null || filter.apply(constructor)) {
				try {
					Parameter[] p = constructor.getParameters();
					Object[] a = new Object[p.length];
					for (int i = 0; i < a.length; i++) {
						a[i] = arguments.apply(i, p[i]);
					}
					return Cast.$(override(constructor).newInstance(a));
				} catch (RuntimeException e) {
					throw e;
				} catch (Throwable e) {}
			}
		}
		throw new NoSuchMethodException(filter + " not found.");
	}
}
