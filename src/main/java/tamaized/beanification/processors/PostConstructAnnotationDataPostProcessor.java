package tamaized.beanification.processors;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;
import tamaized.beanification.InternalAutowired;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.PostConstruct;
import tamaized.beanification.internal.InternalReflectionHelper;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@BeanProcessor(priority = 1)
public class PostConstructAnnotationDataPostProcessor implements AnnotationDataPostProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, Object bean, AtomicReference<Object> currentInjectionTarget) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, PostConstruct.class).filter(a -> a.clazz().equals(internalReflectionHelper.getType(bean.getClass()))).iterator(); it.hasNext(); ) {
			String name = it.next().memberName().split("\\(")[0];
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
					currentInjectionTarget.set(method);

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

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, AtomicReference<Object> currentInjectionTarget) throws Throwable {

	}

}
