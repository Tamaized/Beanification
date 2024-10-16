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
import java.util.stream.Stream;

@BeanProcessor
public class AutowiredAnnotationDataPostProcessor implements AnnotationDataPostProcessor {

	private final DistAnnotationRetriever distAnnotationRetriever = InternalBeanContext.inject(DistAnnotationRetriever.class);

	private final InternalReflectionHelper internalReflectionHelper = InternalBeanContext.inject(InternalReflectionHelper.class);

	@Override
	public void process(BeanContext.BeanContextInternalInjector context, ModContainer modContainer, ModFileScanData scanData, Object bean, AtomicReference<Object> currentInjectionTarget) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD)
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
		List<String> ignoredClasses = Stream.concat(
			distAnnotationRetriever.retrieve(scanData, Configurable.class, ElementType.TYPE),
			Stream.concat(
				distAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE),
				distAnnotationRetriever.retrieve(scanData, Mod.class, ElementType.TYPE)
			)
		).map(d -> d.clazz().getClassName()).toList();
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, Autowired.class, ElementType.FIELD).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			currentInjectionTarget.set(data.clazz());
			if (ignoredClasses.contains(data.clazz().getClassName()))
				continue;
			Class<?> type = internalReflectionHelper.forName(data.clazz().getClassName());
			if (type.isAnnotationPresent(Configurable.class) || type.isAnnotationPresent(Component.class) || type.isAnnotationPresent(Mod.class))
				continue;
			Field field = type.getDeclaredField(data.memberName());
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
