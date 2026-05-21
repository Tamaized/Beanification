package tamaized.beanification.processors.validate;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.util.*;

@BeanProcessor(BeanLifeCycle.Validate)
public class DependencyValidateBeanProcessor implements IBeanProcessor {

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		context.dependencies().orElseThrow().keySet()
			.forEach(bean -> process(new ArrayList<>(), bean, context.dependencies().orElseThrow()));
	}

	private void process(List<BeanDefinition<?>> stack, BeanDefinition<?> currentBean, Map<BeanDefinition<?>, List<BeanDefinition<?>>> lookup) {
		if (stack.contains(currentBean)) {
			throw new CircularDependencyException(printCircularError(stack, currentBean));
		}
		stack.add(currentBean);
		List<BeanDefinition<?>> deps = lookup.get(currentBean);
		if (deps != null) {
			deps.forEach(dep -> process(new ArrayList<>(stack), dep, lookup));
		}
	}

	private String printCircularError(List<BeanDefinition<?>> chain, BeanDefinition<?> dep) {
		BeanDefinition<?> parent = chain.getFirst();
		StringBuilder builder = new StringBuilder("Validating: ").append(parent.type());
		if (parent.name() != null)
			builder.append("@").append(parent.name());
		chain.stream().skip(1).forEach(e -> {
			builder.append("\n").append("Chain: ").append(e.type());
			if (e.name() != null)
				builder.append("@").append(e.name());
		});
		builder.append("\n").append("Dependency: ").append(dep.type());
		if (dep.name() != null)
			builder.append("@").append(dep.name());
		return builder.toString();
	}

}
