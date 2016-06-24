package org.reflect.invoke;

import java.lang.reflect.InvocationTargetException;

import org.reflect.invoke.util.Cast;
import org.reflect.invoke.util.Invocable;

public class Invoker<T> {
	private final T target;
	private final Invocable<T> invocable;

	public Invoker(T target, Class<T> ceil, boolean touch) {
		this.invocable = new Invocable<>(
				Cast.clazz(this.target = target), ceil, touch);
	}

	public Invoker(T target, boolean overall) {
		this(target, overall ? null : Cast.$(target.getClass()), true);
	}

	public Invoker(T target) {
		this(target, true);
	}

	public <R> R get(String name)
			throws IllegalArgumentException,
			ClassCastException,
			NoSuchFieldException {
		return invocable.get(target, field -> field.getName().equals(name));
	}

	public void set(String name, Object argument)
			throws IllegalArgumentException,
			ClassCastException,
			NoSuchFieldException {
		invocable.set(target, field -> field.getName().equals(name),
				field -> Cast.$(field.getType(), argument));
	}

	public <R> R call(String name, Object... arguments)
			throws IllegalArgumentException,
			ClassCastException,
			InvocationTargetException,
			NoSuchMethodException {
		return invocable.call(target,
				method -> method.getName().equals(name) && arguments == null
						? method.getParameters().length == 0
						: method.getParameters().length == arguments.length,
				parameters -> {
					if (parameters.length == 0) {
						return new Object[0];
					}
					Object[] rtn = new Object[arguments.length];
					for (int i = 0; i < arguments.length; i++) {
						rtn[i] = Cast.$(parameters[i].getType(), arguments[i]);
					}
					return rtn;
				});
	}
}
