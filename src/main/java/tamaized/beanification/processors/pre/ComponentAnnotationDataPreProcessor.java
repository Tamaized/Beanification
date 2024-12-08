package tamaized.beanification.processors.pre;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.Autowired;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.InternalAutowired;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.AnnotationDataPreProcessor;
import tamaized.beanification.processors.BeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor
public class ComponentAnnotationDataPreProcessor implements AnnotationDataPreProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanContextInternalDependencyTreeAccumulator context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Class<?> c = Class.forName(data.clazz().getClassName());
			Component annotation = internalReflectionHelper.getAnnotation(c, Component.class);
			String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

			Constructor<?> targetConstructor = null;
			boolean hasAutowiredCtor = false;
			for (Constructor<?> constructor : internalReflectionHelper.getConstructors(c)) {
				if (targetConstructor == null && constructor.getParameterCount() == 0)
					targetConstructor = constructor;
				else {
					if (internalReflectionHelper.allParametersHaveAnnotation(constructor.getParameterAnnotations(), Autowired.class)) {
						if (hasAutowiredCtor) {
							throw new IllegalArgumentException("Conflicting Constructors found: " + constructor.toGenericString() + " and " + targetConstructor.toGenericString());
						} else {
							hasAutowiredCtor = true;
							targetConstructor = constructor;
						}
					}
				}
			}

			if (targetConstructor == null) {
				throw new IllegalArgumentException("Could not find valid constructor");
			}

			if (hasAutowiredCtor) {
				for (Parameter parameter : targetConstructor.getParameters()) {
					String unresolvedName = parameter.getAnnotation(Autowired.class).value();
					context.addDependency(c, name, parameter.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName);
				}

			}
		}
	}

}
