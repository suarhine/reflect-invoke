package org.reflect.invoke.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

public class Generic {

	public static <R> Class<R> raw(Type type) {
		if (type instanceof Class) {
			return Cast.$(type);
		} else if (type instanceof ParameterizedType) {
			return raw(((ParameterizedType) type).getRawType());
		} else {
			throw new UnsupportedOperationException("Not support for " + type);
		}
	}

	public static <T, R> Class<R> $(
			Class<T> base, Class<? super T> target, String var)
			throws NullPointerException {
		return new Generic(base).find(target, var);
	}

	public static <T, R> Class<R> $(
			T base, Class<? super T> target, String var)
			throws NullPointerException {
		return $(Cast.clazz(base), target, var);
	}

	/**
	 * Field สำหรับเก็บ {@link Type} ของ {@link TypeVariable} ทั้งหมดที่ค้นพบ
	 */
	private final Map<TypeVariable<?>, Type> variable = new HashMap<>();

	public Generic(Type type) {
		initial(type);
	}

	public Generic(Class<?> type) throws NullPointerException {
		initial(type);
	}

	private void initial(Class<?> type) throws NullPointerException {
		initial(type.getGenericSuperclass());
		for (Type iface : type.getGenericInterfaces()) {
			initial(iface);
		}
	}

	private void initial(ParameterizedType type)
			throws NullPointerException, UnsupportedOperationException {
		Class<?> raw = (Class<?>) type.getRawType();
		Type[] act = type.getActualTypeArguments();
		TypeVariable<?>[] var = raw.getTypeParameters();
		for (int i = 0; i < act.length; i++) {
			if (act[i] instanceof TypeVariable<?>) {
				variable.put(var[i], variable.get(act[i]));
			} else {
				variable.put(var[i], act[i]);
			}
		}
		initial(raw);
	}

	private void initial(Type type) {
		if (type instanceof Class) {
			initial((Class<?>) type);
		} else if (type instanceof ParameterizedType) {
			initial((ParameterizedType) type);
		} else if (type != null) {
			throw new UnsupportedOperationException("Not support for " + type);
		}
	}

	public Type[] actual(Class<?> clazz) throws NullPointerException {
		TypeVariable<?>[] var = clazz.getTypeParameters();
		Type[] act = new Class<?>[var.length];
		for (int i = 0; i < act.length; i++) {
			act[i] = variable.get(var[i]);
		}
		return act;
	}

	public Type actual(Class<?> clazz, String var) throws NullPointerException {
		for (TypeVariable<?> variable : clazz.getTypeParameters()) {
			if (variable.getName().equals(var)) {
				return this.variable.get(variable);
			}
		}
		return null;
	}

	public Class<?>[] find(Class<?> clazz) throws NullPointerException {
		TypeVariable<?>[] var = clazz.getTypeParameters();
		Class<?>[] act = new Class<?>[var.length];
		for (int i = 0; i < act.length; i++) {
			act[i] = raw(variable.get(var[i]));
		}
		return act;
	}

	public <R> Class<R> find(Class<?> clazz, String var)
			throws NullPointerException {
		for (TypeVariable<?> variable : clazz.getTypeParameters()) {
			if (variable.getName().equals(var)) {
				return raw(this.variable.get(variable));
			}
		}
		return null;
	}
}
