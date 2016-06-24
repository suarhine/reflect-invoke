package org.reflect.invoke.util;

import java.util.function.Consumer;

public class Listener<C> {
	private static class DispatchEventException extends RuntimeException {
		/**
		 * <a href="https://www.google.co.th/search?btnI&q=serialVersionUID">
		 * serialVersionUID</a>
		 */
		private static final long serialVersionUID = 1L;
	}

	private static class Registry<T> {

		public final Class<T> type;
		private Consumer<T> listener;

		public Registry(Class<T> key) {
			this.type = key;
		}

		public void listen(Consumer<T> after) {
			listener = listener == null ? after : listener.andThen(after);
		}

		public void launch(T value) {
			if (listener != null) {
				listener.accept(value);
			}
		}
	}

	public static void despatch() {
		throw new DispatchEventException();
	}

	private final C component;
	private final Registry<?>[] registry;

	@SafeVarargs
	public Listener(C component, Class<?>... registry) {
		this.component = component;
		this.registry = Cast.$(new Registry[registry.length]);
		for (int i = 0; i < registry.length; i++) {
			this.registry[i] = new Registry<>(registry[i]);
		}
	}

	public <R extends C, T> R listen(Class<T> type, Consumer<T> listener)
			throws ClassCastException {
		for (Registry<?> registry : registry) {
			if (registry.type.isAssignableFrom(type)) {
				registry.listen(Cast.$(listener));
			}
		}
		return Cast.$(component);
	}

	public <R extends C, T> R launch(Object value) {
		try {
			for (Registry<?> registry : registry) {
				if (value == null ? registry.type == void.class
						|| registry.type == Void.class
						: registry.type.isInstance(value)) {
					registry.launch(Cast.$(value));
				}
			}
		} catch (DispatchEventException e) {}
		return Cast.$(component);
	}
}