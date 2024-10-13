package tamaized.beanification.processors;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.ModFileScanData;
import tamaized.beanification.BeanContext;

public interface AnnotationDataProcessor {

	void process(BeanContext.BeanContextInternalRegistrar context, ModContainer modContainer, ModFileScanData scanData) throws Throwable;

}
