package tamaized.beanification.processors;

import tamaized.beanification.BeanContext;
import tamaized.beanification.scan.AnnotationScanData;

import java.util.List;

public interface IBeanProcessor {

	void process(
		BeanContext.BeanLifeCycleContext context,
		List<AnnotationScanData> scanData,
		BeanAnnotationProcessorMetadata metadata
	) throws Throwable;

}
