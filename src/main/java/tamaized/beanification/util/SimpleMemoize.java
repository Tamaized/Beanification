package tamaized.beanification.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class SimpleMemoize {

	private SimpleMemoize() {

	}

	public static <T> Supplier<T> memoize(Supplier<T> t) {
		return new MemoizedSupplier<>(t);
	}

	private static class MemoizedSupplier<T> implements Supplier<T> {

		@Nullable
		private Supplier<T> delegate;

		private final AtomicReference<T> resolved = new AtomicReference<>();

		MemoizedSupplier(Supplier<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public T get() {
			T result = resolved.get();
			if (result == null) {
				synchronized(resolved) {
					result = resolved.get();
					if (result == null) {
						result = Objects.requireNonNull(Objects.requireNonNull(delegate).get());
						resolved.set(result);
						delegate = null;
					}
				}
			}
			return result;
		}
	}

}
