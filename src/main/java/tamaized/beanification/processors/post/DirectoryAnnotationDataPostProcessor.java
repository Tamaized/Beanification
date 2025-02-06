package tamaized.beanification.processors.post;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.AnnotationDataPostProcessor;
import tamaized.beanification.processors.BeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@BeanProcessor
public class DirectoryAnnotationDataPostProcessor implements AnnotationDataPostProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, Object bean, AtomicReference<Object> currentInjectionTarget) throws Throwable {
		if (bean instanceof Record)
			return;
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class)
			.filter(a -> internalReflectionHelper.classOrSuperEquals(a.clazz(), bean.getClass())).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			for (Field field : internalReflectionHelper.getAllDirectoryFieldsIncludingSuper(bean.getClass(), data.memberName())) {
				currentInjectionTarget.set(field);
				if (internalReflectionHelper.isStatic(field)) {
					throw new IllegalStateException("@Directory fields must be non-static inside Beans");
				}
				field.trySetAccessible();
				field.set(bean, injectList(context, scanData, bean.getClass(), field.getAnnotation(Directory.class).value()));
			}
		}
	}

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, AtomicReference<Object> currentInjectionTarget) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Directory.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			currentInjectionTarget.set(data.clazz());
			Class<?> type = Class.forName(data.clazz().getClassName());
			Field field = internalReflectionHelper.getDeclaredField(type, data.memberName());
			currentInjectionTarget.set(field);
			Directory annotation = field.getAnnotation(Directory.class);
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, injectList(context, scanData, type, annotation.value()));
			}
		}
	}

	private List<?> injectList(BeanContext.BeanContextInternalInjector context, ModFileScanData scanData, Class<?> parent, Class<?> classFilter) {
		return scanData.getClasses().stream()
			.map(data -> {
				try {
					return Class.forName(data.clazz().getClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			})
			.filter(data -> data.getPackageName().equals(parent.getPackageName()))
			.filter(classFilter::isAssignableFrom)
			.filter(data -> context.contains(data, null))
			.map(data -> context.inject(data, null))
			.toList();
	}

}
