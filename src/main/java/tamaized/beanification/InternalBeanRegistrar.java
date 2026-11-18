package tamaized.beanification;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface InternalBeanRegistrar {

	void register(Class<?> type, @Nullable String name, Object instance);

}
