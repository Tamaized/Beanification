package tamaized.beanification.processors.gather;

import tamaized.beanification.*;
import tamaized.beanification.internal.*;
import tamaized.beanification.processors.BeanAnnotationProcessorMetadata;
import tamaized.beanification.processors.BeanProcessor;
import tamaized.beanification.processors.IBeanProcessor;
import tamaized.beanification.scan.AnnotationScanData;

import java.lang.annotation.ElementType;
import java.util.Iterator;
import java.util.List;

@BeanProcessor(BeanLifeCycle.Gather)
public class ComponentAnnotationGatherBeanProcessor implements IBeanProcessor {

	@InternalAutowired
	private BeanConstructorLocater beanConstructorLocater;

	@InternalAutowired
	private ConjoinedParameterInjector conjoinedParameterInjector;

	@Override
	public void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable {
		for (Iterator<AnnotationScanData.AnnotationData> it = scanData.stream()
			.flatMap(data -> data.filtered(ElementType.TYPE, Component.class))
			.iterator(); it.hasNext();
		) {
			AnnotationScanData.AnnotationData data = it.next();
			BeanConstructorLocater.BeanConstructor beanConstructor = beanConstructorLocater.locate(data);

			context.gather().orElseThrow().put(beanConstructor.definition(), () -> {
				if (beanConstructor.ctor().getParameterCount() == 0)
					return beanConstructor.ctor().newInstance();
				return beanConstructor.ctor().newInstance(conjoinedParameterInjector.inject(context, beanConstructor.ctor().getParameters(), beanConstructor.ctor()));
			});
		}
	}

}
