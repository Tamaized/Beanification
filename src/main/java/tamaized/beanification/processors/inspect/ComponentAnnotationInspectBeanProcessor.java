package tamaized.beanification.processors.inspect;

import tamaized.beanification.*;
import tamaized.beanification.internal.BeanConstructorLocater;
import tamaized.beanification.internal.DependencyInspector;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inspect)
public class ComponentAnnotationInspectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private BeanConstructorLocater beanConstructorLocater;

	@InternalAutowired
	private DependencyInspector dependencyInspector;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Iterator<AnnotationScanData.AnnotationData> it = scanData.stream()
			.flatMap(data -> data.filtered(ElementType.TYPE, Component.class))
			.iterator(); it.hasNext();
		) {
			AnnotationScanData.AnnotationData data = it.next();
			Class<?> c = Class.forName(data.member().metadata().clazz().className());
			Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
			String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

			Constructor<?> ctor = beanConstructorLocater.locate(c);

			if (ctor.getParameterCount() > 0) {
				context.dependencies().orElseThrow().put(
					new BeanDefinition<>(c, name),
					dependencyInspector.inspect(context, ctor.getParameters())
				);
			}
		}
	}

}
