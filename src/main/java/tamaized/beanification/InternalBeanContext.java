package tamaized.beanification;

import org.jspecify.annotations.Nullable;
import tamaized.beanification.internal.*;

import java.lang.reflect.Field;
import java.util.Map;

public class InternalBeanContext extends AbstractBeanContext {

	static InternalBeanContext INSTANCE = new InternalBeanContext();

	static {
		INSTANCE.registerInternal(InternalReflectionHelper.class, null, new InternalReflectionHelper());
		INSTANCE.registerInternal(FieldLocator.class, null, new FieldLocator());
		INSTANCE.registerInternal(BeanContextConfig.class, null, new BeanContextConfig());
		INSTANCE.registerInternal(BeanConstructorLocater.class, null, new BeanConstructorLocater());
		INSTANCE.registerInternal(DependencyInspector.class, null, new DependencyInspector());
		INSTANCE.registerInternal(AutowiredParameterInjector.class, null, new AutowiredParameterInjector());
		INSTANCE.registerInternal(ConjoinedParameterInjector.class, null, new ConjoinedParameterInjector());

		INSTANCE.freeze();

		INSTANCE.getBeans().values().forEach(InternalBeanContext::injectInto);
	}

	private InternalBeanContext() {

	}

	public static <T> T inject(Class<T> type) {
		return inject(type, null);
	}

	public static <T> T inject(Class<T> type, @Nullable String name) {
		return INSTANCE.injectInternal(type, name);
	}

	static void injectInto(Object target) {
		injectInto(target, Map.of());
	}

	static void injectInto(Object target, Map<BeanDefinition<?>, Object> beans) {
		for (Field field : target.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(InternalAutowired.class)) {
				InternalAutowired annotation = field.getAnnotation(InternalAutowired.class);
				String name = annotation.value().equals(Component.DEFAULT_VALUE) ? null : annotation.value();
				field.trySetAccessible();
				try {
					Object bean = beans.get(new BeanDefinition<>(field.getType(), name));
					field.set(target, bean != null ? bean : inject(field.getType(), name));
				} catch (Throwable e) {
					throw new RuntimeException("Internal Bean Injection failed", e);
				}
			}
		}
	}

}
