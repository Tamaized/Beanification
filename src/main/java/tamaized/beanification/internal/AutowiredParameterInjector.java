package tamaized.beanification.internal;

import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.Autowired;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.Component;

import java.lang.reflect.Parameter;
import java.util.Arrays;

@ApiStatus.Internal
public class AutowiredParameterInjector {

	public final Object[] inject(BeanContext.BeanLifeCycleContext context, Parameter[] parameters, Object objOp) {
		return Arrays.stream(parameters).map(p -> {
			String unresolvedName = p.getAnnotation(Autowired.class).value();
			BeanDefinition<?> depDef = new BeanDefinition<>(p.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName);
			context.currentInjection().orElseThrow().set(objOp);
			return context.injector().orElseThrow().apply(depDef);
		}).toArray();
	}

}
