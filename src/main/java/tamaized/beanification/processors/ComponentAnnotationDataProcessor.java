package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.InternalAutowired;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;

import java.lang.annotation.ElementType;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor
public class ComponentAnnotationDataProcessor implements AnnotationDataProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanContextInternalRegistrar context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> c = Class.forName(data.clazz().getClassName());
			Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
			context.register(c, Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value(), c.getConstructor().newInstance());
		}
	}

}
