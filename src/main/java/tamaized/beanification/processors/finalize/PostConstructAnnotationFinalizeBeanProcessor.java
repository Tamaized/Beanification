package tamaized.beanification.processors.finalize;

import tamaized.beanification.*;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Finalize)
public class PostConstructAnnotationFinalizeBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Map.Entry<BeanDefinition<?>, Object> entry : context.beansToProcess().orElseThrow().entrySet()) {
			Object bean = entry.getValue();
			for (Iterator<AnnotationScanData.AnnotationData> it = scanData.stream()
				.flatMap(data -> data.filtered(ElementType.METHOD, PostConstruct.class))
				.filter(a -> a.member().metadata().clazz().className().equals(bean.getClass().getName()))
				.iterator(); it.hasNext();
			) {
				for (Method method : internalReflectionHelper.getDeclaredMethodsForName(bean.getClass(), it.next().member().metadata().memberName())) {
					if (method.isAnnotationPresent(PostConstruct.class)) {
						context.currentInjection().orElseThrow().set(method);

						if (internalReflectionHelper.isStatic(method)) {
							throw new IllegalStateException("@PostConstruct methods must be non-static");
						}

						if (method.getParameterCount() != 0) {
							throw new IllegalStateException("@PostConstruct methods must not have parameters");
						}

						method.trySetAccessible();
						method.invoke(bean);
					}
				}
			}
		}
	}

}
