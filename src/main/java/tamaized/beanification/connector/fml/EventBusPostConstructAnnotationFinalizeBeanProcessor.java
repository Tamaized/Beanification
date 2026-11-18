package tamaized.beanification.connector.fml;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import tamaized.beanification.BeanContext;
import tamaized.beanification.BeanDefinition;
import tamaized.beanification.BeanLifeCycle;
import tamaized.beanification.InternalAutowired;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@BeanProcessor(value = BeanLifeCycle.Finalize, priority = 1)
public class EventBusPostConstructAnnotationFinalizeBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private FMLBeanContextConnector fmlBeanContextConnector;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Map.Entry<BeanDefinition<?>, Object> entry : context.beansToProcess().orElseThrow().entrySet()) {
			Object bean = entry.getValue();
			Set<Method> methods = scanData.stream()
				.flatMap(data -> data.filtered(ElementType.METHOD, EventBusPostConstruct.class))
				.filter(a -> a.member().metadata().clazz().className().equals(bean.getClass().getName()))
				.flatMap(a -> internalReflectionHelper.getDeclaredMethodsForName(bean.getClass(), a.member().metadata().memberName()).stream())
				.filter(method -> method.isAnnotationPresent(EventBusPostConstruct.class))
				.collect(Collectors.toCollection(LinkedHashSet::new));
			for (Method method : methods) {
				context.currentInjection().orElseThrow().set(method);

				if (internalReflectionHelper.isStatic(method)) {
					throw new IllegalStateException("@EventBusPostConstruct methods must be non-static");
				}

				method.trySetAccessible();

				IEventBus modBus = fmlBeanContextConnector.getModContainer().getEventBus();
				boolean isModType = method.getAnnotation(EventBusPostConstruct.class).value() == EventBusPostConstruct.Bus.MOD;
				IEventBus first = isModType ? modBus : NeoForge.EVENT_BUS;
				IEventBus second = isModType ? NeoForge.EVENT_BUS : modBus;
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 2 && parameterTypes[0].equals(IEventBus.class) && parameterTypes[1].equals(IEventBus.class)) {
					method.invoke(bean, first, second);
				} else if (parameterTypes.length == 1 && parameterTypes[0].equals(IEventBus.class)) {
					method.invoke(bean, first);
				} else {
					throw new IllegalStateException("@EventBusPostConstruct methods must have one or two IEventBus parameter(s)");
				}
			}
		}
	}

}
