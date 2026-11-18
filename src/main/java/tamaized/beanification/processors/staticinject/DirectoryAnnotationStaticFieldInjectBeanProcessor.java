package tamaized.beanification.processors.staticinject;

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

@BeanProcessor(BeanLifeCycle.StaticInject)
public class DirectoryAnnotationStaticFieldInjectBeanProcessor implements IBeanProcessor {

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
			.flatMap(data -> data.filtered(ElementType.FIELD, Directory.class))
			.iterator(); it.hasNext();
		) {
			AnnotationScanData.AnnotationData data = it.next();
			Field field = fieldLocator.locate(context, data);
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, context.fuzzyInjector().orElseThrow().apply(field.getAnnotation(Directory.class).value()));
			}
		}
	}

}
