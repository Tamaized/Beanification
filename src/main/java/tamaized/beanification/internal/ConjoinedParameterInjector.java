package tamaized.beanification.internal;

import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.*;

import java.lang.reflect.Parameter;
import java.util.Arrays;

@ApiStatus.Internal
public class ConjoinedParameterInjector {

	@InternalAutowired
	private AutowiredParameterInjector autowiredParameterInjector;

	@InternalAutowired
	private ListInjector listInjector;

	public final Object[] inject(BeanContext.BeanLifeCycleContext context, ModFileScanData scanData, Class<?> parent, Parameter[] parameters, Object objOp) {
		return Arrays.stream(parameters).map(p -> {
			if (p.isAnnotationPresent(Autowired.class)) {
				return autowiredParameterInjector.inject(context, new Parameter[] { p }, objOp);
			}

			if (p.isAnnotationPresent(Directory.class)) {
				Directory annotation = p.getAnnotation(Directory.class);
				return listInjector.inject(context, scanData, parent, annotation.value(), annotation.recursive());
			}

			throw new IllegalArgumentException("Could not find any @Autowired or @Directory annotation on parameter: " + p.getName());
		}).toArray();
	}

}
