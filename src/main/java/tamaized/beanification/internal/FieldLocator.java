package tamaized.beanification.internal;

import tamaized.beanification.*;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.reflect.Field;

public class FieldLocator {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	public Field locate(BeanContext.BeanLifeCycleContext context, AnnotationScanData.AnnotationData data) throws ClassNotFoundException, NoSuchFieldException {
		context.currentInjection().orElseThrow().set(data.member().metadata().clazz());
		Class<?> type = Class.forName(data.member().metadata().clazz().className());
		Field field = internalReflectionHelper.getDeclaredField(type, data.member().metadata().memberName());
		context.currentInjection().orElseThrow().set(field);

		return field;
	}

}
