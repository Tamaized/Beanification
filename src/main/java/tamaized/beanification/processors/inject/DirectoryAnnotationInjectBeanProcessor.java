package tamaized.beanification.processors.inject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
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
					field.set(bean, injectList(context, scanData, bean.getClass(), annotation.value(), annotation.recursive()));
				}
			}
		}
	}

	private void processStatic(BeanContext.BeanLifeCycleContext context, ModFileScanData scanData) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			context.currentInjection().orElseThrow().set(data.clazz());
			Class<?> type = Class.forName(data.clazz().getClassName());
			Field field = internalReflectionHelper.getDeclaredField(type, data.memberName());
			context.currentInjection().orElseThrow().set(field);
			Directory annotation = field.getAnnotation(Directory.class);
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, injectList(context, scanData, type, annotation.value(), annotation.recursive()));
			}
		}
	}

	private List<?> injectList(BeanContext.BeanLifeCycleContext context, ModFileScanData scanData, Class<?> parent, Class<?> classFilter, boolean recursive) {
		return scanData.getClasses().stream()
			.filter(data -> {
				String pkg = data.clazz().getInternalName().replaceAll("/", ".");
				pkg = pkg.substring(0, pkg.lastIndexOf("."));
				return recursive ? pkg.contains(parent.getPackageName()) : pkg.equals(parent.getPackageName());
			})
			.map(data -> {
				try {
					return Class.forName(data.clazz().getClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			})
			.filter(classFilter::isAssignableFrom)
			.map(data -> context.injector().orElseThrow().apply(new BeanDefinition<>(data, null)))
			.filter(Objects::nonNull)
			.toList();
	}

}
