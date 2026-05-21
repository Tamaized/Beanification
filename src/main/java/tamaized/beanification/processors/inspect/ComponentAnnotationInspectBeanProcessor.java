package tamaized.beanification.processors.inspect;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.BeanConstructorLocater;
import tamaized.beanification.internal.DependencyInspector;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inspect)
public class ComponentAnnotationInspectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private BeanConstructorLocater beanConstructorLocater;

	@InternalAutowired
	private DependencyInspector dependencyInspector;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> c = Class.forName(data.clazz().getClassName());
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
