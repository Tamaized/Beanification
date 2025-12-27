package tamaized.beanification.processors.staticinject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.internal.ListInjector;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Iterator;

@BeanProcessor(BeanLifeCycle.StaticInject)
public class DirectoryAnnotationStaticInjectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private ListInjector listInjector;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			context.currentInjection().orElseThrow().set(data.clazz());
			Class<?> type = Class.forName(data.clazz().getClassName());
			Field field = internalReflectionHelper.getDeclaredField(type, data.memberName());
			context.currentInjection().orElseThrow().set(field);
			Directory annotation = field.getAnnotation(Directory.class);
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, listInjector.inject(context, scanData, type, annotation.value(), annotation.recursive()));
			}
		}
	}

}
