package tamaized.beanification.processors.inject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorClassMetadata;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inject)
public class DirectoryAnnotationFieldInjectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		ModContainer modContainer,
		ModFileScanData scanData,
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
				BeanDefinition<?> def = memberEntry.getValue().get(Directory.class);
				if (def == null)
					continue;
				foundEntry = true;
				Field field = memberEntry.getKey();
				field.trySetAccessible();
				field.set(bean, context.fuzzyInjector().orElseThrow().apply(
					def.type()
				));
			}

			if (foundEntry)
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
					Class<?> def = field.getAnnotation(Directory.class).value();
					field.set(bean, context.fuzzyInjector().orElseThrow().apply(def));
					if (!meta.fields.containsKey(field)) {
						meta.fields.put(field, new BeanAnnotationProcessorClassMetadata.AnnotationMemberMetadata());
					}
					meta.fields.get(field).put(Directory.class, new BeanDefinition<>(def, null));
				}
			}
		}
	}

}
