package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.Autowired;
import tamaized.beanification.InternalAutowired;

import java.lang.reflect.Constructor;

@ApiStatus.Internal
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
				if (internalReflectionHelper.allParametersHaveAnnotation(constructor.getParameterAnnotations(), Autowired.class)) {
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
			throw new IllegalStateException("Could not find any Constructors in class " + c);
		}

		return targetConstructor;
	}

}
