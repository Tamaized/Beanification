package tamaized.beanification.processors.inject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inject)
public class AutowiredAnnotationInjectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		processBeans(context, scanData);
		processStatic(context, scanData);
	}

	private void processBeans(BeanContext.BeanLifeCycleContext context, ModFileScanData scanData) throws IllegalAccessException {
		for (Map.Entry<BeanDefinition<?>, Object> entry : context.beans().orElseThrow().entrySet()) {
			Object bean = entry.getValue();
			if (bean instanceof Record)
				continue;
			for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)
				.filter(a -> internalReflectionHelper.classOrSuperEquals(a.clazz(), bean.getClass())).iterator(); it.hasNext();
			) {
				ModFileScanData.AnnotationData data = it.next();
				Optional<String> name = Optional.ofNullable(data.annotationData().get("value"))
					.filter(String.class::isInstance)
					.map(String.class::cast);
				for (Field field : internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(bean.getClass(), data.memberName(), name.orElse(Component.DEFAULT_VALUE))) {
					context.currentInjection().orElseThrow().set(field);
					if (internalReflectionHelper.isStatic(field)) {
						throw new IllegalStateException("@Autowired fields must be non-static inside Beans");
					}
					field.trySetAccessible();
					field.set(bean, context.injector().orElseThrow().apply(
						new BeanDefinition<>(field.getType(), name.filter(s -> !s.equals(Component.DEFAULT_VALUE)).orElse(null))
					));
				}
			}
		}
	}

	private void processStatic(BeanContext.BeanLifeCycleContext context, ModFileScanData scanData) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			context.currentInjection().orElseThrow().set(data.clazz());
			Class<?> type = Class.forName(data.clazz().getClassName());
			Field field = internalReflectionHelper.getDeclaredField(type, data.memberName());
			context.currentInjection().orElseThrow().set(field);
			Autowired annotation = field.getAnnotation(Autowired.class);
			final @Nullable String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, context.injector().orElseThrow().apply(
					new BeanDefinition<>(field.getType(), name)
				));
			}
		}
	}

}
