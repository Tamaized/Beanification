package tamaized.beanification.processors.gather;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.BeanConstructorLocater;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor(BeanLifeCycle.Gather)
public class ComponentAnnotationGatherBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private BeanConstructorLocater beanConstructorLocater;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> c = Class.forName(data.clazz().getClassName());
			Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
			String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

			Constructor<?> ctor = beanConstructorLocater.locate(c);

			context.gather().orElseThrow().put(new BeanDefinition<>(c, name), () -> {
				if (ctor.getParameterCount() == 0)
					return ctor.newInstance();
				return ctor.newInstance(Arrays.stream(ctor.getParameters()).map(p -> {
					String unresolvedName = p.getAnnotation(Autowired.class).value();
					BeanDefinition<?> depDef = new BeanDefinition<>(p.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName);
					context.currentInjection().orElseThrow().set(ctor);
					return context.injector().orElseThrow().apply(depDef);
				}).toArray());
			});
		}
	}

}
