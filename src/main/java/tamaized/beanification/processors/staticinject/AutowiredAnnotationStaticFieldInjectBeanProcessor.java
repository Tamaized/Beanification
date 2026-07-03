package tamaized.beanification.processors.staticinject;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.FieldLocator;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor(BeanLifeCycle.StaticInject)
public class AutowiredAnnotationStaticFieldInjectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@InternalAutowired
	private FieldLocator fieldLocator;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		ModContainer modContainer,
		ModFileScanData scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.FIELD, Autowired.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();

			Field field = fieldLocator.locate(context, data);
			Autowired annotation = field.getAnnotation(Autowired.class);
			final @Nullable String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();
			if (internalReflectionHelper.isStatic(field)) {
				field.trySetAccessible();
				field.set(null, context.strictInjector().orElseThrow().apply(
					new BeanDefinition<>(field.getType(), name)
				));
			}
		}
	}

}
