package tamaized.beanification.processors.gather;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.*;
import tamaized.beanification.internal.*;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;

import java.lang.annotation.ElementType;
import java.util.Iterator;

@BeanProcessor(BeanLifeCycle.Gather)
public class ComponentAnnotationGatherBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private DistAnnotationRetriever distAnnotationRetriever;

	@InternalAutowired
	private BeanConstructorLocater beanConstructorLocater;

	@InternalAutowired
	private ConjoinedParameterInjector conjoinedParameterInjector;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		ModContainer modContainer,
		ModFileScanData scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Iterator<ModFileScanData.AnnotationData> it = distAnnotationRetriever.retrieve(scanData, ElementType.TYPE, Component.class).iterator(); it.hasNext(); ) {
			ModFileScanData.AnnotationData data = it.next();
			BeanConstructorLocater.BeanConstructor beanConstructor = beanConstructorLocater.locate(data);

			context.gather().orElseThrow().put(beanConstructor.definition(), () -> {
				if (beanConstructor.ctor().getParameterCount() == 0)
					return beanConstructor.ctor().newInstance();
				return beanConstructor.ctor().newInstance(conjoinedParameterInjector.inject(context, beanConstructor.ctor().getParameters(), beanConstructor.ctor()));
			});
		}
	}

}
