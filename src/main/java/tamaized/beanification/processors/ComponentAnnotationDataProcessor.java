package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.InternalBeanContext;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;

import java.lang.annotation.ElementType;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor
public class ComponentAnnotationDataProcessor implements AnnotationDataProcessor {

	private final DistAnnotationRetriever distAnnotationRetriever = InternalBeanContext.inject(DistAnnotationRetriever.class);

	private final InternalReflectionHelper internalReflectionHelper = InternalBeanContext.inject(InternalReflectionHelper.class);

	@Override
	public void process(BeanContext.BeanContextInternalRegistrar context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, Component.class, ElementType.TYPE).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> c = internalReflectionHelper.forName(data.clazz().getClassName());
			Component annotation = c.getAnnotation(Component.class);
			context.register(c, Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value(), c.getConstructor().newInstance());
		}
	}

}
