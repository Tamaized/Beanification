package tamaized.beanification.processors.finalize;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Finalize)
public class PostConstructAnnotationFinalizeBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Map.Entry<BeanDefinition<?>, Object> entry : context.beans().orElseThrow().entrySet()) {
			Object bean = entry.getValue();
			for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class)
				.filter(a -> a.clazz().equals(internalReflectionHelper.getType(bean.getClass()))).iterator(); it.hasNext();
			) {
				String name = it.next().memberName();
				List<Method> methods = new ArrayList<>();
				try {
					methods.add(internalReflectionHelper.getDeclaredMethod(bean.getClass(), name));
				} catch (NoSuchMethodException ex) {
					// NO-OP
				}
				try {
					methods.add(internalReflectionHelper.getDeclaredMethod(bean.getClass(), name, IEventBus.class));
				} catch (NoSuchMethodException ex) {
					// NO-OP
				}
				for (Method method : methods) {
					if (method.isAnnotationPresent(PostConstruct.class)) {
						context.currentInjection().orElseThrow().set(method);

						if (internalReflectionHelper.isStatic(method)) {
							throw new IllegalStateException("@PostConstruct methods must be non-static");
						}

						method.trySetAccessible();

						if (method.getParameterCount() == 2 && method.getParameterTypes()[0].equals(IEventBus.class) && method.getParameterTypes()[1].equals(IEventBus.class)) {
							final boolean isModType = method.getAnnotation(PostConstruct.class).value() == PostConstruct.Bus.MOD;
							method.invoke(bean, isModType ? modContainer.getEventBus() : NeoForge.EVENT_BUS, isModType ? NeoForge.EVENT_BUS : modContainer.getEventBus());
						} else if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(IEventBus.class)) {
							method.invoke(bean, method.getAnnotation(PostConstruct.class).value() == PostConstruct.Bus.MOD ? modContainer.getEventBus() : NeoForge.EVENT_BUS);
						} else {
							if (method.getParameterCount() != 0) {
								throw new IllegalStateException("@PostConstruct methods must not have parameters or only have one or two IEventBus parameter(s)");
							}

							method.invoke(bean);
						}
					}
				}
			}
		}
	}

}
