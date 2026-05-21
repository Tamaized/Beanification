package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.*;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApiStatus.Internal
public class DependencyInspector {

	public final List<BeanDefinition<?>> inspect(BeanContext.BeanLifeCycleContext context, Parameter[] parameters) {
		List<BeanDefinition<?>> deps = new ArrayList<>();
		for (Parameter parameter : parameters) {
			if (parameter.isAnnotationPresent(Autowired.class)) {
				String unresolvedName = parameter.getAnnotation(Autowired.class).value();
				deps.add(new BeanDefinition<>(parameter.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName));
			} else if (parameter.isAnnotationPresent(Directory.class)) {
				deps.addAll(
					context.gather().orElseThrow()
						.keySet().stream()
						.filter(def -> parameter.getAnnotation(Directory.class).value().isAssignableFrom(def.type()))
						.toList()
				);
			}
		}
		return deps;
	}

}
