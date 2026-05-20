package tamaized.beanification.processors.inspect;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.BeanConstructorLocater;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.internal.ListInjector;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
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
	private ListInjector listInjector;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> c = Class.forName(data.clazz().getClassName());
			Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
			String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

			Constructor<?> ctor = beanConstructorLocater.locate(c);

			if (ctor.getParameterCount() > 0) {
				List<BeanDefinition<?>> deps = new ArrayList<>();
				for (Parameter parameter : ctor.getParameters()) {
					if (parameter.isAnnotationPresent(Autowired.class)) {
						String unresolvedName = parameter.getAnnotation(Autowired.class).value();
						deps.add(new BeanDefinition<>(parameter.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName));
					} else if (parameter.isAnnotationPresent(Directory.class)) {
						Directory directoryAnnotation = parameter.getAnnotation(Directory.class);
						deps.addAll(listInjector.gather(scanData, c, directoryAnnotation.value(), directoryAnnotation.recursive()));
					}
				}
				context.dependencies().orElseThrow().put(new BeanDefinition<>(c, name), deps);
			}
		}
	}

}
