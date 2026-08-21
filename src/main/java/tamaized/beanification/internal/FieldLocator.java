package tamaized.beanification.internal;

import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;
import tamaized.beanification.*;

import java.lang.reflect.Field;

@ApiStatus.Internal
public class FieldLocator {

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	public Field locate(BeanContext.BeanLifeCycleContext context, ModFileScanData.AnnotationData data) throws ClassNotFoundException, NoSuchFieldException {
		context.currentInjection().orElseThrow().set(data.clazz());
		Class<?> type = Class.forName(data.clazz().getClassName());
		Field field = internalReflectionHelper.getDeclaredField(type, data.memberName());
		context.currentInjection().orElseThrow().set(field);

		return field;
	}

}
