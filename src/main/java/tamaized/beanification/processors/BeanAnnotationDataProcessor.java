package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.Bean;
import tamaized.beanification.BeanContext;
import tamaized.beanification.Component;
import tamaized.beanification.DistAnnotationRetriever;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Objects;

@BeanProcessor
public class BeanAnnotationDataProcessor implements AnnotationDataProcessor {

	@Override
	public void process(BeanContext.BeanContextInternalRegistrar context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = DistAnnotationRetriever.retrieve(scanData, Bean.class, ElementType.METHOD).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Method method = Class.forName(data.clazz().getClassName()).getDeclaredMethod(data.memberName());
			Bean annotation = method.getAnnotation(Bean.class);
			context.register(method.getReturnType(), Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value(), method.invoke(null));
		}
	}

}
