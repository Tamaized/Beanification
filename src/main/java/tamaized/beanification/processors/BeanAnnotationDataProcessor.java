package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.Bean;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.InternalBeanContext;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor
public class BeanAnnotationDataProcessor implements AnnotationDataProcessor {

	private final DistAnnotationRetriever distAnnotationRetriever = InternalBeanContext.inject(DistAnnotationRetriever.class);

	private final InternalReflectionHelper internalReflectionHelper = InternalBeanContext.inject(InternalReflectionHelper.class);

	@Override
	public void process(BeanContext.BeanContextInternalRegistrar context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Method method = internalReflectionHelper.getDeclaredMethod(Class.forName(data.clazz().getClassName()), data.memberName());
			Bean annotation = method.getAnnotation(Bean.class);
			context.register(method.getReturnType(), Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value(), method.invoke(null));
		}
	}

}
