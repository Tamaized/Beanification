package tamaized.beanification.processors.staticinject;

import org.jspecify.annotations.Nullable;
import tamaized.beanification.*;
import tamaized.beanification.internal.FieldLocator;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@BeanProcessor(BeanLifeCycle.StaticInject)
public class LazyAutowiredAnnotationStaticFieldInjectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private FieldLocator fieldLocator;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Iterator<AnnotationScanData.AnnotationData> it = scanData.stream()
			.flatMap(data -> data.filtered(ElementType.FIELD, LazyAutowired.class))
			.iterator(); it.hasNext();
		) {
			AnnotationScanData.AnnotationData data = it.next();

			Field field = fieldLocator.locate(context, data);
			LazyAutowired annotation = field.getAnnotation(LazyAutowired.class);
			final @Nullable String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, context.lazyInjector().orElseThrow().apply(
					new BeanDefinition<>(field.getType(), name)
				));
			}
		}
	}

}
