package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;

public interface AnnotationDataPreProcessor {

	void process(BeanContext.BeanContextInternalDependencyTreeAccumulator context, ModContainer modContainer, ModFileScanData scanData) throws Throwable;

}
