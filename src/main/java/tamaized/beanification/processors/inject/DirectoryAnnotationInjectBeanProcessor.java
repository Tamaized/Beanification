package tamaized.beanification.processors.inject;

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
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inject)
public class DirectoryAnnotationInjectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private ListInjector listInjector;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Map.Entry<BeanDefinition<?>, Object> entry : context.beans().orElseThrow().entrySet()) {
			Object bean = entry.getValue();
			if (bean instanceof Record)
				continue;
			for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)
				.filter(a -> internalReflectionHelper.classOrSuperEquals(a.clazz(), bean.getClass())).iterator(); it.hasNext();
			) {
				ModFileScanData.AnnotationData data = it.next();
				for (Field field : internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(bean.getClass(), data.memberName())) {
					context.currentInjection().orElseThrow().set(field);
					if (internalReflectionHelper.isStatic(field)) {
						throw new IllegalStateException("@Directory fields must be non-static inside Beans");
					}
					field.trySetAccessible();
					Directory annotation = field.getAnnotation(Directory.class);
					field.set(bean, listInjector.inject(context, scanData, bean.getClass(), annotation.value(), annotation.recursive()));
				}
			}
		}
	}

}
