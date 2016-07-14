package org.reflect.invoke.util;

import java.lang.reflect.*;
import java.util.*;

public class Cast {
	@FunctionalInterface
	public interface Castable<T> {

		public default boolean exclude(Class<?> type) {
			return false;
		}

		public default boolean include(Class<?> type) {
			return Generic.$(this, Castable.class, "T").isAssignableFrom(type);
		}

		public default boolean support(Class<?> type) {
			return include(type) && !exclude(type);
		}

		public T cast(Class<?> type, Object value);
	}

	public static class Custom {
		public static class Fixed extends Custom {
			@Override
			public Custom register(Castable<?> castable) {
				throw new UnsupportedOperationException("Not Support.");
			}
		}

		private final ArrayList<Castable<?>> castable = new ArrayList<>();

		public Custom() {
			for (Class<?> hier : new Hierarchy<>(getClass(), Custom.class)) {
				for (Class<?> clazz : hier.getDeclaredClasses()) {
					if (Castable.class.isAssignableFrom(clazz)) {
						try {
							castable.add((Castable<?>) found(clazz));
						} catch (Throwable e) {}
					}
				}
			}
		}

		private Object found(Class<?> clazz)
				throws InvocationTargetException, NoSuchMethodException {
			return new Invocable<>(clazz).found(null, (i, p) -> {
				return p.getType().isInstance(this) ? this : null;
			});
		}

		public <T> T cast(Class<T> type, Object value) {
			for (Castable<?> castable : castable) {
				if (castable.support(type)) {
					return type.cast(castable.cast(type, value));
				}
			}
			throw new UnsupportedOperationException();
		}

		public Custom register(Castable<?> castable) {
			this.castable.add(castable);
			return this;
		}
	}

	public static Cast $ = new Cast(new Custom.Fixed());

	@SuppressWarnings("unchecked")
	public static <R> R $(Object value) throws ClassCastException {
		return (R) value;
	}

	public static <T> T $(Class<T> type, Object value) {
		return $.cast(type, value);
	}

	public static <T> T $(Object value, T dfl) {
		return value == null ? dfl : $.cast(value, dfl);
	}

	@SafeVarargs
	public static <T> T[] array(T... value) {
		return value;
	}

	public static <T> Class<? extends T> clazz(T value)
			throws NullPointerException {
		return $(value.getClass());
	}

	public static <T> T clone(T object, T origin) throws NullPointerException {
		for (Class<?> c = origin.getClass(); c != null; c = c.getSuperclass()) {
			if (c.isInstance(object)) {
				for (Field field : Invocable.override(c.getDeclaredFields())) {
					if ((field.getModifiers() & Modifier.STATIC) == 0
							&& (field.getModifiers() & Modifier.FINAL) == 0) {
						try {
							field.set(object, field.get(origin));
						} catch (Throwable e) {}
					}
				}
			}
		}
		return object;
	}

	public final Custom custom;

	public Cast(Custom custom) {
		init(this.custom = custom, Custom.class);
		init(this, Cast.class);
	}

	public Cast() {
		this(new Custom());
	}

	protected <T> void init(T init, Class<T> ceil) {
		for (Class<?> clazz : new Hierarchy<>(clazz(init), ceil, false)) {
			for (Class<?> nested : clazz.getDeclaredClasses()) {
				if (nested.isAssignableFrom(Castable.class)
						&& (nested.getModifiers() & Modifier.PUBLIC) != 0) {
					try {
						if ((nested.getModifiers() & Modifier.STATIC) == 0) {
							custom.castable.add($(nested.newInstance()));
						} else {
							Constructor<?> c = nested.getConstructor(clazz);
							custom.castable.add($(c.newInstance(init)));
						}
					} catch (Throwable e) {}
				}
			}
		}
	}

	public <T> T[] array(Class<T> type, Collection<T> value)
			throws NullPointerException {
		return value.toArray($(Array.newInstance(type, value.size())));
	}

	public <T> T array(T value, int length)
			throws NullPointerException,
			IllegalArgumentException,
			NegativeArraySizeException {
		int min = Math.min(Array.getLength(value), length);
		Class<?> ctype = value.getClass().getComponentType();
		T temp = $(Array.newInstance(ctype, length));
		System.arraycopy(value, 0, temp, 0, min);
		return temp;
	}

	public <T> T primitive(Class<T> type, Object value)
			throws NullPointerException, ClassCastException {
		try {
			for (Class<?> wrapper : array(Byte.class, Short.class,
					Integer.class, Long.class, Float.class, Double.class)) {
				if (wrapper.getField("TYPE").get(null) == type) {
					if (value instanceof Number) {
						try {
							String n = type.getName() + "Value";
							return $(Number.class.getMethod(n).invoke(value));
						} catch (Throwable e) {}
					}
					return $(instance(wrapper, value.toString()));
				}
			}
		} catch (
				ClassCastException | NullPointerException e) {
			throw e;
		} catch (Throwable e) {}
		if (type == boolean.class) {
			return $(flag(value));
		} else if (type == char.class) {
			try {
				return $(value.toString().charAt(0));
			} catch (IndexOutOfBoundsException e) {
				throw new ClassCastException("Can't cast \"\" to a char.");
			}
		} else if (type == void.class) {
			return null;
		}
		throw new IllegalArgumentException(
				type.getName() + " is not recognize primitive type.");
	}

	public <T> T array(Class<T> type, Object value) {
		if (value == null) {
			return null;
		} else if (type == null) {
			return value.getClass().isArray()
					? $(value) : $(new Object[] { value });
		}
		Class<?> ctype = type.getComponentType();
		if (ctype == null) {
			throw new IllegalArgumentException(
					"Class " + type.getName() + " is not array.");
		} else if (value instanceof Object[] && !ctype.isPrimitive()) {
			int length = ((Object[]) value).length;
			Object array = Array.newInstance(ctype, length);
			for (int i = 0; i < length; i++) {
				try {
					((Object[]) array)[i] = cast(ctype, ((Object[]) value)[i]);
				} catch (Throwable e) {}
			}
			return $(array);
		} else if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			Object array = Array.newInstance(ctype, length);
			for (int i = 0; i < length; i++) {
				Array.set(array, i, primitive(ctype, Array.get(value, i)));
			}
			return $(array);
		} else if (value instanceof Iterable) {
			return array(type, ((Iterable<?>) value).iterator());
		} else if (value instanceof Iterator) {
			Object array = Array.newInstance(ctype, 0);
			int i = 0;
			for (Iterator<?> it = (Iterator<?>) value; it.hasNext(); i++) {
				try {
					Object cast = cast(ctype, it.next());
					try {
						Array.set(array, i, cast);
					} catch (IndexOutOfBoundsException e) {
						int l = i < 1024 ? i * 2 + 1 : i + 1024;
						Array.set(array = array(array, l), i, cast);
					}
				} catch (ClassCastException e) {
					if (ctype.isPrimitive()) {
						throw e;
					}
				}
			}
			return $(array(array, i));
		} else if (value instanceof CharSequence) {
			return array(type, value.toString().split(" *, *"));
		} else {
			Object array = Array.newInstance(ctype, 1);
			Array.set(array, 0, cast(ctype, value));
			return $(array);
		}
	}

	public <T> T chars(Class<T> type, Object value)
			throws ClassCastException {
		try {
			return type.getConstructor(String.class)
					.newInstance(value.toString());
		} catch (Throwable e) {
			if (type == null) {
				return $(value instanceof CharSequence
						? value : value.toString());
			} else if (value == null) {
				return null;
			}
			try (Formatter f = new Formatter()) {
				f.format("Can't initial \"%s\"'s instance with \"%s\".",
						type.getName(), value.toString());
				throw new ClassCastException(f.toString());
			}
		}
	}

	public Boolean flag(Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof Number) {
			return ((Number) value).doubleValue() != 0;
		} else {
			try {
				String sValue = value.toString().trim();
				if ("true".equalsIgnoreCase(sValue)) {
					return Boolean.TRUE;
				} else if ("false".equalsIgnoreCase(sValue)) {
					return Boolean.FALSE;
				}
			} catch (NullPointerException e) {}
			return null;
		}
	}

	public <T> T instance(Class<T> type, Object value)
			throws ClassCastException, NullPointerException {
		for (Method method : type.getMethods()) {
			if ((method.getModifiers() & Modifier.STATIC) != 0
					&& method.getName().equals("valueOf")
					&& method.getParameters().length == 1
					&& method.getParameters()[0].getType().isInstance(value)) {
				try {
					return type.cast(method.invoke(null, value));
				} catch (Throwable e) {}
			}
		}
		for (Constructor<?> cons : type.getConstructors()) {
			if (cons.getParameters().length == 1
					&& cons.getParameters()[0].getType().isInstance(value)) {
				try {
					return type.cast(cons.newInstance(value));
				} catch (Throwable e) {}
			}
		}
		try (Formatter f = new Formatter()) {
			f.format("Can't initial \"%s\"'s instance with \"%s\".",
					type.getName(), value);
			throw new ClassCastException(f.toString());
		}
	}

	public <T> T cast(Class<T> type, Object value) throws ClassCastException {
		try {
			if (type == null || value == null || type.isInstance(value)) {
				return $(value);
			} else if (type.isPrimitive()) {
				return primitive(type, value);
			} else if (type.isArray()) {
				return array(type, value);
			} else if (CharSequence.class.isAssignableFrom(type)) {
				return chars(type, value);
			} else if (type == Boolean.class) {
				return $(flag(value));
			} else {
				try {
					return custom.cast(type, value);
				} catch (UnsupportedOperationException e) {
					return instance(type, value);
				}
			}
		} catch (ClassCastException e) {
			throw e;
		} catch (Throwable e) {
			try (Formatter f = new Formatter()) {
				f.format("Can't cast \"%s\" to %s.", value, type.getName());
				throw new ClassCastException(f.toString());
			}
		}
	}

	public <T> T cast(Class<T> type, Object value, T dfl) {
		try {
			return cast(type, value);
		} catch (ClassCastException e) {
			return dfl;
		}
	}

	public <T> T cast(Object value, T dfl) throws NullPointerException {
		return cast(clazz(dfl), value, dfl);
	}
}