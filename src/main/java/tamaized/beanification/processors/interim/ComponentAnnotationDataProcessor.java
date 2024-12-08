package tamaized.beanification.processors.interim;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.AnnotationDataProcessor;
import tamaized.beanification.processors.BeanProcessor;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@BeanProcessor
public class ComponentAnnotationDataProcessor implements AnnotationDataProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanContextInternalRegistrar context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		List<ModFileScanData.AnnotationData> retry = new ArrayList<>();
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			process(context, data, retry);
		}
		while (!retry.isEmpty()) {
			List<ModFileScanData.AnnotationData> toProcess = new ArrayList<>(retry);
			retry.clear();
			for (ModFileScanData.AnnotationData data : toProcess) {
				process(context, data, retry);
			}
		}
	}

	private void process(BeanContext.BeanContextInternalRegistrar context, ModFileScanData.AnnotationData data, List<ModFileScanData.AnnotationData> retry) throws Throwable {
		Class<?> c = Class.forName(data.clazz().getClassName());
		Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
		String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
		List<BeanDefinition<?>> deps = context.getDependencies(c, name);
		if (deps.isEmpty())
			context.register(c, name, c.getConstructor().newInstance());
		else {
			boolean skip = false;
			Object[] resolved = new Object[deps.size()];
			Class<?>[] paramTypes = new Class<?>[deps.size()];
			for (int i = 0; i < deps.size(); i++) {
				BeanDefinition<?> dep = deps.get(i);
				resolved[i] = context.getUnfrozenBean(dep);
				if (resolved[i] == null) {
					checkCircularDep(context, c, name, dep);
					retry.add(data);
					skip = true;
					break;
				} else {
					paramTypes[i] = resolved[i].getClass();
				}
			}
			if (!skip)
				context.register(c, name, internalReflectionHelper.getConstructor(c, paramTypes).newInstance(resolved));
		}
	}

	private void checkCircularDep(BeanContext.BeanContextInternalRegistrar context, Class<?> parentType, @Nullable String parentName, BeanDefinition<?> dep) {
		List<BeanDefinition<?>> chain = new ArrayList<>();
		chain.add(new BeanDefinition<>(parentType, parentName));
		if (chain.contains(dep)) {
			throw new CircularDependencyException();
		}
		stepDownAndCheckCircularDep(chain, context, dep);
	}

	private void stepDownAndCheckCircularDep(List<BeanDefinition<?>> chain, BeanContext.BeanContextInternalRegistrar context, BeanDefinition<?> dep) {
		chain.add(dep);
		context.getDependencies(dep.type(), dep.name()).forEach(child -> {
			if (chain.contains(child))
				throw new CircularDependencyException();
			stepDownAndCheckCircularDep(new ArrayList<>(chain), context, child);
		});
	}

}
