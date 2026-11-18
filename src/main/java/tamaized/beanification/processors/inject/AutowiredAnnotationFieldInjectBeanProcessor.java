package tamaized.beanification.processors.inject;

import tamaized.beanification.*;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorClassMetadata;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inject)
public class AutowiredAnnotationFieldInjectBeanProcessor implements IBeanProcessor {

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
			if (bean instanceof Record)
				continue;
			BeanAnnotationProcessorClassMetadata meta = metadata.beans.get(entry.getKey());
			if (meta == null) {
				meta = new BeanAnnotationProcessorClassMetadata();
				metadata.beans.put(entry.getKey(), meta);
			}

			boolean foundEntry = false;

			for (Map.Entry<Field, BeanAnnotationProcessorClassMetadata.AnnotationMemberMetadata> memberEntry : meta.fields.entrySet()) {
				BeanDefinition<?> def = memberEntry.getValue().get(Autowired.class);
				if (def == null)
					continue;
				foundEntry = true;
				Field field = memberEntry.getKey();
				field.trySetAccessible();
				field.set(bean, context.strictInjector().orElseThrow().apply(def));
			}

			if (foundEntry)
				continue;

			for (Iterator<AnnotationScanData.AnnotationData> it = scanData.stream()
				.flatMap(data -> data.filtered(ElementType.FIELD, Autowired.class))
				.filter(a -> internalReflectionHelper.classOrSuperEquals(a.member().metadata().clazz(), bean.getClass()))
				.iterator(); it.hasNext();
			) {
				AnnotationScanData.AnnotationData data = it.next();
				Optional<String> name = Optional.ofNullable(data.annotationData().get("value"))
					.filter(String.class::isInstance)
					.map(String.class::cast);
				for (Field field : internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(bean.getClass(), data.member().metadata().memberName(),name.orElse(Component.DEFAULT_VALUE))) {
					context.currentInjection().orElseThrow().set(field);
					if (internalReflectionHelper.isStatic(field)) {
						throw new IllegalStateException("@Autowired fields must be non-static inside Beans");
					}
					field.trySetAccessible();
					BeanDefinition<?> def = new BeanDefinition<>(field.getType(), name.filter(s -> !s.equals(Component.DEFAULT_VALUE)).orElse(null));
					field.set(bean, context.strictInjector().orElseThrow().apply(def));
					if (!meta.fields.containsKey(field)) {
						meta.fields.put(field, new BeanAnnotationProcessorClassMetadata.AnnotationMemberMetadata());
					}
					meta.fields.get(field).put(Autowired.class, def);
				}
			}
		}
	}

}
