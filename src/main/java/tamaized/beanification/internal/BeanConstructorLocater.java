package tamaized.beanification.internal;

import tamaized.beanification.*;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.reflect.Constructor;
import java.util.Objects;

public class BeanConstructorLocater {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	public Constructor<?> locate(Class<?> c) {
		Constructor<?> targetConstructor = null;
		boolean hasAutowiredCtor = false;
		for (Constructor<?> constructor : internalReflectionHelper.getConstructors(c)) {
			if (targetConstructor == null && constructor.getParameterCount() == 0)
				targetConstructor = constructor;
			else {
				if (internalReflectionHelper.allParametersHaveAnnotation(constructor.getParameterAnnotations(), Autowired.class, Directory.class)) {
					if (hasAutowiredCtor) {
						throw new IllegalArgumentException("Conflicting Constructors found: " + constructor.toGenericString() + " and " + targetConstructor.toGenericString());
					} else {
						hasAutowiredCtor = true;
						targetConstructor = constructor;
					}
				}
			}
		}

		if (targetConstructor == null) {
			throw new IllegalStateException("Could not find any Constructors in class: " + c);
		}

		return targetConstructor;
	}

	public BeanConstructor locate(AnnotationScanData.AnnotationData data) throws ClassNotFoundException {
		Class<?> c = Class.forName(data.member().metadata().clazz().className());
		Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
		String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

		return new BeanConstructor(new BeanDefinition<>(c, name), locate(c));
	}

	public record BeanConstructor(BeanDefinition<?> definition, Constructor<?> ctor) {

	}

}
