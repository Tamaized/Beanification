package tamaized.beanification;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;

@ApiStatus.Internal
abstract class AbstractBeanContext {

	private Map<BeanDefinition<?>, Object> BEANS = new HashMap<>();
	private boolean frozen = false;

	Map<BeanDefinition<?>, Object> getBeans() {
		return BEANS;
	}

	public boolean isFrozen() {
		return frozen;
	}

	protected void freeze() {
		BEANS = Collections.unmodifiableMap(BEANS);
		frozen = true;
	}

	protected boolean canAccessUnfrozen() {
		return false;
	}

	protected void registerInternal(Class<?> type, @Nullable String name, Object instance) {
		if (frozen)
			throw new IllegalStateException("Bean Context already frozen");
		BeanDefinition<?> beanDefinition = new BeanDefinition<>(type, name);
		if (BEANS.containsKey(beanDefinition)) {
			final StringBuilder error = new StringBuilder("Class: ").append(type);
			if (name != null) {
				error.append(", Name: ").append(name);
			}
			throw new RuntimeException("Attempted to register a duplicate Bean." + error);
		}
		BEANS.put(beanDefinition, instance);
	}

	private void assertFrozenAccess() {
		if (!frozen && !canAccessUnfrozen())
			throw new IllegalStateException("Bean Context has not been initialized yet");
	}

	<T> T injectInternal(Class<T> type, @Nullable String name) {
		assertFrozenAccess();
		return type.cast(Objects.requireNonNull(BEANS.get(new BeanDefinition<>(type, name)), "Trying to inject Bean: " + type + (name == null ? "" : " (" + name + ")")));
	}

	<T> List<T> injectFuzzyInternal(Class<T> type) {
		assertFrozenAccess();
		return BEANS.entrySet().stream()
			.filter(entry -> type.isAssignableFrom(entry.getKey().type()))
			.map(Map.Entry::getValue)
			.map(type::cast)
			.toList();
	}

}
