package tamaized.beanification.processors.gather;

import tamaized.beanification.*;
import tamaized.beanification.internal.ConjoinedParameterInjector;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.util.*;

@BeanProcessor(value = BeanLifeCycle.Gather, priority = 1)
public class BeanAnnotationGatherBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private ConjoinedParameterInjector conjoinedParameterInjector;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		List<Data> list = new ArrayList<>();
		for (Iterator<AnnotationScanData.AnnotationData> it = scanData.stream()
			.flatMap(data -> data.filtered(ElementType.METHOD, Bean.class))
			.iterator(); it.hasNext();
		) {
			AnnotationScanData.AnnotationData data = it.next();
			internalReflectionHelper.getDeclaredMethodsForName(Class.forName(data.member().metadata().clazz().className()), data.member().metadata().memberName()).stream()
				.filter(method -> method.isAnnotationPresent(Bean.class))
				.forEach(method -> {
					method.trySetAccessible();
					if (!internalReflectionHelper.isStatic(method))
						throw new IllegalStateException("@Bean methods must be static");
					Bean annotation = method.getAnnotation(Bean.class);
					String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
					list.add(new Data(annotation.priority(), new BeanDefinition<>(method.getReturnType(), name), () -> {
						if (method.getParameterCount() == 0) {
							return method.invoke(null);
						} else {
							if (!internalReflectionHelper.allParametersHaveAnnotation(method.getParameterAnnotations(), Autowired.class, Directory.class)) {
								throw new IllegalStateException("@Bean method parameters must be annotated with @Autowired or @Directory");
							}
							return method.invoke(null, conjoinedParameterInjector.inject(context, method.getParameters(), method));
						}
					}));
				});
		}
		list.stream()
			.sorted(Comparator.comparingInt(Data::priority))
			.forEach(data -> {
				if (context.gather().orElseThrow().putIfAbsent(data.definition, data.factory) != null)
					throw new IllegalStateException("Duplicate bean detected - " + data.definition);
			});
	}

	private record Data(int priority, BeanDefinition<?> definition, BeanContext.ThrowingSupplier<Object> factory) {

	}

}
