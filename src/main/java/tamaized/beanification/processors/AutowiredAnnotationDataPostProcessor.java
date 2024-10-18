package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@BeanProcessor
public class AutowiredAnnotationDataPostProcessor implements AnnotationDataPostProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, Object bean, AtomicReference<Object> currentInjectionTarget) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class)
			.filter(a -> internalReflectionHelper.classOrSuperEquals(a.clazz(), bean.getClass())).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Optional<String> name = Optional.ofNullable(data.annotationData().get("value"))
				.filter(String.class::isInstance)
				.map(String.class::cast);
			for (Field field : internalReflectionHelper.getAllAutowiredFieldsIncludingSuper(bean.getClass(), data.memberName(), name.orElse(Component.DEFAULT_VALUE))) {
				currentInjectionTarget.set(field);
				if (internalReflectionHelper.isStatic(field)) {
					throw new IllegalStateException("@Autowired fields must be non-static inside Beans");
				}
				field.trySetAccessible();
				field.set(bean, context.inject(field.getType(), name.filter(s -> !s.equals(Component.DEFAULT_VALUE)).orElse(null)));
			}
		}
	}

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, AtomicReference<Object> currentInjectionTarget) throws Throwable {
		List<String> ignoredClasses = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Configurable.class, Component.class, Mod.class)
			.map(d -> d.clazz().getClassName())
			.toList();
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			currentInjectionTarget.set(data.clazz());
			if (ignoredClasses.contains(data.clazz().getClassName()))
				continue;
			Class<?> type = Class.forName(data.clazz().getClassName());
			if (internalReflectionHelper.isAnyAnnotationPresent(type, Configurable.class, Component.class, Mod.class))
				continue;
			Field field = internalReflectionHelper.getDeclaredField(type, data.memberName());
			currentInjectionTarget.set(field);
			Autowired annotation = field.getAnnotation(Autowired.class);
			final @Nullable String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, context.inject(field.getType(), name));
			} else if (!context.contains(type, name)) {
				throw new IllegalStateException("@Autowired fields must be static outside of Beans");
			}
		}
	}

}
