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

	public final Object injectSingle(BeanContext.BeanLifeCycleContext context, Parameter parameter, Object objOp) {
		String unresolvedName = parameter.getAnnotation(Autowired.class).value();
		BeanDefinition<?> depDef = new BeanDefinition<>(parameter.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName);
		context.currentInjection().orElseThrow().set(objOp);
		return context.strictInjector().orElseThrow().apply(depDef);
	}

	public final Object[] inject(BeanContext.BeanLifeCycleContext context, Parameter[] parameters, Object objOp) {
		return Arrays.stream(parameters).map(p -> injectSingle(context, p, objOp)).toArray();
	}

}
