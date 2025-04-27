package tamaized.beanification.processors.inspect;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.DistAnnotationRetriever;
import tamaized.beanification.internal.InternalReflectionHelper;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@BeanProcessor(BeanLifeCycle.Inspect)
public class BeanAnnotationInspectBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private InternalReflectionHelper internalReflectionHelper;

	@Override
	public void process(BeanContext.BeanLifeCycleContext context, ModContainer modContainer, ModFileScanData scanData) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.METHOD, Bean.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			Method method = internalReflectionHelper.getDeclaredMethod(Class.forName(data.clazz().getClassName()), data.memberName());
			method.trySetAccessible();
			if (!internalReflectionHelper.isStatic(method))
				throw new IllegalStateException("@Bean methods must be static");
			Bean annotation = method.getAnnotation(Bean.class);
			String name = Objects.equals(Component.DEFAULT_VALUE, annotation.value()) ? null : annotation.value();

			if (method.getParameterCount() > 0) {
				List<BeanDefinition<?>> deps = new ArrayList<>();
				for (Parameter parameter : method.getParameters()) {
					String unresolvedName = parameter.getAnnotation(Autowired.class).value();
					deps.add(new BeanDefinition<>(parameter.getType(), unresolvedName.equals(Component.DEFAULT_VALUE) ? null : unresolvedName));
				}
				context.dependencies().orElseThrow().put(new BeanDefinition<>(method.getReturnType(), name), deps);
			}
		}
	}

}
