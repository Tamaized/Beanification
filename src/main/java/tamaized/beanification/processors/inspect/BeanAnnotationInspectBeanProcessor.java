package tamaized.beanification.processors.inspect;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DependencyInspector;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.util.*;

@BeanProcessor(value = BeanLifeCycle.Inspect, priority = 1)
public class BeanAnnotationInspectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private DependencyInspector dependencyInspector;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		List<Data> list = new ArrayList<>();
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> parent = Class.forName(data.clazz().getClassName());
			internalReflectionHelper.getDeclaredMethodsForName(parent, data.memberName()).stream()
				.filter(method -> method.isAnnotationPresent(Bean.class))
				.forEach(method -> {
					method.trySetAccessible();
					if (!internalReflectionHelper.isStatic(method))
						throw new IllegalStateException("@Bean methods must be static");
					Bean annotation = method.getAnnotation(Bean.class);
					String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

					if (method.getParameterCount() > 0) {
						if (!internalReflectionHelper.allParametersHaveAnnotation(method.getParameterAnnotations(), Autowired.class, Directory.class)) {
							throw new IllegalStateException("@Bean method parameters must be annotated with @Autowired or @Directory");
						}
						list.add(new Data(
							annotation.priority(),
							new BeanDefinition<>(method.getReturnType(), name),
							dependencyInspector.inspect(context, method.getParameters())
						));
					}
				});
		}
		list.stream()
			.sorted(Comparator.comparingInt(Data::priority))
			.forEach(data -> {
				if (context.dependencies().orElseThrow().putIfAbsent(data.definition, data.deps) != null)
					throw new IllegalStateException("Duplicate bean detected - " + data.definition);
			});
	}

	private record Data(int priority, BeanDefinition<?> definition, List<BeanDefinition<?>> deps) {

	}

}
