package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.*;

import java.lang.reflect.Parameter;
import java.util.Arrays;

@ApiStatus.Internal
public class ConjoinedParameterInjector {

	@InternalAutowired
	private AutowiredParameterInjector autowiredParameterInjector;

	public final Object[] inject(BeanContext.BeanLifeCycleContext context, Parameter[] parameters, Object objOp) {
		return Arrays.stream(parameters).map(p -> {
			if (p.isAnnotationPresent(Autowired.class)) {
				return autowiredParameterInjector.inject(context, new Parameter[] { p }, objOp);
			}

			if (p.isAnnotationPresent(Directory.class)) {
				context.currentInjection().orElseThrow().set(objOp);
				return context.fuzzyInjector().orElseThrow().apply(p.getAnnotation(Directory.class).value());
			}

			throw new IllegalArgumentException("Could not find any @Autowired or @Directory annotation on parameter: " + p.getName());
		}).toArray();
	}

}
