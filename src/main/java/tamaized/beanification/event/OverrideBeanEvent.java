package tamaized.beanification.event;

import net.neoforged.bus.api.Event;
import tamaized.beanification.BeanContext;

import javax.annotation.Nullable;

/**
 * Fired right before the BeanContext is frozen, after everything else is registered.<br/>
 * Can be used to override an existing Bean.
 */
public class OverrideBeanEvent extends Event {

	private final BeanContext.BeanContextOverrideInjector injector;

	public OverrideBeanEvent(BeanContext.BeanContextOverrideInjector injector) {
		this.injector = injector;
	}

	public BeanContext.BeanContextOverrideInjector injector() {
		return injector;
	}

	// Convenience methods //

	public <T> void override(Class<T> type, @Nullable String name, T instance) {
		injector.override(type, name, instance);
	}

	public boolean contains(Class<?> type, @Nullable String name) {
		return injector.contains(type, name);
	}

}
