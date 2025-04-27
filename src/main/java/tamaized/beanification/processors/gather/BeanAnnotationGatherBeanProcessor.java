package tamaized.beanification.processors.gather;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.*;

@BeanProcessor(value = BeanLifeCycle.Gather, priority = 1)
public class BeanAnnotationGatherBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		List<Data> list = new ArrayList<>();
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Method method = internalReflectionHelper.getDeclaredMethod(Class.forName(data.clazz().getClassName()), data.memberName());
			method.trySetAccessible();
			if (!internalReflectionHelper.isStatic(method))
				throw new IllegalStateException("@Bean methods must be static");
			Bean annotation = method.getAnnotation(Bean.class);
			String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
			list.add(new Data(annotation.priority(), new BeanDefinition<>(method.getReturnType(), name), () -> {
				if (method.getParameterCount() == 0) {
					return method.invoke(null);
				} else {
					return method.invoke(null, Arrays.stream(method.getParameters()).map(p -> {
						String unresolvedName = p.getAnnotation(Autowired.class).value();
						BeanDefinition<?> depDef = new BeanDefinition<>(p.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName);
						context.currentInjection().orElseThrow().set(method);
						return context.injector().orElseThrow().apply(depDef);
					}).toArray());
				}
			}));
		}
		list.stream()
			.sorted(Comparator.comparingInt(Data::priority))
			.forEach(data -> context.gather().orElseThrow().put(data.definition, data.factory));
	}

	private record Data(int priority, BeanDefinition<?> definition, BeanContext.ThrowingSupplier<Object> factory) {

	}

}
